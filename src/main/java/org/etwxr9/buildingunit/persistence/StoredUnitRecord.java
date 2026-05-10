package org.etwxr9.buildingunit.persistence;

import org.etwxr9.buildingunit.UnitInfo;

public class StoredUnitRecord {

    private int schemaVersion;
    private String uuid;
    private String schematicName;
    private String worldName;
    private int originX;
    private int originY;
    private int originZ;
    private int rotationQuarterTurns;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;
    private long createdAtEpochMilli;

    public StoredUnitRecord() {
    }

    public StoredUnitRecord(UnitInfo unitInfo) {
        this.schemaVersion = 2;
        this.uuid = unitInfo.getUuid();
        this.schematicName = unitInfo.getSchematicName();
        this.worldName = unitInfo.getWorldName();
        this.originX = unitInfo.getOriginX();
        this.originY = unitInfo.getOriginY();
        this.originZ = unitInfo.getOriginZ();
        this.rotationQuarterTurns = unitInfo.getRotate();
        this.minX = unitInfo.getMinX();
        this.minY = unitInfo.getMinY();
        this.minZ = unitInfo.getMinZ();
        this.maxX = unitInfo.getMaxX();
        this.maxY = unitInfo.getMaxY();
        this.maxZ = unitInfo.getMaxZ();
        this.createdAtEpochMilli = unitInfo.getCreatedAtEpochMilli();
    }

    public UnitInfo toUnitInfo() {
        return new UnitInfo(
                schematicName,
                worldName,
                originX,
                originY,
                originZ,
                rotationQuarterTurns,
                uuid,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                createdAtEpochMilli);
    }
}
