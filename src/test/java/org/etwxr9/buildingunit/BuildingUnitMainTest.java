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
        System.out.println(gson.toJson(new Location(null, 1, 0, 1).serialize()));
        System.out.println(new Vector(1, 0, 1).serialize());
        System.out.println(gson.toJson(new Vector(1, 0, 1).rotateAroundY(Math.toRadians(90)).toBlockVector()));
    }

    // boolean iso(Vector min, Vector max, Vector min2, Vector max2) {
    //     return !(min.getBlockX() > max2.getBlockX() || max.getBlockX() < min2.getBlockX() ||
    //              min.getBlockY() > max2.getBlockY() || max.getBlockY() < min2.getBlockY() ||
    //              min.getBlockZ() > max2.getBlockZ() || max.getBlockZ() < min2.getBlockZ());
    // }

    // @org.junit.jupiter.api.Test
    // void isOverlapTest() {
    //     Vector min = new Vector(8, -59, 9);
    //     Vector max = new Vector(8 + 8, -59 + 20, 9 + 8);
    //     Vector min2 = new Vector(7, -59, 10);
    //     Vector max2 = new Vector(7 + 8, -59 + 20, 10 + 8);

    //     // assertTrue(iso(min, max, min2, max2));

    //     Vector min3 = new Vector(1, -40, 2);
    //     Vector max3 = new Vector(1 + 8, -40 + 20, 2 + 8);
    //     Vector min4 = new Vector(0, -40, 3);
    //     Vector max4 = new Vector(8, -40 + 20, 3 + 8);

    //     assertTrue(iso(min3, max3, min4, max4));
    // }
}