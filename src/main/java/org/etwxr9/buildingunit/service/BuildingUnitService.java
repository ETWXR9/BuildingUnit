package org.etwxr9.buildingunit.service;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.etwxr9.buildingunit.UnitInfo;
import org.etwxr9.buildingunit.event.OnPasteEvent;
import org.etwxr9.buildingunit.event.OnSaveEvent;
import org.etwxr9.buildingunit.event.PostDeleteUnitEvent;
import org.etwxr9.buildingunit.event.PostPasteUnitEvent;
import org.etwxr9.buildingunit.event.PostSaveSchematicEvent;
import org.etwxr9.buildingunit.event.PreDeleteUnitEvent;
import org.etwxr9.buildingunit.event.PrePasteUnitEvent;
import org.etwxr9.buildingunit.event.PreSaveSchematicEvent;
import org.etwxr9.buildingunit.index.UnitIndex;
import org.etwxr9.buildingunit.persistence.SchematicRepository;
import org.etwxr9.buildingunit.persistence.UnitRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public class BuildingUnitService {

    public record Stats(int schematicCount, int unitCount, int worldBucketCount, int chunkBucketCount) {
    }

    private final JavaPlugin plugin;
    private final SchematicRepository schematicRepository;
    private final UnitRepository unitRepository;
    private final UnitIndex unitIndex;

    public BuildingUnitService(JavaPlugin plugin, SchematicRepository schematicRepository, UnitRepository unitRepository, UnitIndex unitIndex) {
        this.plugin = plugin;
        this.schematicRepository = schematicRepository;
        this.unitRepository = unitRepository;
        this.unitIndex = unitIndex;
    }

    public void reload() {
        schematicRepository.reload();
        unitIndex.rebuild(unitRepository.loadAll(schematicRepository));
    }

    public void persistUnits() {
        unitRepository.saveAll(unitIndex.getAllUnits());
    }

    public Collection<UnitInfo> getAllUnits() {
        return unitIndex.getAllUnits();
    }

    public Map<String, Clipboard> getAllSchematics() {
        return schematicRepository.getAll();
    }

    public boolean isSchematicExist(String name) {
        return schematicRepository.exists(name);
    }

    public CuboidRegion getPasteRegion(Location location, String name, int rotate) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        Clipboard clipboard = schematicRepository.get(name);
        if (clipboard == null) {
            return null;
        }
        return computePasteRegion(clipboard, location, rotate);
    }

    public UnitInfo pasteUnit(Location originLocation, String schematicName, int rotate, boolean ignoreAirBlocks) {
        if (originLocation == null || originLocation.getWorld() == null) {
            return null;
        }
        Clipboard clipboard = schematicRepository.get(schematicName);
        if (clipboard == null) {
            plugin.getLogger().warning("Schematic file " + schematicName + " not found.");
            return null;
        }
        int normalizedRotate = Math.floorMod(rotate, 4);
        CuboidRegion targetRegion = computePasteRegion(clipboard, originLocation, normalizedRotate);
        if (targetRegion == null) {
            return null;
        }
        PrePasteUnitEvent preEvent = new PrePasteUnitEvent(clipboard, targetRegion, schematicName, originLocation, normalizedRotate, ignoreAirBlocks);
        Bukkit.getPluginManager().callEvent(preEvent);
        if (preEvent.isCancelled()) {
            return null;
        }
        List<UnitInfo> overlapUnits = getOverlapUnits(
                originLocation.getWorld().getName(),
                targetRegion.getMinimumPoint().x(), targetRegion.getMinimumPoint().y(), targetRegion.getMinimumPoint().z(),
                targetRegion.getMaximumPoint().x(), targetRegion.getMaximumPoint().y(), targetRegion.getMaximumPoint().z());
        if (!overlapUnits.isEmpty()) {
            plugin.getLogger().info("Unit overlap detected at " + originLocation);
            return null;
        }
        BlockVector3 blockVector = BukkitAdapter.asBlockVector(originLocation);
        World world = BukkitAdapter.adapt(originLocation.getWorld());
        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(world).build()) {
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            holder.setTransform(holder.getTransform().combine(new AffineTransform().rotateY(normalizedRotate * 90)));
            Operation operation = holder.createPaste(editSession)
                    .to(blockVector)
                    .ignoreAirBlocks(ignoreAirBlocks)
                    .build();
            Operations.complete(operation);
            UnitInfo unitInfo = new UnitInfo(
                    schematicName,
                    originLocation.getWorld().getName(),
                    originLocation.getBlockX(),
                    originLocation.getBlockY(),
                    originLocation.getBlockZ(),
                    normalizedRotate,
                    UUID.randomUUID().toString(),
                    targetRegion.getMinimumPoint().x(),
                    targetRegion.getMinimumPoint().y(),
                    targetRegion.getMinimumPoint().z(),
                    targetRegion.getMaximumPoint().x(),
                    targetRegion.getMaximumPoint().y(),
                    targetRegion.getMaximumPoint().z(),
                    System.currentTimeMillis());
            unitIndex.add(unitInfo);
            persistUnits();
            Bukkit.getPluginManager().callEvent(new PostPasteUnitEvent(clipboard, targetRegion, schematicName, unitInfo, originLocation));
            Bukkit.getPluginManager().callEvent(new OnPasteEvent(clipboard, targetRegion, schematicName, unitInfo, originLocation));
            return unitInfo;
        } catch (WorldEditException e) {
            plugin.getLogger().warning("Failed to paste schematic " + schematicName + ": " + e.getMessage());
            return null;
        }
    }

    public boolean deleteUnit(UnitInfo unitInfo, boolean clearEntities) {
        return deleteUnit(unitInfo, entity -> false, clearEntities);
    }

    public boolean deleteUnit(UnitInfo unitInfo, Predicate<Entity> preserveEntities) {
        return deleteUnit(unitInfo, preserveEntities, true);
    }

    private boolean deleteUnit(UnitInfo unitInfo, Predicate<Entity> preserveEntities, boolean clearEntities) {
        if (unitInfo == null || unitInfo.getWorld() == null) {
            return false;
        }
        PreDeleteUnitEvent preDeleteUnitEvent = new PreDeleteUnitEvent(unitInfo, clearEntities, preserveEntities);
        Bukkit.getPluginManager().callEvent(preDeleteUnitEvent);
        if (preDeleteUnitEvent.isCancelled()) {
            return false;
        }
        World world = BukkitAdapter.adapt(unitInfo.getWorld());
        CuboidRegion region = new CuboidRegion(
                world,
                BlockVector3.at(unitInfo.getMinX(), unitInfo.getMinY(), unitInfo.getMinZ()),
                BlockVector3.at(unitInfo.getMaxX(), unitInfo.getMaxY(), unitInfo.getMaxZ()));
        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(world).build()) {
            if (clearEntities) {
                loadCoveredChunks(unitInfo);
                for (Entity entity : unitInfo.getWorld().getNearbyEntities(unitInfo.getBoundingBox())) {
                    if (!(entity instanceof Player) && !preserveEntities.test(entity)) {
                        entity.remove();
                    }
                }
            }
            BlockState air = BukkitAdapter.adapt(Material.AIR.createBlockData());
            editSession.setBlocks((Region) region, air);
            unitIndex.remove(unitInfo);
            persistUnits();
            Bukkit.getPluginManager().callEvent(new PostDeleteUnitEvent(unitInfo));
            return true;
        } catch (WorldEditException e) {
            plugin.getLogger().warning("Failed to delete unit " + unitInfo.getUuid() + ": " + e.getMessage());
            return false;
        }
    }

    public boolean saveSchematic(Location min, Location max, String name, Location origin) {
        if (min == null || max == null || origin == null || min.getWorld() == null || max.getWorld() == null || origin.getWorld() == null) {
            return false;
        }
        if (!min.getWorld().equals(max.getWorld()) || !min.getWorld().equals(origin.getWorld())) {
            return false;
        }
        PreSaveSchematicEvent preEvent = new PreSaveSchematicEvent(min, max, name, origin);
        Bukkit.getPluginManager().callEvent(preEvent);
        if (preEvent.isCancelled()) {
            return false;
        }
        BlockVector3 minVector = BukkitAdapter.asBlockVector(min);
        BlockVector3 maxVector = BukkitAdapter.asBlockVector(max);
        CuboidRegion region = new CuboidRegion(minVector, maxVector);
        if (!region.contains(BukkitAdapter.asBlockVector(origin))) {
            plugin.getLogger().warning("Saving schematic error: origin not in region.");
            return false;
        }
        World world = BukkitAdapter.adapt(min.getWorld());
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(BukkitAdapter.asBlockVector(origin));
        ForwardExtentCopy copy = new ForwardExtentCopy(world, region, clipboard, region.getMinimumPoint());
        Operations.complete(copy);
        schematicRepository.save(name, clipboard);
        var filePath = schematicRepository.resolvePath(name);
        Bukkit.getPluginManager().callEvent(new PostSaveSchematicEvent(clipboard, filePath, name));
        Bukkit.getPluginManager().callEvent(new OnSaveEvent(clipboard, filePath));
        return true;
    }

    public UnitInfo getUnit(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return unitIndex.getByLocation(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public UnitInfo getUnit(String uuid) {
        return unitIndex.getByUuid(uuid);
    }

    public List<UnitInfo> getUnitsByName(String name) {
        return unitIndex.getByName(name);
    }

    public List<UnitInfo> getOverlapUnits(Location min, Location max) {
        if (min == null || max == null || min.getWorld() == null || max.getWorld() == null) {
            return List.of();
        }
        if (!min.getWorld().getName().equals(max.getWorld().getName())) {
            return List.of();
        }
        return getOverlapUnits(min.getWorld().getName(),
                min.getBlockX(), min.getBlockY(), min.getBlockZ(),
                max.getBlockX(), max.getBlockY(), max.getBlockZ());
    }

    public List<UnitInfo> getOverlapUnits(CuboidRegion region) {
        if (region == null || region.getWorld() == null) {
            return List.of();
        }
        return getOverlapUnits(region.getWorld().getName(),
                region.getMinimumPoint().x(), region.getMinimumPoint().y(), region.getMinimumPoint().z(),
                region.getMaximumPoint().x(), region.getMaximumPoint().y(), region.getMaximumPoint().z());
    }

    public List<UnitInfo> getOverlapUnits(String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return unitIndex.getOverlapping(worldName, minX, minY, minZ, maxX, maxY, maxZ);
    }

    public void showCubeParticle(Location loc1, Location loc2, List<Player> playerList,
            int r, int g, int b, int tickInterval, int displayTimeNumber) {
        if (loc1 == null || loc2 == null || loc1.getWorld() == null || loc2.getWorld() == null) {
            return;
        }
        CuboidRegion region = new CuboidRegion(
                BlockVector3.at(loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ()),
                BlockVector3.at(loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ()));
        BlockVector3 min = region.getMinimumPoint();
        int sizeX = region.getWidth();
        int sizeY = region.getHeight();
        int sizeZ = region.getLength();
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(r, g, b), 1);
        List<Player> viewers = playerList == null ? List.of() : new ArrayList<>(playerList);
        class ParticleTask extends BukkitRunnable {
            private int count;

            @Override
            public void run() {
                for (int x = 0; x < sizeX; x++) {
                    for (int y = 0; y < sizeY; y++) {
                        for (int z = 0; z < sizeZ; z++) {
                            boolean xEdge = x == 0 || x == sizeX - 1;
                            boolean yEdge = y == 0 || y == sizeY - 1;
                            boolean zEdge = z == 0 || z == sizeZ - 1;
                            if (xEdge ^ yEdge ? zEdge : xEdge) {
                                double particleX = x + min.x() + 0.5;
                                double particleY = y + min.y() + 0.5;
                                double particleZ = z + min.z() + 0.5;
                                for (Player player : viewers) {
                                    player.spawnParticle(Particle.DUST, particleX, particleY, particleZ, 1, 0, 0, 0, 0, dust);
                                }
                            }
                        }
                    }
                }
                count++;
                if (count >= displayTimeNumber) {
                    cancel();
                }
            }
        }
        new ParticleTask().runTaskTimer(plugin, 0L, tickInterval);
    }

    public void showSchematicParticle(Location location, List<Player> playerList, String name,
            int r, int g, int b, int tickInterval, int displayTimeNumber, int rotate) {
        CuboidRegion region = getPasteRegion(location, name, rotate);
        if (region == null) {
            return;
        }
        showCubeParticle(
                new Location(location.getWorld(), region.getMinimumPoint().x(), region.getMinimumPoint().y(), region.getMinimumPoint().z()),
                new Location(location.getWorld(), region.getMaximumPoint().x(), region.getMaximumPoint().y(), region.getMaximumPoint().z()),
                playerList, r, g, b, tickInterval, displayTimeNumber);
    }

    public void showUnitParticle(UnitInfo unitInfo, List<Player> playerList, int r, int g, int b, int tickInterval, int displayTimeNumber) {
        Location min = unitInfo.getMinLocation();
        Location max = unitInfo.getMaxLocation();
        if (min != null && max != null) {
            showCubeParticle(min, max, playerList, r, g, b, tickInterval, displayTimeNumber);
        }
    }

    public Stats getStats() {
        return new Stats(schematicRepository.getAll().size(), unitIndex.getAllUnits().size(), unitIndex.getWorldBucketCount(), unitIndex.getChunkBucketCount());
    }

    public static CuboidRegion computePasteRegion(Clipboard clipboard, Location location, int rotate) {
        if (clipboard == null || location == null || location.getWorld() == null) {
            return null;
        }
        BlockVector3 to = BukkitAdapter.asBlockVector(location);
        World world = BukkitAdapter.adapt(location.getWorld());
        ClipboardHolder holder = new ClipboardHolder(clipboard);
        holder.setTransform(holder.getTransform().combine(new AffineTransform().rotateY(Math.floorMod(rotate, 4) * 90)));
        BlockVector3 clipboardOffset = clipboard.getRegion().getMinimumPoint().subtract(clipboard.getOrigin());
        Vector3 realMin = to.toVector3().add(holder.getTransform().apply(clipboardOffset.toVector3()));
        Vector3 realMax = realMin.add(holder.getTransform().apply(
                clipboard.getRegion().getMaximumPoint().subtract(clipboard.getRegion().getMinimumPoint()).toVector3()));
        return new CuboidRegion(world, realMin.toBlockPoint(), realMax.toBlockPoint());
    }

    private void loadCoveredChunks(UnitInfo unitInfo) {
        var world = unitInfo.getWorld();
        if (world == null) {
            return;
        }
        for (int chunkX = unitInfo.getMinX() >> 4; chunkX <= unitInfo.getMaxX() >> 4; chunkX++) {
            for (int chunkZ = unitInfo.getMinZ() >> 4; chunkZ <= unitInfo.getMaxZ() >> 4; chunkZ++) {
                world.getChunkAt(chunkX, chunkZ);
            }
        }
    }
}
