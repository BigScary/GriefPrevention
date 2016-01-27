package me.ryanhamshire.GriefPrevention;

import java.util.UUID;

public class ChunkLocationInfo {
    int x, z;
    UUID worldUUID;

    public ChunkLocationInfo(int x, int z, UUID worldUUID) {
        this.x = x;
        this.z = z;
        this.worldUUID = worldUUID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChunkLocationInfo that = (ChunkLocationInfo) o;

        if (x != that.x) return false;
        if (z != that.z) return false;
        return worldUUID.equals(that.worldUUID);

    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + z;
        result = 31 * result + worldUUID.hashCode();
        return result;
    }
}