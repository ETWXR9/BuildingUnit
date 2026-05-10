# BuildingUnit

`BuildingUnit` 是一个面向 Paper 服务端的建筑模板与实例管理插件。

它提供三类核心能力：
- 把世界中的方块区域保存为 schematic
- 把 schematic 粘贴为可追踪、可持久化的 `UnitInfo`
- 为其他插件提供按位置、名称、UUID、区域的高效查询能力

插件内部已经维护按 `world + chunk` 的查询索引，适合被小游戏、地块、塔防、模拟经营等需要“建筑单元”概念的插件复用。

## 运行要求

- Paper `1.21.11`
- Java `21`
- FastAsyncWorldEdit

## 安装

1. 将 `BuildingUnit` 放入服务端 `plugins/` 目录。
2. 确保服务器已安装 FastAsyncWorldEdit。
3. 启动服务器，插件会自动创建数据目录。

## 主要功能

- 保存 schematic 模板
- 粘贴 schematic 并生成唯一 `UnitInfo`
- 删除已放置的建筑单元
- 查询某个位置所属的单元
- 查询某个区域内重叠的单元
- 显示保存范围或粘贴范围的粒子预览
- 通过 Bukkit 事件与其他插件联动

## 数据文件

插件数据默认保存在 `plugins/BuildingUnit/` 目录下：

- `Schematic/`：保存的 schematic 文件
- `units-v2.json`：已放置单元的持久化数据

每个 `UnitInfo` 会固化保存以下信息：

- schematic 名称
- 世界名称
- 原点坐标
- 旋转次数
- 最小/最大边界
- UUID
- 创建时间戳

## 命令

权限节点：`buildingunit.admin`

- `/bu save <name> <sizeX> <sizeY> <sizeZ> [originOffsetX originOffsetY originOffsetZ] [confirm]`
- `/bu paste <name> <rotate> [ignoreAir] [confirm]`
- `/bu preview <name> <rotate>`
- `/bu delete`
- `/bu info`
- `/bu reload`
- `/bu stats`

说明：

- `save` 与 `paste` 默认先显示粒子预览。
- 只有追加 `confirm` 才会真正执行保存或粘贴。
- `stats` 会显示 schematic 数量、unit 数量、world 分桶数、chunk 分桶数。

## 查询能力

运行时会维护以下索引：

- `uuid -> UnitInfo`
- `schematicName -> uuid 集合`
- `world -> chunk bucket -> uuid 集合`

这意味着：

- `getUnit(uuid)` 可以直接命中主索引
- `getUnitsByName(name)` 不需要全表扫描
- `getUnit(location)` 只扫描当前位置所在 chunk 的候选单元
- `getOverlapUnits(...)` 只扫描目标区域覆盖 chunk 的候选单元

## 对外开发

如果你要在其他插件中依赖 `BuildingUnit`，优先使用静态入口类：

- `org.etwxr9.buildingunit.BuildingUnitAPI`

文档见：

- [API 文档](docs/api.md)
- [外部依赖接入示例](docs/integration-example.md)

## 事件

可用于接入校验、保护区、经济系统、统计逻辑的事件包括：

- `PrePasteUnitEvent`
- `PostPasteUnitEvent`
- `PreDeleteUnitEvent`
- `PostDeleteUnitEvent`
- `PreSaveSchematicEvent`
- `PostSaveSchematicEvent`

## 构建

```bash
mvn test
```
