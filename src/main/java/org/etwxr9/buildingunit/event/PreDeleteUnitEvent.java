package org.etwxr9.buildingunit.event;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.etwxr9.buildingunit.UnitInfo;

import java.util.function.Predicate;

public class PreDeleteUnitEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UnitInfo unitInfo;
    private final boolean clearEntities;
    private final Predicate<Entity> preserveEntities;
    private boolean cancelled;

    public PreDeleteUnitEvent(UnitInfo unitInfo, boolean clearEntities, Predicate<Entity> preserveEntities) {
        this.unitInfo = unitInfo;
        this.clearEntities = clearEntities;
        this.preserveEntities = preserveEntities;
    }

    public UnitInfo getUnitInfo() {
        return unitInfo;
    }

    public boolean isClearEntities() {
        return clearEntities;
    }

    public Predicate<Entity> getPreserveEntities() {
        return preserveEntities;
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
