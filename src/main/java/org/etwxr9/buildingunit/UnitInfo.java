package org.etwxr9.buildingunit;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.etwxr9.buildingunit.util.ChunkKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UnitInfo {

    private final String schematicName;
    private final String worldName;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final int rotate;
    private final String uuid;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final long createdAtEpochMilli;

    public UnitInfo(
            String schematicName,
            String worldName,
            int originX,
            int originY,
            int originZ,
            int rotate,
            String uuid,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            long createdAtEpochMilli) {
        this.schematicName = schematicName;
        this.worldName = worldName;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.rotate = Math.floorMod(rotate, 4);
        this.uuid = uuid;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.createdAtEpochMilli = createdAtEpochMilli;
    }

    public String getName() {
        return schematicName;
    }

    public String getSchematicName() {
        return schematicName;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public int getOriginZ() {
        return originZ;
    }

    public int getRotate() {
        return rotate;
    }

    public String getUuid() {
        return uuid;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public long getCreatedAtEpochMilli() {
        return createdAtEpochMilli;
    }

    public boolean isLocationInside(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return isLocationInside(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public boolean isLocationInside(String worldName, int x, int y, int z) {
        return this.worldName.equals(worldName)
                && x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public boolean isOverlap(Location min, Location max) {
        if (min == null || max == null || min.getWorld() == null || max.getWorld() == null) {
            return false;
        }
        if (!min.getWorld().getName().equals(max.getWorld().getName())) {
            return false;
        }
        return isOverlap(
                min.getWorld().getName(),
                min.getBlockX(), min.getBlockY(), min.getBlockZ(),
                max.getBlockX(), max.getBlockY(), max.getBlockZ());
    }

    public boolean isOverlap(String worldName, int otherMinX, int otherMinY, int otherMinZ, int otherMaxX, int otherMaxY, int otherMaxZ) {
        if (!this.worldName.equals(worldName)) {
            return false;
        }
        int normalizedMinX = Math.min(otherMinX, otherMaxX);
        int normalizedMinY = Math.min(otherMinY, otherMaxY);
        int normalizedMinZ = Math.min(otherMinZ, otherMaxZ);
        int normalizedMaxX = Math.max(otherMinX, otherMaxX);
        int normalizedMaxY = Math.max(otherMinY, otherMaxY);
        int normalizedMaxZ = Math.max(otherMinZ, otherMaxZ);
        return !(normalizedMinX > maxX || normalizedMaxX < minX
                || normalizedMinY > maxY || normalizedMaxY < minY
                || normalizedMinZ > maxZ || normalizedMaxZ < minZ);
    }

    public BoundingBox getBoundingBox() {
        return new BoundingBox(minX, minY, minZ, maxX + 1.0, maxY + 1.0, maxZ + 1.0);
    }

    public Location getOriginLocation() {
        World world = getWorld();
        return world == null ? null : new Location(world, originX, originY, originZ);
    }

    public Location getMinLocation() {
        World world = getWorld();
        return world == null ? null : new Location(world, minX, minY, minZ);
    }

    public Location getMaxLocation() {
        World world = getWorld();
        return world == null ? null : new Location(world, maxX, maxY, maxZ);
    }

    public World getWorld() {
        if (BuildingUnitMain.i == null) {
            return null;
        }
        return BuildingUnitMain.i.getServer().getWorld(worldName);
    }

    public List<Player> getEveryoneInside() {
        List<Player> result = new ArrayList<>();
        World world = getWorld();
        if (world == null) {
            return result;
        }
        for (Player player : world.getPlayers()) {
            if (isLocationInside(player.getLocation())) {
                result.add(player);
            }
        }
        return result;
    }

    public List<Long> getCoveredChunkKeys() {
        List<Long> keys = new ArrayList<>();
        for (int chunkX = ChunkKey.toChunk(minX); chunkX <= ChunkKey.toChunk(maxX); chunkX++) {
            for (int chunkZ = ChunkKey.toChunk(minZ); chunkZ <= ChunkKey.toChunk(maxZ); chunkZ++) {
                keys.add(ChunkKey.of(chunkX, chunkZ));
            }
        }
        return keys;
    }

    public void showInfoMsg(Player player) {
        player.sendMessage("========UnitInfo Start========");
        player.sendMessage("uuid: " + uuid);
        player.sendMessage("schematic: " + schematicName);
        player.sendMessage("world: " + worldName);
        player.sendMessage(String.format("Origin: (%d,%d,%d)", originX, originY, originZ));
        player.sendMessage(String.format("Bounds Min: (%d,%d,%d)", minX, minY, minZ));
        player.sendMessage(String.format("Bounds Max: (%d,%d,%d)", maxX, maxY, maxZ));
        player.sendMessage("========UnitInfo End========");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UnitInfo unitInfo)) {
            return false;
        }
        return Objects.equals(uuid, unitInfo.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
