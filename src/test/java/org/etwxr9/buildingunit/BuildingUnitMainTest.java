package org.etwxr9.buildingunit;

import org.etwxr9.buildingunit.index.UnitIndex;
import org.etwxr9.buildingunit.persistence.StoredUnitRecord;
import org.etwxr9.buildingunit.util.ChunkKey;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingUnitMainTest {

    @Test
    void unitRecordRoundTripKeepsStableBounds() {
        UnitInfo original = new UnitInfo("tower", "world", 10, 64, 10, 1, "u1", 8, 64, 8, 12, 70, 12, 1000L);
        UnitInfo restored = new StoredUnitRecord(original).toUnitInfo();
        assertEquals(original.getUuid(), restored.getUuid());
        assertEquals(original.getSchematicName(), restored.getSchematicName());
        assertEquals(original.getMinX(), restored.getMinX());
        assertEquals(original.getMaxZ(), restored.getMaxZ());
        assertTrue(restored.isLocationInside("world", 10, 65, 10));
    }

    @Test
    void chunkKeySupportsNegativeCoordinates() {
        long keyA = ChunkKey.of(-1, -2);
        long keyB = ChunkKey.of(-1, -2);
        assertEquals(keyA, keyB);
        assertEquals(-1, ChunkKey.toChunk(-1));
        assertEquals(-1, ChunkKey.toChunk(-16));
        assertEquals(-2, ChunkKey.toChunk(-17));
    }

    @Test
    void unitIndexFindsUnitsByUuidNameLocationAndOverlap() {
        UnitInfo a = new UnitInfo("tower", "world", 0, 64, 0, 0, "a", 0, 64, 0, 15, 80, 15, 1L);
        UnitInfo b = new UnitInfo("tower", "world", 32, 64, 32, 0, "b", 32, 64, 32, 47, 80, 47, 2L);
        UnitInfo c = new UnitInfo("farm", "world_nether", 0, 64, 0, 0, "c", 0, 64, 0, 15, 80, 15, 3L);
        UnitIndex index = new UnitIndex();
        index.rebuild(List.of(a, b, c));

        assertEquals(a, index.getByUuid("a"));
        assertEquals(2, index.getByName("tower").size());
        assertEquals(a, index.getByLocation("world", 1, 70, 1));
        assertNull(index.getByLocation("world", 20, 70, 20));
        assertEquals(1, index.getOverlapping("world", 10, 64, 10, 20, 70, 20).size());
        assertEquals(1, index.getOverlapping("world_nether", 0, 64, 0, 10, 70, 10).size());
    }

    @Test
    void unitIndexRemovesChunkRegistrations() {
        UnitInfo unit = new UnitInfo("wall", "world", 0, 64, 0, 0, "wall-1", 0, 64, 0, 31, 70, 31, 1L);
        UnitIndex index = new UnitIndex();
        index.add(unit);
        assertNotNull(index.getByLocation("world", 20, 65, 20));
        index.remove(unit);
        assertNull(index.getByUuid("wall-1"));
        assertNull(index.getByLocation("world", 20, 65, 20));
        assertTrue(index.getOverlapping("world", 0, 64, 0, 31, 70, 31).isEmpty());
    }
}
