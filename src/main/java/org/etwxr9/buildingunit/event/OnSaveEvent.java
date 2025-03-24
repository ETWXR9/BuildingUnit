package org.etwxr9.buildingunit.event;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.nio.file.Path;

public class OnSaveEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    public OnSaveEvent(Clipboard clipboard, Path filePath) {
        this.clipboard = clipboard;
        this.filePath = filePath;
    }

    public Clipboard getClipboard() {
        return clipboard;
    }

    private final Clipboard clipboard;
    private final Path filePath;

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public Path getFilePath() {
        return filePath;
    }
}
