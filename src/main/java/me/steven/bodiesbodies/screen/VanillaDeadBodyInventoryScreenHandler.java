package me.steven.bodiesbodies.screen;

import com.mojang.datafixers.util.Pair;
import me.steven.bodiesbodies.BodiesBodies;
import me.steven.bodiesbodies.data.VanillaDeadBodyData;
import me.steven.bodiesbodies.data.persistentstate.DeathData;
import me.steven.bodiesbodies.entity.DeadBodyEntity;
import me.steven.bodiesbodies.utils.Utils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static net.minecraft.world.inventory.InventoryMenu.*;

public class VanillaDeadBodyInventoryScreenHandler extends AbstractContainerMenu {
    private static final ResourceLocation[] EMPTY_ARMOR_SLOT_TEXTURES = new ResourceLocation[]{EMPTY_ARMOR_SLOT_BOOTS, EMPTY_ARMOR_SLOT_LEGGINGS, EMPTY_ARMOR_SLOT_CHESTPLATE, EMPTY_ARMOR_SLOT_HELMET};
    private static final EquipmentSlot[] EQUIPMENT_SLOT_ORDER = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    public final DeathData deathData;
    public VanillaDeadBodyInventoryScreenHandler(int syncId, Inventory inventory, DeathData deathData, VanillaDeadBodyData data) {
        super(BodiesBodies.VANILLA_DEAD_BODY_SH, syncId);
        this.deathData = deathData;

        SimpleContainer armorInv = Utils.toSimpleInventory(data.armor);
        for(int i = 0; i < 4; ++i) {
            final EquipmentSlot equipmentSlot = EQUIPMENT_SLOT_ORDER[i];
            this.addSlot(new Slot(armorInv, 3-i, 8 + i* 18, 23) {

                public int getMaxStackSize() {
                    return 1;
                }

                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                    return Pair.of(InventoryMenu.BLOCK_ATLAS, EMPTY_ARMOR_SLOT_TEXTURES[equipmentSlot.getIndex()]);
                }
            });
        }
        SimpleContainer mainInv = Utils.toSimpleInventory(data.main);

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(mainInv, j + (i + 1) * 9, 8 + j * 18, 45 + i * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        for(int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(mainInv, i, 8 + i * 18, 103){
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        SimpleContainer offHandInv = Utils.toSimpleInventory(data.offHand);

        this.addSlot(new Slot(offHandInv, 0, 98, 23) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });

        for(int i = 0; i < 3; ++i) {
            for(int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inventory, j + (i + 1) * 9, 8 + j * 18, 142 + i * 18));
            }
        }

        for(int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 200));
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
