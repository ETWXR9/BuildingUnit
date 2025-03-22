package org.etwxr9.buildingunit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.etwxr9.buildingunit.event.OnPasteEvent;
import org.etwxr9.buildingunit.event.OnSaveEvent;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class BuildingUnitAPI {

    public static boolean isSchemaExist(String name) {
        return BuildingUnitMain.i.getClipboards().containsKey(name);
    }

    /**
     * 保存建筑信息
     */
    public static void saveUnitInfo() {
        BuildingUnitMain.i.SaveUnitInfo();
    }

    /**
     * 取得建筑粘贴的目标区域信息
     *
     * @param location 源点位置
     * @param name     建筑名称
     * @param rotate   90度逆时针旋转次数
     * @return
     */
    public static CuboidRegion getPasteRegion(Location location, String name, int rotate) {
        BlockVector3 to = BukkitAdapter.asBlockVector(location);
        World world = BukkitAdapter.adapt(location.getWorld());
        var clipboard = BuildingUnitMain.i.getClipboards().get(name);
        if (clipboard == null)
            return null;
        var holder = new ClipboardHolder(clipboard);
        holder.setTransform(holder.getTransform().combine(new AffineTransform().rotateY(rotate * 90)));
        var region = clipboard.getRegion();
        BlockVector3 clipboardOffset = clipboard.getRegion().getMinimumPoint().subtract(clipboard.getOrigin());
        Vector3 realTo = to.toVector3().add(holder.getTransform().apply(clipboardOffset.toVector3()));
        Vector3 max = realTo.add(
                holder.getTransform().apply(region.getMaximumPoint().subtract(region.getMinimumPoint()).toVector3()));
        return new CuboidRegion(world, realTo.toBlockPoint(), max.toBlockPoint());
    }

    /**
     * 粘贴建筑
     *
     * @param oriLoc 投影源点位置
     * @param name   建筑名称
     * @param rotate 90度逆时针旋转次数
     * @return
     */
    public static UnitInfo pasteUnit(Location oriLoc, String name, int rotate) {
        Clipboard clipboard = BuildingUnitMain.i.getClipboards().get(name);
        if (clipboard == null)
            BuildingUnitMain.i.getLogger().warning("Schematic File " + name + " Not Found!");
        BlockVector3 blockVector3 = BukkitAdapter.asBlockVector(oriLoc);
        World world = BukkitAdapter.adapt(oriLoc.getWorld());
        // 加载投影文件到reader
        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(world).build()) {
            var ch = new ClipboardHolder(clipboard);// 创建了一个clipboard的holder实例
            ch.setTransform(ch.getTransform().combine(new AffineTransform().rotateY(rotate * 90)));// 旋转指定次数
            BlockVector3 clipboardOffset = clipboard.getRegion().getMinimumPoint().subtract(clipboard.getOrigin());
            Vector3 realTo = blockVector3.toVector3().add(ch.getTransform().apply(clipboardOffset.toVector3()));
            Vector3 max = realTo.add(ch.getTransform().apply(clipboard.getRegion().getMaximumPoint()
                    .subtract(clipboard.getRegion().getMinimumPoint()).toVector3()));
            var cr = new CuboidRegion(world, realTo.toBlockPoint(), max.toBlockPoint());
            var ou = getOverlapUnits(
                    new Location(oriLoc.getWorld(), cr.getMinimumX(), cr.getMinimumY(), cr.getMinimumZ()),
                    new Location(oriLoc.getWorld(), cr.getMaximumX(), cr.getMaximumY(), cr.getMaximumZ()));
            if (ou.size() != 0) {
                // 重叠！
                BuildingUnitMain.i.getLogger().info("Unit overlay! At " + oriLoc.toString());
                return null;
            }
            Operation operation = ch.createPaste(editSession)// Create a builder using the edit session
                    .to(blockVector3) // Set where you want the paste to go
                    .ignoreAirBlocks(false) // Tell world edit not to paste air blocks (true/false)
                    .build(); // Build the operation
            try {
                Operations.complete(operation); // This'll complete a operation synchronously until it's finished
                // 添加建筑信息
                var result = new UnitInfo(name, oriLoc, rotate, UUID.randomUUID());
                BuildingUnitMain.i.getUnitInfos().add(result);
                OnPasteEvent ope = new OnPasteEvent(clipboard, cr, name, result, oriLoc);
                Bukkit.getPluginManager().callEvent(ope);
                return result;
            } catch (WorldEditException worldEditException) {
                worldEditException.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 删除建筑并清除对应区域方块和非玩家实体
     *
     * @param unitInfo    建筑信息
     * @param clearEntity 是否清除实体
     */
    public static void deleteUnit(UnitInfo unitInfo, boolean clearEntity) {
        var bv3Min = BukkitAdapter.asBlockVector(unitInfo.getMinLocation());
        var bv3Max = BukkitAdapter.asBlockVector(unitInfo.getMaxLocation());
        var w = BukkitAdapter.adapt(unitInfo.getWorld());
        CuboidRegion region = new CuboidRegion(w, bv3Min, bv3Max);
        try (EditSession es = WorldEdit.getInstance().newEditSessionBuilder().world(w).build()) {
            BlockState air = BukkitAdapter.adapt(Material.AIR.createBlockData());
            // 清除非玩家实体
            if (clearEntity)
                es.getEntities(region).forEach(entity -> {
                    if (!(entity instanceof Player)) {
                        entity.remove();
                    }
                });
            es.setBlocks((Region) region, air);
            BuildingUnitMain.i.getUnitInfos().remove(unitInfo);
        } catch (WorldEditException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存投影文件
     *
     * @param min
     * @param max
     * @param name
     * @param origin
     */
    public static void saveSchematic(Location min, Location max, String name, Location origin) {
        BlockVector3 minBV3 = BukkitAdapter.asBlockVector(min);
        BlockVector3 maxBV3 = BukkitAdapter.asBlockVector(max);
        World world = BukkitAdapter.adapt(min.getWorld());
        CuboidRegion region = new CuboidRegion(minBV3, maxBV3);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        var path = Path.of(BuildingUnitMain.i.getDataFolder() + "/Schematic/" + name + ".sche");
        if (!region.contains(BukkitAdapter.asBlockVector(origin))) {
            BuildingUnitMain.i.getLogger().info("Saving schematic error: origin not in region");
            return;
        }
        clipboard.setOrigin(BukkitAdapter.asBlockVector(origin));

        ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                world, region, clipboard, region.getMinimumPoint());
        Operations.complete(forwardExtentCopy);
        try (ClipboardWriter clipboardWriter = BuiltInClipboardFormat.FAST
                .getWriter(new FileOutputStream(path.toString()))) {
            clipboardWriter.write(clipboard);
            BuildingUnitMain.i.getClipboards().put(name, clipboard);
            OnSaveEvent ose = new OnSaveEvent(clipboard, path);
            Bukkit.getPluginManager().callEvent(ose);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 取得指定位置的建筑
     *
     * @param loc
     * @return
     */
    public static UnitInfo getUnit(Location loc) {
        var result = BuildingUnitMain.i.getUnitInfos().stream().filter(u -> u.getWorld() == loc.getWorld())
                .filter(u -> u.isLocationInside(loc)).toList();
        return result.size() == 0 ? null : result.get(0);
    }

    /**
     * 根据uuid取得建筑信息
     *
     * @param uuid
     * @return
     */
    public static UnitInfo getUnit(String uuid) {
        return BuildingUnitMain.i.getUnitInfos().stream().filter(unitInfo -> unitInfo.getUuid().equals(uuid))
                .findFirst().orElse(null);
    }

    /**
     * 根据名称取得建筑信息
     *
     * @param name
     * @return
     */
    public static List<UnitInfo> getUnitsByName(String name) {
        return BuildingUnitMain.i.getUnitInfos().stream().filter(unitInfo -> unitInfo.getName().equals(name)).toList();
    }

    // 判断指定立方体区域是否与某个unit重叠
    public static List<UnitInfo> getOverlapUnits(Location min, Location max) {
        return BuildingUnitMain.i.getUnitInfos().stream().filter(u -> u.getWorld() == min.getWorld())
                .filter(u -> u.isOverlap(min, max)).toList();
    }

    public static List<UnitInfo> getOverlapUnits(CuboidRegion cr) {
        return getOverlapUnits(
                new Location(BuildingUnitMain.i.getServer().getWorld(cr.getWorld().getName()), cr.getMinimumX(),
                        cr.getMinimumY(), cr.getMinimumZ()),
                new Location(BuildingUnitMain.i.getServer().getWorld(cr.getWorld().getName()), cr.getMaximumX(),
                        cr.getMaximumY(), cr.getMaximumZ()));
    }

    // 粒子提示相关方法
    // region ShowParticle

    /**
     * 在一个方形区域边缘显示粒子
     *
     * @param loc1              最小位置
     * @param loc2              最大位置
     * @param playerList        可以看到粒子的玩家
     * @param r
     * @param g
     * @param b
     * @param tickInterval      每次显示粒子的间隔
     * @param displayTimeNumber 显示粒子的次数
     */
    public static void showCubeParticle(Location loc1, Location loc2,
            List<Player> playerList,
            int r, int g, int b,
            int tickInterval, int displayTimeNumber) {
        // 取得最小最大位置
        var cr = new CuboidRegion(BlockVector3.at(loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ()),
                BlockVector3.at(loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ()));
        var vecMin = cr.getMinimumPoint();
        var vecMax = cr.getMaximumPoint();
        var sizeX = cr.getWidth();
        var sizeY = cr.getHeight();
        var sizeZ = cr.getLength();
        // 在边缘处生成粒子
        class Task extends BukkitRunnable {
            private int count = 0;
            @Override
            public void run() {
                for (int x = 0; x < sizeX; x++) {
                    for (int y = 0; y < sizeY; y++) {
                        for (int z = 0; z < sizeZ; z++) {
                            var xEdge = x == 0 || x == (sizeX - 1);
                            var yEdge = y == 0 || y == (sizeY - 1);
                            var zEdge = z == 0 || z == (sizeZ - 1);
                            // 筛选立方体的边
                            if (xEdge ^ yEdge ? zEdge : xEdge) {
                                loc1.getWorld().spawnParticle(org.bukkit.Particle.REDSTONE,
                                        x + vecMin.getBlockX() + 0.5,
                                        y + vecMin.getBlockY() + 0.5,
                                        z + vecMin.getBlockZ() + 0.5,
                                        1, 0, 0, 0, 0,
                                        new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(r, g, b), 1));
                            }
                        }
                    }
                }
                if (count > displayTimeNumber) {
                    this.cancel();
                }
                count++;
            }
        }
        new Task().runTaskTimer(BuildingUnitMain.i, 0, tickInterval);
    }

    public static void showSchematicParticle(Location loc, List<Player> playerList, String name,
            int r, int g, int b, int tickInterval, int displayTimeNumber, int rotate) {
        var cr = getPasteRegion(loc, name, rotate);

        var vMin = cr.getMinimumPoint();
        var vMax = cr.getMaximumPoint();
        BuildingUnitMain.i.getLogger().info("ShowSchematicParticle");
        BuildingUnitMain.i.getLogger().info(vMin.toString());
        BuildingUnitMain.i.getLogger().info(vMax.toString());
        showCubeParticle(new Location(loc.getWorld(), vMin.getBlockX(), vMin.getBlockY(), vMin.getBlockZ()),
                new Location(loc.getWorld(), vMax.getBlockX(), vMax.getBlockY(), vMax.getBlockZ()),
                playerList, r, g, b, tickInterval, displayTimeNumber);

    }

    public static void showUnitParticle(UnitInfo unitInfo, List<Player> playerList, int r, int g, int b,
            int tickInterval, int displayTimeNumber) {
        showCubeParticle(unitInfo.getMinLocation(), unitInfo.getMaxLocation(),
                playerList, r, g, b, tickInterval, displayTimeNumber);
    }

    // endregion

}
