# BuildingUnit 改造方案

## 1. 文档目的

本文档用于指导 `BuildingUnit` 从“可用但偏原型化的结构模板插件”升级为“可被其他插件稳定依赖的基础设施插件”。

本轮方案额外覆盖以下目标：

- 将当前版本归档到 Git 分支
- 从 Spigot API 迁移到 Paper API
- 升级到 Minecraft/Paper `1.21.11`
- 为 `UnitInfo` 查询引入索引与分桶设计
- 为后续持续演进建立稳定 API、持久化模型和测试基线

## 2. 当前版本归档

已将当前基线版本归档到以下分支：

- `codex/archive-v1.21.1-1.3`

归档基线提交：

- `8ca5f8b857ba9d12588cd39ea76a5ecd5ebf7870`

用途说明：

- 作为重构前可回溯快照
- 用于对照行为变更
- 用于回滚和兼容性验证

## 3. 当前问题摘要

结合现有代码审查，当前实现存在以下结构性问题：

1. `UnitInfo` 的实际占用区域未持久化，历史实例依赖“当前 schematic 内容”重新计算边界，导致模板一旦被替换，旧实例行为可能漂移。
2. API 以静态全局状态为中心，内部可变集合直接暴露，对外部依赖方不安全。
3. 查询全部基于 `List<UnitInfo>` 线性扫描，规模上来后会先在“按位置查询”“重叠检测”“按名称检索”上退化。
4. 事件只有后置通知，没有可取消的前置阶段，不适合作为其他插件的扩展钩子。
5. 命令层与核心能力耦合，且参数校验薄弱，当前命令更接近调试工具而不是可维护接口。
6. 持久化、异常处理和 JSON 恢复策略过于脆弱，损坏数据时缺少可控降级路径。
7. 当前构建目标和插件元数据存在不一致，版本表达不统一。

## 4. 改造目标

### 4.1 功能目标

保持并增强以下能力：

- 保存 schematic
- 粘贴 schematic 并生成稳定 `UnitInfo`
- 删除 `UnitInfo` 及其占用区域
- 根据位置、UUID、名称、区域查询 `UnitInfo`
- 提供粒子预览
- 为其他插件提供可控事件和只读查询能力

### 4.2 工程目标

- API 稳定，避免外部依赖方直接改坏内部状态
- 持久化模型可版本化迁移
- 查询复杂度从全表扫描降到索引命中或局部分桶扫描
- 与 Paper `1.21.11` 保持一致
- 补齐针对核心逻辑的自动化测试

## 5. 目标版本与依赖策略

### 5.1 平台目标

- Java：继续使用 `21`
- Server API：从 `org.spigotmc:spigot-api` 切换到 `io.papermc.paper:paper-api`
- 目标服务端：Paper `1.21.11`

### 5.2 Maven 目标依赖

建议将当前：

- `org.spigotmc:spigot-api:1.21.1-R0.1-SNAPSHOT`

替换为：

- `io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT`

保留 `provided` 或 `compileOnly` 语义，不将服务端 API 打包进插件产物。

### 5.3 插件元数据

建议统一以下信息：

- `pom.xml` 版本
- `plugin.yml` 版本
- `plugin.yml` 的 `api-version`

如果继续使用传统 `plugin.yml` 模式，建议直接声明针对 Paper `1.21.11` 的 API 版本，并确保描述、作者、网站、权限和命令信息同步补齐。

## 6. 总体架构改造

### 6.1 分层目标

建议把当前“主类 + 静态 API”重构为以下层次：

1. `BuildingUnitPlugin`
   负责生命周期、配置加载、服务组装、命令注册、事件注册。
2. `SchematicRepository`
   负责 schematic 文件发现、加载、缓存、刷新。
3. `UnitRepository`
   负责 `UnitInfo` 持久化、加载、保存、迁移。
4. `UnitIndex`
   负责运行期查询索引与分桶。
5. `BuildingUnitService`
   负责粘贴、删除、查询、重叠检测、事件触发。
6. `BuildingUnitFacade`
   作为对外稳定 API，暴露只读查询和受控操作方法。

### 6.2 API 边界目标

对外应避免直接返回内部可变集合，改为：

- 返回不可变集合
- 或返回副本
- 或返回只读接口

受控写操作只通过服务层进行，例如：

- `pasteUnit(...)`
- `deleteUnit(...)`
- `saveSchematic(...)`
- `reloadSchematics()`

## 7. 数据模型改造

### 7.1 现状问题

当前 `UnitInfo` 持久化字段只有：

- `name`
- `world`
- `x/y/z`
- `rotate`
- `uuid`

其占用区域依赖运行时重新计算，且计算依赖外部 schematic 当前内容。

### 7.2 目标模型

建议新增版本化持久化模型 `StoredUnitRecord`，最少包含：

- `schemaVersion`
- `uuid`
- `schematicName`
- `worldName`
- `originX`
- `originY`
- `originZ`
- `rotationQuarterTurns`
- `minX`
- `minY`
- `minZ`
- `maxX`
- `maxY`
- `maxZ`
- `createdAt`

可选字段：

- `tags`
- `metadata`
- `owner`
- `placementFlags`

### 7.3 运行时对象

运行时保留 `UnitInfo`，但它不再负责“从当前 schematic 推导边界”，而是持有已经固化的边界：

- 原点用于业务语义
- 边界用于查询和删除
- schematic 名称用于追踪来源

这样即使后续覆盖同名 schematic，历史 `UnitInfo` 仍保持稳定。

## 8. 查询索引与分桶设计

### 8.1 设计目标

目标是把当前基于 `List<UnitInfo>` 的全量扫描，改为“按需求命中索引，再在局部桶内扫描”。

需要优化的典型查询：

- 根据 UUID 获取 unit
- 根据名称获取 units
- 根据位置获取 unit
- 根据区域获取重叠 units
- 删除时快速找到受影响实体和相邻 units

### 8.2 建议索引结构

#### 主索引

- `Map<String, UnitInfo> unitsByUuid`
  用于 `getUnit(uuid)`

#### 名称索引

- `Map<String, Set<String>> unitIdsBySchematicName`
  `name -> uuid集合`

优点：

- 允许 `UnitInfo` 主体只存一份
- 名称检索只需要二次映射到主索引

#### 世界分桶

- `Map<String, WorldUnitBucket> bucketsByWorld`

每个世界维护独立桶，避免跨世界扫描。

### 8.3 空间分桶设计

建议采用“按 chunk 坐标分桶”的方案，而不是自定义固定立方网格。

原因：

1. 插件的查询天然与世界方块/区块坐标对齐。
2. 删除实体、加载实体、重叠检测都容易和 Bukkit/Paper 的 chunk 概念对接。
3. 复杂度和实现成本之间平衡最好。

#### 结构定义

`WorldUnitBucket` 建议包含：

- `Map<Long, Set<String>> unitIdsByChunkKey`
- `Map<String, UnitInfo> unitsByUuidRef`

其中 `chunkKey` 由 `(chunkX, chunkZ)` 编码为单个 `long`。

编码建议：

- `chunkKey = (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL)`

#### 建桶规则

每个 `UnitInfo` 根据其 `BoundingBox` 覆盖的所有 chunk 注册到多个桶中。

例如：

- `minChunkX = floor(minX >> 4)`
- `maxChunkX = floor(maxX >> 4)`
- `minChunkZ = floor(minZ >> 4)`
- `maxChunkZ = floor(maxZ >> 4)`

遍历覆盖范围内的所有 chunk，将 `uuid` 写入对应桶。

### 8.4 查询流程

#### 根据位置查 unit

1. 根据位置确定世界
2. 根据 `(blockX >> 4, blockZ >> 4)` 命中 chunk 桶
3. 仅对该桶中的 `UnitInfo` 做精确 `BoundingBox contains`

#### 根据区域查重叠 units

1. 计算查询区域覆盖的 chunk 范围
2. 聚合这些桶内的 uuid
3. 去重
4. 对候选 `UnitInfo` 做精确 AABB overlap 判断

#### 根据名称查 units

1. 通过 `unitIdsBySchematicName` 命中 uuid 集
2. 映射回 `unitsByUuid`

### 8.5 复杂度收益

当前：

- 位置查询：`O(n)`
- 重叠查询：`O(n)`
- 名称查询：`O(n)`

目标：

- UUID 查询：`O(1)`
- 名称查询：`O(k)`
- 位置查询：`O(bucketSize)`
- 重叠查询：`O(candidateCount)`

其中 `bucketSize` 和 `candidateCount` 一般远小于全局 `n`。

### 8.6 索引维护策略

对 `pasteUnit`、`deleteUnit`、`loadAllUnits` 三类路径统一维护索引：

1. `loadAllUnits`
   启动时从持久化加载所有 `UnitInfo`，逐个注册到索引。
2. `pasteUnit`
   粘贴成功并持久化后，再加入索引。
3. `deleteUnit`
   删除前先拿到 `UnitInfo`，删除成功后从索引中移除。

注意事项：

- 索引更新必须与持久化和内存状态保持一致。
- 如未来引入异步 IO，需要明确主线程状态变更边界。

## 9. 事件模型改造

### 9.1 目标事件

建议引入以下事件：

- `PrePasteUnitEvent implements Cancellable`
- `PostPasteUnitEvent`
- `PreDeleteUnitEvent implements Cancellable`
- `PostDeleteUnitEvent`
- `PreSaveSchematicEvent implements Cancellable`
- `PostSaveSchematicEvent`

### 9.2 事件原则

1. 前置事件只做校验、审计、权限控制、经济扣费等 veto 类工作。
2. 后置事件只做通知、联动、统计、日志、额外副作用。
3. 后置事件触发时，内存状态和持久化状态必须已经一致。

## 10. Paper 迁移方案

### 10.1 第一阶段要求

本轮不要求强依赖 Paper 专属特性，但要求：

- 构建依赖切换到 Paper API
- 插件在 Paper `1.21.11` 上稳定运行
- 避免继续依赖已过时且即将删除的接口

### 10.2 建议利用的 Paper 能力

迁移后可以逐步采用：

- Adventure `Component` 输出命令反馈，替代纯字符串消息
- 更细粒度的 `spawnParticle` 重载和只对指定玩家发送粒子
- 更现代的命令注册与补全能力
- 更清晰的 plugin metadata 声明

### 10.3 兼容策略

因为目标已经明确为其他插件依赖的基础插件，建议直接收敛到：

- 仅支持 Paper `1.21.11`

不再为 Spigot 保留兼容分支逻辑，避免测试矩阵膨胀。

## 11. 命令与调试能力改造

### 11.1 目标原则

命令层不应继续充当核心 API 的真实入口，而应成为：

- 管理入口
- 调试入口
- 验证入口

### 11.2 建议子命令

- `/bu save <name> <sizeX> <sizeY> <sizeZ> [originX originY originZ]`
- `/bu paste <name> <rotate> [ignoreAir]`
- `/bu delete`
- `/bu info`
- `/bu preview <name> <rotate>`
- `/bu reload`
- `/bu stats`

### 11.3 命令要求

- 补齐权限节点
- 补齐参数长度检查
- 补齐参数类型检查
- 对外统一错误消息
- 使用服务层，不直接拼业务逻辑

## 12. 持久化与迁移策略

### 12.1 存储格式

短期可继续使用 JSON，但结构必须版本化。

建议：

- 文件名改为 `units-v2.json`
- 老文件 `Units.json` 作为迁移输入

### 12.2 启动迁移

首次启动时流程：

1. 检查是否存在 `units-v2.json`
2. 若不存在但存在 `Units.json`，执行一次性迁移
3. 迁移时读取旧 `UnitInfo`
4. 根据当时可解析出的 schematic 和 rotation 计算边界
5. 写出 `units-v2.json`
6. 备份旧文件为 `Units.json.bak`

### 12.3 错误恢复

建议增加：

- 解析失败时输出明确日志
- 失败记录隔离到 `units-corrupted-*.json`
- 启动不中断，但跳过损坏记录并给出数量统计

## 13. 测试策略

### 13.1 必测范围

至少覆盖以下逻辑：

- `BoundingBox` 计算与旋转
- 位置包含判断
- 区域重叠判断
- chunk 分桶注册与移除
- 按位置查询
- 按区域查询
- 按名称查询
- 旧数据迁移
- 同名 schematic 被替换后，旧 `UnitInfo` 不漂移

### 13.2 测试层次

- 纯单元测试：几何计算、chunk 编码、索引维护
- 集成测试：仓储加载、迁移、服务层行为
- 手工验证：实际粘贴、删除、实体清理、粒子预览

## 14. 实施阶段

### 阶段 A：基线整理

- 切换到 Paper API `1.21.11`
- 统一 `pom.xml`、`plugin.yml` 版本信息
- 去掉明显失效或危险命令实现
- 清理废弃 API 用法

交付标准：

- `mvn test` 通过
- 插件可在 Paper `1.21.11` 启动

### 阶段 B：数据模型重构

- 引入 `StoredUnitRecord`
- 固化 `BoundingBox`
- 完成旧数据迁移器

交付标准：

- 启动可读旧数据并输出新格式
- 替换 schematic 后旧实例边界不变

### 阶段 C：索引与分桶

- 引入 `UnitIndex`
- 完成 UUID/名称/world/chunk 级索引
- 替换线性扫描查询路径

交付标准：

- 所有查询走索引路径
- 删除和重叠检测不再全表扫描

### 阶段 D：事件与 API 稳定化

- 引入前后置事件
- 对外返回只读视图
- 文档化对外 API

交付标准：

- 外部插件可以在前置事件中 veto
- 外部无法直接篡改内部状态

### 阶段 E：命令和文档整理

- 重写命令参数解析
- 增加权限和提示
- 修复 README 编码与示例

交付标准：

- 命令可作为管理工具稳定使用
- 对接方能按 README 快速接入

## 15. 推荐目录结构

建议重构后采用类似结构：

```text
src/main/java/org/etwxr9/buildingunit/
  BuildingUnitPlugin.java
  api/
    BuildingUnitFacade.java
    event/
  command/
  model/
    UnitInfo.java
    StoredUnitRecord.java
  persistence/
    UnitRepository.java
    SchematicRepository.java
    migration/
  service/
    BuildingUnitService.java
  index/
    UnitIndex.java
    WorldUnitBucket.java
  util/
    ChunkKey.java
    BoundingBoxes.java
```

## 16. 验收标准

满足以下条件时，认为本轮改造达标：

1. 插件基于 Paper API `1.21.11` 构建和运行。
2. 对外不再暴露内部可变集合。
3. `UnitInfo` 的空间边界在持久化后保持稳定，不受后续 schematic 替换影响。
4. 位置查询和重叠查询使用 chunk 分桶索引。
5. 提供可取消的前置事件。
6. 命令具备基本的参数校验和权限控制。
7. 迁移旧数据时可控、可回滚、可审计。
8. 核心几何与索引逻辑有自动化测试覆盖。

## 17. 建议的下一步执行顺序

建议按以下顺序实际动手：

1. 先完成依赖切换到 Paper `1.21.11`
2. 再重做 `UnitInfo` 持久化模型
3. 然后落地 `UnitIndex` 与 chunk 分桶
4. 再补前后置事件
5. 最后重写命令和 README

这样做的原因是：

- 先把平台基线固定，避免后面边改架构边改依赖
- 先稳住数据模型，再上索引，避免索引绑定旧结构
- 事件和命令属于 API 表层，应该建立在稳定数据与查询模型之上
