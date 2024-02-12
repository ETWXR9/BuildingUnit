package org.etwxr9.buildingunit;

import com.google.gson.Gson;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

class BuildingUnitMainTest {

    @org.junit.jupiter.api.Test
    void savetest() {
        Gson gson = new Gson();
        System.out.println(gson.toJson(new Location(null,1,0,1).serialize()));
        System.out.println(new Vector(1,0,1).serialize());
        System.out.println(gson.toJson(new Vector(1,0,1).rotateAroundY(Math.toRadians(90)).toBlockVector()));
    }
}