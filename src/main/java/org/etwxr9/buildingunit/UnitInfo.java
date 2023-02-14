package org.etwxr9.buildingunit;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//对于每个粘贴到世界中的建筑单元实例，保存其信息
//包括：对应的模板名称String、原点坐标Location、旋转次数int
//API包括：取得基本属性、取得最小最大点、判断内部点、取得内部玩家、打印信息、
public class UnitInfo {
    //取得基本属性
    private String name;
    private String world;
    private int x;
    private int y;
    private int z;
    private int rotate;
    private String uuid;
    private transient CuboidRegion cr;

    public UnitInfo(String name, Location originLocation, int rotate, UUID uuid) {
        this.name = name;
        world = originLocation.getWorld().getName();
        x = originLocation.getBlockX();
        y = originLocation.getBlockY();
        z = originLocation.getBlockZ();
        this.rotate = rotate;
        this.uuid = uuid.toString();
        cr = BuildingUnitAPI.getPasteRegion(getOriginLocation(), name, rotate);
    }

    public CuboidRegion getCr() {
        if (cr == null) {
            cr = BuildingUnitAPI.getPasteRegion(getOriginLocation(), name, rotate);
        }
        return cr;
    }

    public String getUuid() {
        return uuid;
    }

    /**
     * 判断指定立方体区域是否与该unit重叠
     *
     * @param min
     * @param max
     * @return
     */
    public boolean isOverlap(Location min, Location max) {
        var locMax = getMaxLocation();
        var locMin = getMinLocation();
        return (min.getBlockX() <= locMax.getBlockX() && max.getBlockX() >= locMin.getBlockX()) &&
                (min.getBlockY() <= locMax.getBlockY() && max.getBlockY() >= locMin.getBlockX()) &&
                (min.getBlockZ() <= locMax.getBlockZ() && max.getBlockZ() >= locMin.getBlockZ());
    }

    /**
     * 判断位置是否处于该unit之中
     *
     * @param loc
     * @return
     */
    public boolean isLocationInside(Location loc) {
        var cr = getCr();
        return cr.contains(BukkitAdapter.asBlockVector(loc));
    }

    public List<Player> getEveryoneInside() {
        List<Player> result = new ArrayList<Player>();
        var cr = getCr();
        getWorld().getPlayers().forEach(p -> {
            if (isLocationInside(p.getLocation()))
                result.add(p);
        });
        return result;
    }


    /**
     * 取得最小角(西北下)的坐标
     *
     * @return
     */
    public Location getMinLocation() {
        var cr = getCr();
        var minLoc = cr.getMinimumPoint();
        return new Location(getWorld(), minLoc.getBlockX(), minLoc.getBlockY(), minLoc.getBlockZ());
    }

    /**
     * 取得最大角(东南上)的坐标
     *
     * @return
     */
    public Location getMaxLocation() {
        var cr = getCr();
        var maxLoc = cr.getMaximumPoint();
        return new Location(getWorld(), maxLoc.getBlockX(), maxLoc.getBlockY(), maxLoc.getBlockZ());
    }

    /**
     * 取得源点坐标
     *
     * @return
     */
    public Location getOriginLocation() {
        return new Location(BuildingUnitMain.i.getServer().getWorld(world), x, y, z);
    }

    public int getRotate() {
        return rotate;
    }

    public String getName() {
        return name;
    }

    public World getWorld() {
        return BuildingUnitMain.i.getServer().getWorld(world);
    }

    public void showInfoMsg(Player p) {
        p.sendMessage("========UnitInfo Start========");
        p.sendMessage("name: " + name);
        p.sendMessage(String.format("Location Min: (%d,%d,%d)", getMinLocation().getBlockX(), getMinLocation().getBlockY(), getMinLocation().getBlockZ()));
        p.sendMessage(String.format("Location Max: (%d,%d,%d)", getMaxLocation().getBlockX(), getMaxLocation().getBlockY(), getMaxLocation().getBlockZ()));
        p.sendMessage("========UnitInfo End========");
    }


}
