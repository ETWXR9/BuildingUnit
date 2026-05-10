package org.etwxr9.buildingunit.event;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Location;
import org.etwxr9.buildingunit.UnitInfo;

@Deprecated
public class OnPasteEvent extends PostPasteUnitEvent {

    public OnPasteEvent(Clipboard clipboard, CuboidRegion region, String name, UnitInfo unitInfo, Location location) {
        super(clipboard, region, name, unitInfo, location);
    }

    public String getName() {
        return getSchematicName();
    }

    public UnitInfo getUnitinfo() {
        return getUnitInfo();
    }
}
