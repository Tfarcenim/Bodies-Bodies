package me.steven.bodiesbodies.data;

import me.steven.bodiesbodies.data.persistentstate.DeathData;
import me.steven.bodiesbodies.entity.DeadBodyEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface DeadBodyData {

    DeadBodyData transferFrom(Player player);

    void transferTo(LivingEntity entity);

    String getId();

    CompoundTag write(CompoundTag nbt);

    void read(CompoundTag nbt);

    AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player, DeathData data);

    boolean isEmpty();

    DeadBodyData deepCopy();

    default CompoundTag write(NonNullList<ItemStack> stacks) {
        CompoundTag nbt = new CompoundTag();
        ListTag inv = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            CompoundTag nbtCompound = new CompoundTag();
            nbtCompound.putInt("Slot", i);
            stacks.get(i).save(nbtCompound);
            inv.add(nbtCompound);
        }

        nbt.put("Stacks", inv);
        return nbt;
    }

    default void read(NonNullList<ItemStack> stacks, CompoundTag nbt) {
        ListTag inv = nbt.getList("Stacks", 10);
        for (int i = 0; i < inv.size(); i++) {
            CompoundTag nbtCompound = inv.getCompound(i);
            int slot = nbtCompound.getInt("Slot");
            stacks.set(slot, ItemStack.of(nbtCompound));
        }
    }

    default void offer(NonNullList<ItemStack> stacks, Level world, BlockPos pos, int i, ItemStack stack) {
        if (!stacks.get(i).isEmpty()) {
            int firstEmpty = getFirstEmptyIndex(stacks);
            if (firstEmpty != -1) {
                stacks.set(firstEmpty, stack.copyAndClear());
            } else {
                Containers.dropContents(world, pos, NonNullList.withSize(1, stack.copyAndClear()));
            }
        } else {
            stacks.set(i, stack.copyAndClear());
        }
    }

    default int getFirstEmptyIndex(NonNullList<ItemStack> stacks) {
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i).isEmpty()) return i;
        }
        return -1;
    }

}
