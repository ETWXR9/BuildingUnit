# 外部插件接入示例

本文档展示如何在你的 Paper 插件中依赖 `BuildingUnit`。

## Maven 依赖

如果你的插件和 `BuildingUnit` 一起在同一服务端运行，通常使用 `provided` 依赖即可。

```xml
<repositories>
    <repository>
        <id>papermc</id>
        <url>https://repo.papermc.io/repository/maven-public/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.papermc.paper</groupId>
        <artifactId>paper-api</artifactId>
        <version>1.21.11-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>

    <dependency>
        <groupId>org.etwxr9</groupId>
        <artifactId>BuildingUnit</artifactId>
        <version>2.0.0-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

如果你当前还没有把 `BuildingUnit` 发布到私服仓库，最简单的做法是先本地安装：

```bash
mvn install
```

然后在你的依赖插件项目中引用本地 Maven 仓库中的同版本坐标。

## plugin.yml

推荐把 `BuildingUnit` 写入 `depend`，确保你的插件启用时它已经加载完成。

```yaml
name: ExamplePlugin
main: org.example.exampleplugin.ExamplePlugin
version: 1.0.0
api-version: '1.21'
depend: [BuildingUnit]
```

如果你的插件只在部分功能中使用 `BuildingUnit`，也可以改成 `softdepend`，但这时要自行判断插件是否存在。

## 直接调用 API

下面的示例演示了如何：

- 查询玩家脚下的 unit
- 通过名称获取所有 unit
- 在玩家位置尝试粘贴 schematic

```java
package org.example.exampleplugin;

import org.bukkit.entity.Player;
import org.etwxr9.buildingunit.BuildingUnitAPI;
import org.etwxr9.buildingunit.UnitInfo;

import java.util.List;

public final class BuildingUnitUsage {

    public UnitInfo getStandingUnit(Player player) {
        return BuildingUnitAPI.getUnit(player.getLocation());
    }

    public List<UnitInfo> getBarracksUnits() {
        return BuildingUnitAPI.getUnitsByName("barracks");
    }

    public boolean pasteBarracks(Player player) {
        UnitInfo unitInfo = BuildingUnitAPI.pasteUnit(
                player.getLocation().toBlockLocation(),
                "barracks",
                0,
                false
        );
        return unitInfo != null;
    }
}
```

## 监听前置事件进行限制

下面的示例演示如何阻止玩家把建筑粘贴到某个区域中。

```java
package org.example.exampleplugin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.etwxr9.buildingunit.event.PrePasteUnitEvent;

public final class BuildingUnitListener implements Listener {

    @EventHandler
    public void onPrePaste(PrePasteUnitEvent event) {
        String worldName = event.getLocation().getWorld().getName();
        if (!worldName.equals("game_world")) {
            event.setCancelled(true);
            return;
        }

        int minY = event.getRegion().getMinimumPoint().y();
        if (minY < 64) {
            event.setCancelled(true);
        }
    }
}
```

## 监听后置事件建立业务关联

如果你要给建筑单元绑定自己的业务数据，可以在粘贴成功后记录 `uuid`。

```java
package org.example.exampleplugin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.etwxr9.buildingunit.UnitInfo;
import org.etwxr9.buildingunit.event.PostPasteUnitEvent;

public final class BuildingUnitLinkListener implements Listener {

    @EventHandler
    public void onPostPaste(PostPasteUnitEvent event) {
        UnitInfo unitInfo = event.getUnitInfo();

        String uuid = unitInfo.getUuid();
        String schematic = unitInfo.getSchematicName();

        // 在你自己的数据系统中建立关联
        // exampleRepository.bind(uuid, schematic);
    }
}
```

## 按区域查询重叠 unit

如果你自己的玩法有领地、房间、战区等概念，可以直接查询某个区域内已有的建筑单元。

```java
package org.example.exampleplugin;

import org.bukkit.Location;
import org.etwxr9.buildingunit.BuildingUnitAPI;
import org.etwxr9.buildingunit.UnitInfo;

import java.util.List;

public final class RegionCheck {

    public boolean hasAnyUnits(Location min, Location max) {
        List<UnitInfo> units = BuildingUnitAPI.getOverlapUnits(min, max);
        return !units.isEmpty();
    }
}
```

## 接入建议

- 对读操作，直接使用 `BuildingUnitAPI`
- 对写操作，优先配合 `Pre*` / `Post*` 事件做业务控制
- 使用 `UnitInfo#getUuid()` 作为你自己业务系统的外部关联键
- 不要尝试修改 `BuildingUnit` 的内部存储文件格式，业务扩展数据应保存在你自己的插件中
