package me.steven.bodiesbodies.compat;

import me.steven.bodiesbodies.data.persistentstate.DeathData;
import me.steven.bodiesbodies.utils.Utils;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class TrinketsDeadBodyInventoryScreenHandler extends AbstractContainerMenu {
    public final DeathData deathData;
    public TrinketsDeadBodyInventoryScreenHandler(int syncId, Inventory inventory, DeathData deathData, TrinketsDeadBodyData data) {
        super(TrinketCompat.TRINKETS_DEAD_BODY_SH, syncId);
        this.deathData = deathData;

        List<SimpleContainer> inventories = data.inventory.values().stream().map(stringDefaultedListMap -> stringDefaultedListMap.values().stream().map(Utils::toSimpleInventory).toList()).flatMap(Collection::stream).toList();
        Iterator<SimpleContainer> it = inventories.iterator();
        if (!it.hasNext()) return;
        SimpleContainer current = it.next();
        int slotIndex = 0;
        outer:
        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(current, slotIndex, 8 + j * 18, 28 + i * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
                slotIndex++;
                if (current.getContainerSize() <= slotIndex) {
                    if (!it.hasNext()) break outer;
                    current = it.next();
                }
            }
        }

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + (i + 1) * 9, 8 + j * 18, 94 + i * 18));
            }
        }

        for(int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 152));
        }

    }

    public ItemStack quickMoveStack(Player player, int slot) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot2 = this.slots.get(slot);
        if (slot2.hasItem()) {
            ItemStack itemStack2 = slot2.getItem();
            itemStack = itemStack2.copy();
            EquipmentSlot equipmentSlot = Mob.getEquipmentSlotForItem(itemStack);
            if (slot == 0) {
                if (!this.moveItemStackTo(itemStack2, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }

                slot2.onQuickCraft(itemStack2, itemStack);
            } else if (slot >= 1 && slot < 5) {
                if (!this.moveItemStackTo(itemStack2, 9, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slot >= 5 && slot < 9) {
                if (!this.moveItemStackTo(itemStack2, 9, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipmentSlot.getType() == EquipmentSlot.Type.ARMOR && !((Slot)this.slots.get(8 - equipmentSlot.getIndex())).hasItem()) {
                int i = 8 - equipmentSlot.getIndex();
                if (!this.moveItemStackTo(itemStack2, i, i + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (equipmentSlot == EquipmentSlot.OFFHAND && !((Slot)this.slots.get(45)).hasItem()) {
                if (!this.moveItemStackTo(itemStack2, 45, 46, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slot >= 9 && slot < 36) {
                if (!this.moveItemStackTo(itemStack2, 36, 45, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slot >= 36 && slot < 45) {
                if (!this.moveItemStackTo(itemStack2, 9, 36, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 9, 45, false)) {
                return ItemStack.EMPTY;
            }

            if (itemStack2.isEmpty()) {
                slot2.setByPlayer(ItemStack.EMPTY);
            } else {
                slot2.setChanged();
            }

            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot2.onTake(player, itemStack2);
            if (slot == 0) {
                player.drop(itemStack2, false);
            }
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
