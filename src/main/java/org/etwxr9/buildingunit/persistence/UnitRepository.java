package org.etwxr9.buildingunit.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.etwxr9.buildingunit.UnitInfo;
import org.etwxr9.buildingunit.service.BuildingUnitService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UnitRepository {

    private static final Type STORED_RECORD_LIST_TYPE = new TypeToken<List<StoredUnitRecord>>() {
    }.getType();
    private static final Type LEGACY_RECORD_LIST_TYPE = new TypeToken<List<LegacyUnitRecord>>() {
    }.getType();

    private final JavaPlugin plugin;
    private final Gson gson;
    private final Path currentUnitsFile;
    private final Path legacyUnitsFile;

    public UnitRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.currentUnitsFile = plugin.getDataFolder().toPath().resolve("units-v2.json");
        this.legacyUnitsFile = plugin.getDataFolder().toPath().resolve("Units.json");
    }

    public List<UnitInfo> loadAll(SchematicRepository schematicRepository) {
        try {
            Files.createDirectories(plugin.getDataFolder().toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create plugin data directory.", e);
        }
        if (Files.exists(currentUnitsFile)) {
            return readCurrentUnits();
        }
        if (Files.exists(legacyUnitsFile)) {
            List<UnitInfo> migrated = migrateLegacyUnits(schematicRepository);
            saveAll(migrated);
            backupLegacyUnits();
            return migrated;
        }
        saveAll(List.of());
        return List.of();
    }

    public void saveAll(Collection<UnitInfo> units) {
        List<StoredUnitRecord> records = new ArrayList<>(units.size());
        for (UnitInfo unit : units) {
            records.add(new StoredUnitRecord(unit));
        }
        try {
            Files.writeString(currentUnitsFile, gson.toJson(records, STORED_RECORD_LIST_TYPE));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save units.", e);
        }
    }

    private List<UnitInfo> readCurrentUnits() {
        try {
            String json = Files.readString(currentUnitsFile);
            List<StoredUnitRecord> records = gson.fromJson(json, STORED_RECORD_LIST_TYPE);
            if (records == null) {
                return new ArrayList<>();
            }
            List<UnitInfo> units = new ArrayList<>(records.size());
            for (StoredUnitRecord record : records) {
                units.add(record.toUnitInfo());
            }
            return units;
        } catch (IOException | JsonParseException e) {
            quarantineCorruptedFile(currentUnitsFile);
            plugin.getLogger().warning("Failed to read units-v2.json, starting with empty state: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<UnitInfo> migrateLegacyUnits(SchematicRepository schematicRepository) {
        try {
            String json = Files.readString(legacyUnitsFile);
            List<LegacyUnitRecord> records = gson.fromJson(json, LEGACY_RECORD_LIST_TYPE);
            if (records == null) {
                return new ArrayList<>();
            }
            List<UnitInfo> migrated = new ArrayList<>();
            for (LegacyUnitRecord record : records) {
                Clipboard clipboard = schematicRepository.get(record.name);
                if (clipboard == null) {
                    plugin.getLogger().warning("Skipping legacy unit " + record.uuid + " because schematic " + record.name + " is missing.");
                    continue;
                }
                CuboidRegion region = BuildingUnitService.computePasteRegion(
                        clipboard,
                        new Location(plugin.getServer().getWorld(record.world), record.x, record.y, record.z),
                        record.rotate);
                if (region == null) {
                    plugin.getLogger().warning("Skipping legacy unit " + record.uuid + " because target region cannot be computed.");
                    continue;
                }
                migrated.add(new UnitInfo(
                        record.name,
                        record.world,
                        record.x,
                        record.y,
                        record.z,
                        record.rotate,
                        record.uuid,
                        region.getMinimumPoint().x(),
                        region.getMinimumPoint().y(),
                        region.getMinimumPoint().z(),
                        region.getMaximumPoint().x(),
                        region.getMaximumPoint().y(),
                        region.getMaximumPoint().z(),
                        Instant.now().toEpochMilli()));
            }
            return migrated;
        } catch (IOException | JsonParseException e) {
            quarantineCorruptedFile(legacyUnitsFile);
            plugin.getLogger().warning("Failed to migrate legacy units, starting with empty state: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void quarantineCorruptedFile(Path source) {
        try {
            if (!Files.exists(source)) {
                return;
            }
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(Instant.now().atZone(ZoneId.systemDefault()));
            Files.move(source, source.resolveSibling(source.getFileName() + ".corrupted-" + timestamp), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private void backupLegacyUnits() {
        try {
            if (Files.exists(legacyUnitsFile)) {
                Files.move(legacyUnitsFile, legacyUnitsFile.resolveSibling("Units.json.bak"), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to back up legacy Units.json: " + e.getMessage());
        }
    }

    private static final class LegacyUnitRecord {
        private String name;
        private String world;
        private int x;
        private int y;
        private int z;
        private int rotate;
        private String uuid;
    }
}
