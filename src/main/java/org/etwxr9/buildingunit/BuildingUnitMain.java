package org.etwxr9.buildingunit;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
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


public class BuildingUnitMain extends JavaPlugin {

    public static BuildingUnitMain i;

    public List<UnitInfo> getUnitInfos() {
        return unitInfos;
    }

    private List<UnitInfo> unitInfos;


    public HashMap<String, Clipboard> getClipboards() {
        return clipboards;
    }

    private HashMap<String, Clipboard> clipboards;


    @Override
    public void onEnable() {
        i = this;
        getLogger().info("onEnable is called!");
        File dataFolder = getDataFolder();
        var files = getDataFolder().listFiles();
        if (!dataFolder.exists()) dataFolder.mkdirs();
        readUnitInfo();
        readClipboards();
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
        if (!Path.of(getDataFolder() + "/Units.json").toFile().exists()) {
            SaveUnitInfo();
        }
        Type unitsType = new TypeToken<ArrayList<UnitInfo>>() {
        }.getType();
        try {
            unitInfos = gson.fromJson(Files.readString(Path.of(getDataFolder() + "/Units.json")), unitsType);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readClipboards() {
        clipboards = new HashMap<>();
        try {
            File dataFolder = getDataFolder();
            var files = getDataFolder().listFiles();
            var schePath = Path.of(getDataFolder() + "/Schematic");
            if (!Files.exists(schePath)) schePath.toFile().mkdirs();
            Files.list(schePath).forEach(path -> {
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
            Files.writeString(Path.of(getDataFolder() + "/Units.json"), gson.toJson(BuildingUnitMain.i.getUnitInfos()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
