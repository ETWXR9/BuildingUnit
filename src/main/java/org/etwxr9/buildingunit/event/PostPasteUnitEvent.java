package org.etwxr9.buildingunit.event;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.etwxr9.buildingunit.UnitInfo;

public class PostPasteUnitEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Clipboard clipboard;
    private final CuboidRegion region;
    private final String schematicName;
    private final UnitInfo unitInfo;
    private final Location location;

    public PostPasteUnitEvent(Clipboard clipboard, CuboidRegion region, String schematicName, UnitInfo unitInfo, Location location) {
        this.clipboard = clipboard;
        this.region = region;
        this.schematicName = schematicName;
        this.unitInfo = unitInfo;
        this.location = location;
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

    public UnitInfo getUnitInfo() {
        return unitInfo;
    }

    public Location getLocation() {
        return location;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
