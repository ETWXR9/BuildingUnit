package org.etwxr9.buildingunit.index;

import org.etwxr9.buildingunit.UnitInfo;
import org.etwxr9.buildingunit.util.ChunkKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WorldUnitBucket {

    private final Map<String, UnitInfo> unitsByUuidRef = new HashMap<>();
    private final Map<Long, Set<String>> unitIdsByChunkKey = new HashMap<>();

    public void add(UnitInfo unitInfo) {
        unitsByUuidRef.put(unitInfo.getUuid(), unitInfo);
        for (long chunkKey : unitInfo.getCoveredChunkKeys()) {
            unitIdsByChunkKey.computeIfAbsent(chunkKey, ignored -> new LinkedHashSet<>()).add(unitInfo.getUuid());
        }
    }

    public void remove(UnitInfo unitInfo) {
        unitsByUuidRef.remove(unitInfo.getUuid());
        for (long chunkKey : unitInfo.getCoveredChunkKeys()) {
            Set<String> ids = unitIdsByChunkKey.get(chunkKey);
            if (ids == null) {
                continue;
            }
            ids.remove(unitInfo.getUuid());
            if (ids.isEmpty()) {
                unitIdsByChunkKey.remove(chunkKey);
            }
        }
    }

    public UnitInfo findByLocation(int x, int y, int z) {
        Set<String> ids = unitIdsByChunkKey.get(ChunkKey.of(ChunkKey.toChunk(x), ChunkKey.toChunk(z)));
        if (ids == null) {
            ids = Collections.emptySet();
        }
        for (String id : ids) {
            UnitInfo unitInfo = unitsByUuidRef.get(id);
            if (unitInfo != null && unitInfo.isLocationInside(unitInfo.getWorldName(), x, y, z)) {
                return unitInfo;
            }
        }
        return null;
    }

    public List<UnitInfo> findOverlapping(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int normalizedMinX = Math.min(minX, maxX);
        int normalizedMinY = Math.min(minY, maxY);
        int normalizedMinZ = Math.min(minZ, maxZ);
        int normalizedMaxX = Math.max(minX, maxX);
        int normalizedMaxY = Math.max(minY, maxY);
        int normalizedMaxZ = Math.max(minZ, maxZ);
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        for (int chunkX = ChunkKey.toChunk(normalizedMinX); chunkX <= ChunkKey.toChunk(normalizedMaxX); chunkX++) {
            for (int chunkZ = ChunkKey.toChunk(normalizedMinZ); chunkZ <= ChunkKey.toChunk(normalizedMaxZ); chunkZ++) {
                Set<String> ids = unitIdsByChunkKey.get(ChunkKey.of(chunkX, chunkZ));
                if (ids != null) {
                    candidates.addAll(ids);
                }
            }
        }
        List<UnitInfo> result = new ArrayList<>();
        for (String id : candidates) {
            UnitInfo unitInfo = unitsByUuidRef.get(id);
            if (unitInfo != null && unitInfo.isOverlap(unitInfo.getWorldName(),
                    normalizedMinX, normalizedMinY, normalizedMinZ,
                    normalizedMaxX, normalizedMaxY, normalizedMaxZ)) {
                result.add(unitInfo);
            }
        }
        return result;
    }

    public Collection<UnitInfo> getAllUnits() {
        return unitsByUuidRef.values();
    }

    public int getChunkBucketCount() {
        return unitIdsByChunkKey.size();
    }
}
