package me.steven.bodiesbodies.data.persistentstate;

import net.minecraft.nbt.CompoundTag;

public record PlayerBackup(CompoundTag data, long createdTime) {
    public CompoundTag writeNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.put("Backup", this.data);
        nbt.putLong("CreatedAt", createdTime);
        return nbt;
    }

    public static PlayerBackup readNbt(CompoundTag nbt) {
        CompoundTag backup = nbt.getCompound("Backup");
        long createdTime = nbt.getLong("CreatedAt");
        return new PlayerBackup(backup, createdTime);
    }
}
