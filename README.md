# BuildingUnit-建筑单元API
## 简介
在mc各类小游戏活动通常都使用不能修改方块的冒险模式作为游戏规则的基础。然而有的时候，我们希望在小游戏中也能拥有“建筑”的玩法，例如模拟经营、塔防等玩法都依赖于“创建具有功能的建筑”的功能。因此我琢磨了一个简单的插件来为此类功能提供代码基础。

## 功能
插件通过调用它的函数，可以做到：
1. 在服务端将世界中的区域保存为投影文件
2. 将指定名称的投影粘贴到世界中（可旋转），并存储其信息，生成一个UUID。这个信息被称为“UnitInfo”，即单元信息。包括uuid、名称、位置、大小。
3. 通过名称、位置等信息检索指定的建筑单元信息，然后进一步利用建筑的信息实现具体游戏玩法。
4. 删除单元，并清空范围内的方块。
5. 在单元建筑的边框显示粒子进行提示。
6. 检测某个位置、某个区域是否在某个单元内部。
7. 保存、粘贴的事件。

## org.etwxr9.buildingunit.BuildingUnitAPI
该类提供了常用的功能。

*   ### 方法详细资料

    *   ### getPasteRegion

        public static com.sk89q.worldedit.regions.CuboidRegion getPasteRegion(org.bukkit.Location location, String name, int rotate)

        取得建筑粘贴的目标区域信息

        参数:

        `location` - 源点位置

        `name` - 建筑名称

        `rotate` - 90度逆时针旋转次数

        返回:

    *   ### pasteUnit

        public static org.etwxr9.buildingunit.UnitInfo pasteUnit(org.bukkit.Location oriLoc, String name, int rotate)

        粘贴建筑

        参数:

        `oriLoc` - 投影源点位置

        `name` - 建筑名称

        `rotate` - 90度逆时针旋转次数

        返回:

    *   ### deleteUnit

        public static void deleteUnit(org.etwxr9.buildingunit.UnitInfo unitInfo)

        删除建筑并清除对应区域方块

        参数:

        `unitInfo` -

    *   ### saveSchematic

        public static void saveSchematic(org.bukkit.Location min, org.bukkit.Location max, String name, org.bukkit.Location origin)

        保存投影文件

        参数:

        `min` -

        `max` -

        `name` -

        `origin` -

    *   ### getUnit

        public static org.etwxr9.buildingunit.UnitInfo getUnit(org.bukkit.Location loc)

        取得指定位置的建筑

        参数:

        `loc` -

        返回:

    *   ### getUnit

        public static org.etwxr9.buildingunit.UnitInfo getUnit(String uuid)

        根据uuid取得建筑信息

        参数:

        `uuid` -

        返回:

    *   ### getUnitsByName

        public static List<org.etwxr9.buildingunit.UnitInfo> getUnitsByName(String name)

        根据名称取得建筑信息

        参数:

        `name` -

        返回:

    *   ### getOverlapUnits

        public static List<org.etwxr9.buildingunit.UnitInfo> getOverlapUnits(org.bukkit.Location min, org.bukkit.Location max)

    *   ### getOverlapUnits

        public static List<org.etwxr9.buildingunit.UnitInfo> getOverlapUnits(com.sk89q.worldedit.regions.CuboidRegion cr)

    *   ### showCubeParticle

        public static void showCubeParticle(org.bukkit.Location loc1, org.bukkit.Location loc2, List<org.bukkit.entity.Player> playerList, int r, int g, int b, int tickInterval, int displayTimeNumber)

        在一个方形区域边缘显示粒子

        参数:

        `loc1` - 最小位置

        `loc2` - 最大位置

        `playerList` - 可以看到粒子的玩家

        `r` -

        `g` -

        `b` -

        `tickInterval` - 每次显示粒子的间隔

        `displayTimeNumber` - 显示粒子的次数

    *   ### showSchematicParticle

        public static void showSchematicParticle(org.bukkit.Location loc, List<org.bukkit.entity.Player> playerList, String name, int r, int g, int b, int tickInterval, int displayTimeNumber, int rotate)

    *   ### showUnitParticle

        public static void showUnitParticle(org.etwxr9.buildingunit.UnitInfo unitInfo, List<org.bukkit.entity.Player> playerList, int r, int g, int b, int tickInterval, int displayTimeNumber)

