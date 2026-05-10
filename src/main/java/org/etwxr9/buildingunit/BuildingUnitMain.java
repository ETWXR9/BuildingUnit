package org.etwxr9.buildingunit;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.bukkit.plugin.java.JavaPlugin;
import org.etwxr9.buildingunit.commands.BUCommand;
import org.etwxr9.buildingunit.index.UnitIndex;
import org.etwxr9.buildingunit.persistence.SchematicRepository;
import org.etwxr9.buildingunit.persistence.UnitRepository;
import org.etwxr9.buildingunit.service.BuildingUnitService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BuildingUnitMain extends JavaPlugin {

    public static BuildingUnitMain i;

    private SchematicRepository schematicRepository;
    private UnitRepository unitRepository;
    private UnitIndex unitIndex;
    private BuildingUnitService buildingUnitService;

    @Override
    public void onEnable() {
        i = this;
        saveDefaultConfig();
        schematicRepository = new SchematicRepository(this);
        unitRepository = new UnitRepository(this);
        unitIndex = new UnitIndex();
        buildingUnitService = new BuildingUnitService(this, schematicRepository, unitRepository, unitIndex);
        buildingUnitService.reload();
        var command = getCommand("bu");
        if (command != null) {
            command.setExecutor(new BUCommand(buildingUnitService));
        }
        getLogger().info("BuildingUnit enabled with " + buildingUnitService.getAllUnits().size() + " loaded units.");
    }

    @Override
    public void onDisable() {
        if (buildingUnitService != null) {
            buildingUnitService.persistUnits();
        }
        getLogger().info("BuildingUnit disabled.");
    }

    public BuildingUnitService getBuildingUnitService() {
        return buildingUnitService;
    }

    public SchematicRepository getSchematicRepository() {
        return schematicRepository;
    }

    public UnitRepository getUnitRepository() {
        return unitRepository;
    }

    public UnitIndex getUnitIndex() {
        return unitIndex;
    }

    public List<UnitInfo> getUnitInfos() {
        return new ArrayList<>(buildingUnitService.getAllUnits());
    }

    public HashMap<String, Clipboard> getClipboards() {
        return new HashMap<>(buildingUnitService.getAllSchematics());
    }

    public void SaveUnitInfo() {
        buildingUnitService.persistUnits();
    }
}
