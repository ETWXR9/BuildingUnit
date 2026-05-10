package org.etwxr9.buildingunit.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.etwxr9.buildingunit.UnitInfo;

public class PostDeleteUnitEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UnitInfo unitInfo;

    public PostDeleteUnitEvent(UnitInfo unitInfo) {
        this.unitInfo = unitInfo;
    }

    public UnitInfo getUnitInfo() {
        return unitInfo;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
