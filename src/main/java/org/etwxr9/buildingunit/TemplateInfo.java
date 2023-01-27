package org.etwxr9.buildingunit;

import com.google.gson.Gson;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

//模板信息，包括名称String，原点位置Vector，大小Vector，自定义可序列化数据<String,Serlizerable>
//API包括：取得基本属性、保存到本地

public class TemplateInfo {

    public TemplateInfo(String name, Vector origin, Vector size) {
        this.name = name;
        this.origin = origin;
        this.size = size;
    }

    public void save() {
        Gson gson = new Gson();
        try {
            Files.writeString(Path.of(Main.i.getDataFolder() + "/Template/" + name + ".json"), gson.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String name;
    public Vector origin;
    public Vector size;

    public String getName() {
        return name;
    }

    public Vector getOrigin() {
        return origin;
    }

    public Vector getSize() {
        return size;
    }
}
