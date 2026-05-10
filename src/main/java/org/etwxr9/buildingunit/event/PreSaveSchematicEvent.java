package org.etwxr9.buildingunit.event;

import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PreSaveSchematicEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Location min;
    private final Location max;
    private final String name;
    private final Location origin;
    private boolean cancelled;

    public PreSaveSchematicEvent(Location min, Location max, String name, Location origin) {
        this.min = min;
        this.max = max;
        this.name = name;
        this.origin = origin;
    }

    public Location getMin() {
        return min;
    }

    public Location getMax() {
        return max;
    }

    public String getName() {
        return name;
    }

    public Location getOrigin() {
        return origin;
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
