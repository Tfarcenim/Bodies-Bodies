package me.steven.bodiesbodies.compat;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import me.steven.bodiesbodies.data.DeadBodyData;
import me.steven.bodiesbodies.data.persistentstate.DeathData;
import me.steven.bodiesbodies.entity.DeadBodyEntity;
import me.steven.bodiesbodies.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.AbstractCollection;
import java.util.HashMap;
import java.util.Map;

public class TrinketsDeadBodyData implements DeadBodyData {
    public Map<String, Map<String, NonNullList<ItemStack>>> inventory = new HashMap<>();
    @Override
    public DeadBodyData transferFrom(Player player) {
        TrinketsApi.getTrinketComponent(player).map(TrinketComponent::getInventory).ifPresent(inv -> {
            inv.forEach((key, value) -> {
                Map<String, NonNullList<ItemStack>> aaa = new HashMap<>();
                value.forEach((slot, trinketInv) -> {
                    int size = trinketInv.getContainerSize();
                    NonNullList<ItemStack> stacks = NonNullList.withSize(size, ItemStack.EMPTY);
                    for (int i = 0; i < size; i++) {
                        stacks.set(i, trinketInv.getItem(i).copyAndClear());
                    }
                    aaa.put(slot, stacks);
                });
                inventory.put(key, aaa);
            });
        });
        return this;
    }


    @Override
    public void transferTo(LivingEntity entity) {
        if (entity instanceof Player player) {
            TrinketsApi.getTrinketComponent(player).map(TrinketComponent::getInventory).ifPresent(inv -> {
                inventory.forEach((key, value) -> {
                    value.forEach((slot, stacks) -> {
                        for (int i = 0; i < stacks.size(); i++) {
                            offer(inv.get(key).get(slot), entity.level(), entity.blockPosition(), i, stacks.get(i).copyAndClear(), player.getInventory().items);
                        }
                    });
                });
            });
        }
    }

    private void offer(TrinketInventory stacks, Level world, BlockPos pos, int i, ItemStack stack, NonNullList<ItemStack> fallback) {
        if (!stacks.getItem(i).isEmpty()) {
            int firstEmpty = getFirstEmptyIndex(fallback);
            if (firstEmpty != -1) {
                fallback.set(firstEmpty, stack.copyAndClear());
            } else {
                Containers.dropContents(world, pos, NonNullList.withSize(1, stack.copyAndClear()));
            }
        } else {
            stacks.setItem(i, stack.copyAndClear());
        }
    }

    @Override
    public String getId() {
        return "trinkets";
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {
        CompoundTag trinketNbt = new CompoundTag();
        inventory.forEach((key, value) -> {
            CompoundTag nbtKey = new CompoundTag();
            value.forEach((slot, stacks) -> nbtKey.put(slot, write(stacks)));
            trinketNbt.put(key, nbtKey);
        });
        nbt.put("TrinketData", trinketNbt);
        return nbt;
    }

    @Override
    public void read(CompoundTag nbt) {
        CompoundTag trinketNbt = nbt.getCompound("TrinketData");
        for (String key : trinketNbt.getAllKeys()) {
            CompoundTag nbtKey = trinketNbt.getCompound(key);
            Map<String, NonNullList<ItemStack>> map = new HashMap<>();
            for (String slot : nbtKey.getAllKeys()) {
                NonNullList<ItemStack> stacks = NonNullList.withSize(nbtKey.getCompound(slot).size(), ItemStack.EMPTY);
                read(stacks, nbtKey.getCompound(slot));
                map.put(slot, stacks);
            }
            inventory.put(key, map);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player, DeathData data) {
        return new TrinketsDeadBodyInventoryScreenHandler(syncId, playerInventory, data, this);
    }

    @Override
    public boolean isEmpty() {
        return inventory.values().stream().allMatch((e) -> e.values().stream().allMatch(Utils::isEmpty));
    }

    @Override
    public DeadBodyData deepCopy() {
        Map<String, Map<String, NonNullList<ItemStack>>> copy = new HashMap<>();
        inventory.forEach((key, value) -> {
            Map<String, NonNullList<ItemStack>> aaa = new HashMap<>();
            value.forEach((slot, stacks) -> {
                NonNullList<ItemStack> stacksCopy = NonNullList.withSize(stacks.size(), ItemStack.EMPTY);
                for (int i = 0; i < stacks.size(); i++) {
                    stacksCopy.set(i, stacks.get(i).copy());
                }
                aaa.put(slot, stacksCopy);
            });
            copy.put(key, aaa);
        });
        TrinketsDeadBodyData copyData = new TrinketsDeadBodyData();
        copyData.inventory = copy;
        return copyData;
    }
}
