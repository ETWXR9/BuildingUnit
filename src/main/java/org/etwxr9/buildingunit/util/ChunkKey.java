package org.etwxr9.buildingunit.util;

public final class ChunkKey {

    private ChunkKey() {
    }

    public static long of(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
    }

    public static int toChunk(int blockCoordinate) {
        return blockCoordinate >> 4;
    }
}
