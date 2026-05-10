package org.etwxr9.buildingunit.event;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.nio.file.Path;

public class PostSaveSchematicEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Clipboard clipboard;
    private final Path filePath;
    private final String name;

    public PostSaveSchematicEvent(Clipboard clipboard, Path filePath, String name) {
        this.clipboard = clipboard;
        this.filePath = filePath;
        this.name = name;
    }

    public Clipboard getClipboard() {
        return clipboard;
    }

    public Path getFilePath() {
        return filePath;
    }

    public String getName() {
        return name;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
