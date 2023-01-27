package org.etwxr9.buildingunit;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.etwxr9.buildingunit.commands.BUCommand;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Main extends JavaPlugin {

    public static Main i;

    public List<UnitInfo> getUnitInfos() {
        return unitInfos;
    }

    public List<TemplateInfo> getTemplateInfos() {
        return templateInfos;
    }

    private List<UnitInfo> unitInfos;

    private List<TemplateInfo> templateInfos;

    public HashMap<String, Clipboard> getClipboards() {
        return clipboards;
    }

    private HashMap<String, Clipboard> clipboards;

    public HashMap<Player, TemplateInfo> getEditingTemplateInfos() {
        return editingTemplateInfos;
    }

    private HashMap<Player, TemplateInfo> editingTemplateInfos;


    @Override
    public void onEnable() {
        i = this;
        readTemplateInfo();
        readUnitInfo();
        readClipboards();
        editingTemplateInfos = new HashMap<>();
        getLogger().info("onEnable is called!");
        File dataFolder = getDataFolder();
        var files = getDataFolder().listFiles();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.getCommand("bu").setExecutor(new BUCommand());
    }

    @Override
    public void onDisable() {
        getLogger().info("onDisable is called!");
    }


    private void readUnitInfo() {
        //读取unitInfo
        Gson gson = new Gson();
        unitInfos = new ArrayList<UnitInfo>();
        if(!Path.of(getDataFolder() + "/Units.json").toFile().exists()){
            SaveUnitInfo();
        }
        Type unitsType = new TypeToken<ArrayList<UnitInfo>>(){}.getType();
        try {
            unitInfos = gson.fromJson(Files.readString(Path.of(getDataFolder() + "/Units.json")), unitsType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void test(int a){
        var location =  new Location(Main.i.getServer().getWorld("world"), 64, 64,64 );
        BlockVector3 to = BukkitAdapter.asBlockVector(location);
        World world = BukkitAdapter.adapt(location.getWorld());
        var clipboard = Main.i.getClipboards().get("test15");
        var holder = new ClipboardHolder(clipboard);
        holder.setTransform(holder.getTransform().combine(new AffineTransform().rotateY(a)));
        var region = clipboard.getRegion();
        BlockVector3 clipboardOffset = clipboard.getRegion().getMinimumPoint().subtract(clipboard.getOrigin());
        Vector3 realTo = to.toVector3().add(holder.getTransform().apply(clipboardOffset.toVector3()));
        Vector3 max = realTo.add(holder.getTransform().apply(region.getMaximumPoint().subtract(region.getMinimumPoint()).toVector3()));
        var result = new CuboidRegion(world, realTo.toBlockPoint(), max.toBlockPoint());
        getLogger().info("test result:");
        getLogger().info(result.getMinimumPoint().toString());
        getLogger().info(result.getMaximumPoint().toString());
    }

    private void readClipboards() {
        clipboards = new HashMap<>();
        try {
            Files.list(Path.of(getDataFolder() + "/Schematic")).forEach(path -> {
                if (!path.toString().endsWith(".sche")) return;
                ClipboardFormat clipboardFormat = ClipboardFormats.findByFile(path.toFile());
                if (clipboardFormat == null) return;
                Clipboard clipboard;
                try (ClipboardReader clipboardReader = clipboardFormat.getReader(new FileInputStream(path.toFile()))) {
                    clipboard = clipboardReader.read();
                    var name = path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf("."));
                    clipboards.put(name, clipboard);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        getLogger().info(clipboards.toString());
    }

    public void SaveUnitInfo() {
        Gson gson = new Gson();
        try {
            Files.writeString(Path.of(getDataFolder() + "/Units.json"), gson.toJson(Main.i.getUnitInfos()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readTemplateInfo() {
        //读取unitInfo
        Gson gson = new Gson();
        templateInfos = new ArrayList<>();
        try {
            Files.list(Path.of(getDataFolder() + "/Template/")).map(path -> {
                if (!path.endsWith(".json")) return null;
                try {
                    templateInfos.add(gson.fromJson(Files.readString(path),
                            TemplateInfo.class));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void savetest() {

    }

}
