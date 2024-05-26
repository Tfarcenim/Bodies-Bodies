package me.steven.bodiesbodies.data.persistentstate;

import me.steven.bodiesbodies.data.DeadBodyData;
import me.steven.bodiesbodies.data.DeadBodyDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record DeathData(int id, BlockPos pos, ResourceLocation dimension, List<DeadBodyData> savedData, long createdTime) {

    public CompoundTag writeNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("id", id);
        nbt.put("pos", NbtUtils.writeBlockPos(pos));
        nbt.putString("dim", dimension.toString());
        for (DeadBodyData data : savedData) {
            nbt.put(data.getId(), data.write(new CompoundTag()));
        }
        nbt.putLong("CreatedAt", createdTime);
        return nbt;
    }

    public static DeathData readNbt(CompoundTag nbt) {
        int id = nbt.getInt("id");
        BlockPos pos = NbtUtils.readBlockPos(nbt.getCompound("pos"));
        ResourceLocation dimension = new ResourceLocation(nbt.getString("dim"));
        List<DeadBodyData> savedData = new ArrayList<>(DeadBodyDataProvider.initEmpty());
        for (DeadBodyData data : savedData) {
            data.read(nbt.getCompound(data.getId()));
        }
        long createdTime = nbt.getLong("CreatedAt");
        return new DeathData(id, pos, dimension, savedData, createdTime);
    }
}
