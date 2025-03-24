package org.etwxr9.buildingunit.event;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.etwxr9.buildingunit.UnitInfo;

import java.nio.file.Path;

public class OnPasteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    public Clipboard getClipboard() {
        return clipboard;
    }

    public CuboidRegion getRegion() {
        return region;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    private final Clipboard clipboard;
    private final CuboidRegion region;
    private final String name;

    public OnPasteEvent(Clipboard clipboard, CuboidRegion region, String name, UnitInfo unitinfo, Location location) {
        this.clipboard = clipboard;
        this.region = region;
        this.name = name;
        this.unitinfo = unitinfo;
        this.location = location;
    }

    public UnitInfo getUnitinfo() {
        return unitinfo;
    }

    private final UnitInfo unitinfo;
    private final Location location;

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

}
