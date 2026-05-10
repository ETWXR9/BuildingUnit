package org.etwxr9.buildingunit;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.etwxr9.buildingunit.service.BuildingUnitService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class BuildingUnitAPI {

    private BuildingUnitAPI() {
    }

    public static final class PasteRegion {
        private final Location minLocation;
        private final Location maxLocation;

        public PasteRegion(Location minLocation, Location maxLocation) {
            this.minLocation = minLocation;
            this.maxLocation = maxLocation;
        }

        public Location getMinLocation() {
            return minLocation;
        }

        public Location getMaxLocation() {
            return maxLocation;
        }
    }

    private static BuildingUnitService service() {
        if (BuildingUnitMain.i == null || BuildingUnitMain.i.getBuildingUnitService() == null) {
            throw new IllegalStateException("BuildingUnit is not enabled.");
        }
        return BuildingUnitMain.i.getBuildingUnitService();
    }

    public static Map<String, Clipboard> getAllSchematics() {
        return new HashMap<>(service().getAllSchematics());
    }

    public static boolean isSchematicExist(String name) {
        return service().isSchematicExist(name);
    }

    public static PasteRegion getPasteRegion(Location location, String name, int rotate) {
        CuboidRegion region = getPasteRegionCR(location, name, rotate);
        if (region == null) {
            return null;
        }
        return new PasteRegion(
                new Location(location.getWorld(), region.getMinimumPoint().x(), region.getMinimumPoint().y(), region.getMinimumPoint().z()),
                new Location(location.getWorld(), region.getMaximumPoint().x(), region.getMaximumPoint().y(), region.getMaximumPoint().z()));
    }

    static CuboidRegion getPasteRegionCR(Location location, String name, int rotate) {
        return service().getPasteRegion(location, name, rotate);
    }

    public static UnitInfo pasteUnit(Location oriLoc, String name, int rotate, boolean ignoreAirBlocks) {
        return service().pasteUnit(oriLoc, name, rotate, ignoreAirBlocks);
    }

    public static void deleteUnit(UnitInfo unitInfo, boolean clearEntity) {
        service().deleteUnit(unitInfo, clearEntity);
    }

    public static void deleteUnit(UnitInfo unitInfo, Predicate<Entity> preserveEntities) {
        service().deleteUnit(unitInfo, preserveEntities);
    }

    public static void saveSchematic(Location min, Location max, String name, Location origin) {
        service().saveSchematic(min, max, name, origin);
    }

    public static UnitInfo getUnit(Location loc) {
        return service().getUnit(loc);
    }

    public static UnitInfo getUnit(String uuid) {
        return service().getUnit(uuid);
    }

    public static List<UnitInfo> getUnitsByName(String name) {
        return service().getUnitsByName(name);
    }

    public static List<UnitInfo> getOverlapUnits(Location min, Location max) {
        return service().getOverlapUnits(min, max);
    }

    public static List<UnitInfo> getOverlapUnits(CuboidRegion region) {
        return service().getOverlapUnits(region);
    }

    public static void showCubeParticle(Location loc1, Location loc2, List<Player> playerList,
            int r, int g, int b, int tickInterval, int displayTimeNumber) {
        service().showCubeParticle(loc1, loc2, playerList, r, g, b, tickInterval, displayTimeNumber);
    }

    public static void showSchematicParticle(Location loc, List<Player> playerList, String name,
            int r, int g, int b, int tickInterval, int displayTimeNumber, int rotate) {
        service().showSchematicParticle(loc, playerList, name, r, g, b, tickInterval, displayTimeNumber, rotate);
    }

    public static void showUnitParticle(UnitInfo unitInfo, List<Player> playerList, int r, int g, int b,
            int tickInterval, int displayTimeNumber) {
        service().showUnitParticle(unitInfo, playerList, r, g, b, tickInterval, displayTimeNumber);
    }
}
