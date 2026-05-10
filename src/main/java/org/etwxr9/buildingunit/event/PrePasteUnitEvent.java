package org.etwxr9.buildingunit.event;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PrePasteUnitEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Clipboard clipboard;
    private final CuboidRegion region;
    private final String schematicName;
    private final Location location;
    private final int rotate;
    private final boolean ignoreAirBlocks;
    private boolean cancelled;

    public PrePasteUnitEvent(Clipboard clipboard, CuboidRegion region, String schematicName, Location location, int rotate, boolean ignoreAirBlocks) {
        this.clipboard = clipboard;
        this.region = region;
        this.schematicName = schematicName;
        this.location = location;
        this.rotate = rotate;
        this.ignoreAirBlocks = ignoreAirBlocks;
    }

    public Clipboard getClipboard() {
        return clipboard;
    }

    public CuboidRegion getRegion() {
        return region;
    }

    public String getSchematicName() {
        return schematicName;
    }

    public Location getLocation() {
        return location;
    }

    public int getRotate() {
        return rotate;
    }

    public boolean isIgnoreAirBlocks() {
        return ignoreAirBlocks;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
