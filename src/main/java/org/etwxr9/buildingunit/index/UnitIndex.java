package org.etwxr9.buildingunit.index;

import org.etwxr9.buildingunit.UnitInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UnitIndex {

    private final Map<String, UnitInfo> unitsByUuid = new HashMap<>();
    private final Map<String, Set<String>> unitIdsBySchematicName = new HashMap<>();
    private final Map<String, WorldUnitBucket> bucketsByWorld = new HashMap<>();

    public void clear() {
        unitsByUuid.clear();
        unitIdsBySchematicName.clear();
        bucketsByWorld.clear();
    }

    public void rebuild(Collection<UnitInfo> units) {
        clear();
        for (UnitInfo unit : units) {
            add(unit);
        }
    }

    public void add(UnitInfo unitInfo) {
        unitsByUuid.put(unitInfo.getUuid(), unitInfo);
        unitIdsBySchematicName.computeIfAbsent(unitInfo.getSchematicName(), ignored -> new LinkedHashSet<>()).add(unitInfo.getUuid());
        bucketsByWorld.computeIfAbsent(unitInfo.getWorldName(), ignored -> new WorldUnitBucket()).add(unitInfo);
    }

    public void remove(UnitInfo unitInfo) {
        unitsByUuid.remove(unitInfo.getUuid());
        Set<String> ids = unitIdsBySchematicName.get(unitInfo.getSchematicName());
        if (ids != null) {
            ids.remove(unitInfo.getUuid());
            if (ids.isEmpty()) {
                unitIdsBySchematicName.remove(unitInfo.getSchematicName());
            }
        }
        WorldUnitBucket worldBucket = bucketsByWorld.get(unitInfo.getWorldName());
        if (worldBucket != null) {
            worldBucket.remove(unitInfo);
            if (worldBucket.getAllUnits().isEmpty()) {
                bucketsByWorld.remove(unitInfo.getWorldName());
            }
        }
    }

    public UnitInfo getByUuid(String uuid) {
        return unitsByUuid.get(uuid);
    }

    public List<UnitInfo> getByName(String schematicName) {
        Set<String> ids = unitIdsBySchematicName.get(schematicName);
        if (ids == null) {
            ids = Collections.emptySet();
        }
        List<UnitInfo> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            UnitInfo unitInfo = unitsByUuid.get(id);
            if (unitInfo != null) {
                result.add(unitInfo);
            }
        }
        return result;
    }

    public UnitInfo getByLocation(String worldName, int x, int y, int z) {
        WorldUnitBucket bucket = bucketsByWorld.get(worldName);
        return bucket == null ? null : bucket.findByLocation(x, y, z);
    }

    public List<UnitInfo> getOverlapping(String worldName, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        WorldUnitBucket bucket = bucketsByWorld.get(worldName);
        return bucket == null ? List.of() : bucket.findOverlapping(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public Collection<UnitInfo> getAllUnits() {
        return Collections.unmodifiableCollection(unitsByUuid.values());
    }

    public int getWorldBucketCount() {
        return bucketsByWorld.size();
    }

    public int getChunkBucketCount() {
        return bucketsByWorld.values().stream().mapToInt(WorldUnitBucket::getChunkBucketCount).sum();
    }
}
