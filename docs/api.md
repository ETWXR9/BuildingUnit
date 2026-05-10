# BuildingUnit API 文档

本文档面向需要在其他 Paper 插件中直接调用 `BuildingUnit` 的开发者。

## 入口类

静态入口类：`org.etwxr9.buildingunit.BuildingUnitAPI`

这是推荐的对外调用入口。插件内部已经拆分为 service/repository/index 分层，但外部依赖方通常不需要直接访问这些内部实现。

## 核心数据对象

### UnitInfo

`org.etwxr9.buildingunit.UnitInfo` 表示一个已经放置到世界中的建筑单元实例。

常用字段与方法：

- `getUuid()`：唯一标识
- `getSchematicName()`：来源 schematic 名称
- `getWorldName()`：所在世界名称
- `getOriginX()/getOriginY()/getOriginZ()`：放置原点
- `getRotate()`：旋转次数，范围会被规范到 `0..3`
- `getMinX()/getMinY()/getMinZ()`：最小边界
- `getMaxX()/getMaxY()/getMaxZ()`：最大边界
- `getCreatedAtEpochMilli()`：创建时间戳
- `getOriginLocation()`：原点位置
- `getMinLocation()/getMaxLocation()`：边界位置
- `getBoundingBox()`：Bukkit `BoundingBox`
- `isLocationInside(...)`：判断坐标或位置是否在当前单元内部
- `isOverlap(...)`：判断是否与指定区域重叠
- `getEveryoneInside()`：获取当前单元范围内的玩家

说明：

- `UnitInfo` 的边界是持久化固定值，不依赖后续同名 schematic 的变化。
- `equals/hashCode` 基于 `uuid`。

### BuildingUnitAPI.PasteRegion

用于描述某个 schematic 在指定位置、指定旋转下的目标边界。

- `getMinLocation()`
- `getMaxLocation()`

## API 方法

### schematic 查询

`Map<String, Clipboard> getAllSchematics()`

- 返回当前已加载的 schematic 映射副本
- key 为 schematic 名称

`boolean isSchematicExist(String name)`

- 判断指定 schematic 是否存在

### 粘贴区域计算

`PasteRegion getPasteRegion(Location location, String name, int rotate)`

- 计算某个 schematic 在指定位置与旋转下的目标边界
- `rotate` 以 90 度为单位，允许传任意整数，内部会规范到 `0..3`
- schematic 不存在或参数无效时返回 `null`

### 粘贴单元

`UnitInfo pasteUnit(Location origin, String name, int rotate, boolean ignoreAirBlocks)`

- 把 schematic 粘贴到世界中
- 成功时返回新建的 `UnitInfo`
- 失败时返回 `null`

常见失败原因：

- schematic 不存在
- 目标区域与现有 unit 重叠
- 对应前置事件被取消
- WorldEdit/FAWE 执行失败

### 删除单元

`void deleteUnit(UnitInfo unitInfo, boolean clearEntity)`

- 删除指定单元
- `clearEntity=true` 时会同时清理范围内非玩家实体

`void deleteUnit(UnitInfo unitInfo, Predicate<Entity> preserveEntities)`

- 删除指定单元
- 你可以通过 `Predicate<Entity>` 指定哪些非玩家实体需要保留

说明：

- API 为兼容历史签名保留了 `void` 返回值
- 实际删除逻辑内部有成功/失败判断，并会触发前后置事件

### 保存 schematic

`void saveSchematic(Location min, Location max, String name, Location origin)`

- 将世界中的区域保存为 schematic
- `origin` 必须落在 `min/max` 构成的区域内

### unit 查询

`UnitInfo getUnit(Location location)`

- 查询某个位置所在的 unit
- 如果没有命中则返回 `null`

`UnitInfo getUnit(String uuid)`

- 按 UUID 查询 unit

`List<UnitInfo> getUnitsByName(String name)`

- 查询某个 schematic 名称对应的所有已放置单元

`List<UnitInfo> getOverlapUnits(Location min, Location max)`

- 查询与目标区域重叠的所有 unit

`List<UnitInfo> getOverlapUnits(CuboidRegion region)`

- 使用 WorldEdit 区域对象查询重叠 unit

### 粒子预览

`showCubeParticle(Location loc1, Location loc2, List<Player> playerList, int r, int g, int b, int tickInterval, int displayTimeNumber)`

- 对指定玩家显示立方区域边缘粒子

`showSchematicParticle(Location loc, List<Player> playerList, String name, int r, int g, int b, int tickInterval, int displayTimeNumber, int rotate)`

- 对指定玩家显示某个 schematic 的粘贴预览

`showUnitParticle(UnitInfo unitInfo, List<Player> playerList, int r, int g, int b, int tickInterval, int displayTimeNumber)`

- 对指定玩家显示已放置 unit 的边界预览

## 事件

### PrePasteUnitEvent

可取消事件，粘贴前触发。

可获取：

- `getClipboard()`
- `getRegion()`
- `getSchematicName()`
- `getLocation()`
- `getRotate()`
- `isIgnoreAirBlocks()`

用途：

- 权限校验
- 保护区限制
- 经济扣费前置判断
- 业务 veto

### PostPasteUnitEvent

粘贴成功后触发。

可获取：

- `getClipboard()`
- `getRegion()`
- `getSchematicName()`
- `getUnitInfo()`
- `getLocation()`

### PreDeleteUnitEvent

可取消事件，删除前触发。

可获取：

- `getUnitInfo()`
- `isClearEntities()`
- `getPreserveEntities()`

### PostDeleteUnitEvent

删除成功后触发。

可获取：

- `getUnitInfo()`

### PreSaveSchematicEvent

可取消事件，保存 schematic 前触发。

可获取：

- `getMin()`
- `getMax()`
- `getName()`
- `getOrigin()`

### PostSaveSchematicEvent

保存 schematic 成功后触发。

可获取：

- `getClipboard()`
- `getFilePath()`
- `getName()`

## 兼容事件

以下事件仍保留，但只建议已有依赖方临时继续使用：

- `OnPasteEvent`：继承自 `PostPasteUnitEvent`
- `OnSaveEvent`：继承自 `PostSaveSchematicEvent`

## 使用建议

- 写操作前优先自行做业务校验，不要完全依赖失败返回值
- 如果你需要 veto 某个操作，优先监听 `Pre*` 事件
- 如果你只需要读取已放置 unit，优先使用 `getUnit(...)`、`getUnitsByName(...)`、`getOverlapUnits(...)`
- 如果你需要保存自己的业务数据，不要修改 `UnitInfo` 内部状态，使用它的 UUID 作为外部关联键
