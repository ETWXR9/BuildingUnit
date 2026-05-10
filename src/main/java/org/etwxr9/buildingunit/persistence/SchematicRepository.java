package org.etwxr9.buildingunit.persistence;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class SchematicRepository {

    private final JavaPlugin plugin;
    private final Path schematicDirectory;
    private final Map<String, Clipboard> clipboards = new HashMap<>();

    public SchematicRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.schematicDirectory = plugin.getDataFolder().toPath().resolve("Schematic");
    }

    public void reload() {
        clipboards.clear();
        try {
            Files.createDirectories(schematicDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create schematic directory.", e);
        }
        try (Stream<Path> stream = Files.list(schematicDirectory)) {
            stream.filter(Files::isRegularFile).forEach(this::loadClipboardFile);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read schematic directory.", e);
        }
    }

    private void loadClipboardFile(Path path) {
        ClipboardFormat format = ClipboardFormats.findByFile(path.toFile());
        if (format == null) {
            return;
        }
        try (ClipboardReader reader = format.getReader(new FileInputStream(path.toFile()))) {
            String fileName = path.getFileName().toString();
            int extensionIndex = fileName.lastIndexOf('.');
            String schematicName = extensionIndex == -1 ? fileName : fileName.substring(0, extensionIndex);
            clipboards.put(schematicName, reader.read());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load schematic file: " + path + " - " + e.getMessage());
        }
    }

    public void save(String name, Clipboard clipboard) {
        try {
            Files.createDirectories(schematicDirectory);
            Path filePath = resolvePath(name);
            try (ClipboardWriter writer = BuiltInClipboardFormat.FAST.getWriter(new FileOutputStream(filePath.toFile()))) {
                writer.write(clipboard);
            }
            clipboards.put(name, clipboard);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save schematic " + name, e);
        }
    }

    public Clipboard get(String name) {
        return clipboards.get(name);
    }

    public boolean exists(String name) {
        return clipboards.containsKey(name);
    }

    public Map<String, Clipboard> getAll() {
        return Collections.unmodifiableMap(clipboards);
    }

    public Path resolvePath(String name) {
        return schematicDirectory.resolve(name + ".sche");
    }
}
