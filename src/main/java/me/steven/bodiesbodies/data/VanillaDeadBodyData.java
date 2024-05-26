package me.steven.bodiesbodies.data;

import me.steven.bodiesbodies.data.persistentstate.DeathData;
import me.steven.bodiesbodies.entity.DeadBodyEntity;
import me.steven.bodiesbodies.screen.VanillaDeadBodyInventoryScreenHandler;
import me.steven.bodiesbodies.utils.Utils;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class VanillaDeadBodyData implements DeadBodyData {
    public NonNullList<ItemStack> main;
    public NonNullList<ItemStack> armor;
    public NonNullList<ItemStack> offHand;
    public int selectedSlot;
    public VanillaDeadBodyData() {
        this.main = NonNullList.withSize(36, ItemStack.EMPTY);
        this.armor = NonNullList.withSize(4, ItemStack.EMPTY);
        this.offHand = NonNullList.withSize(1, ItemStack.EMPTY);
    }

    @Override
    public DeadBodyData transferFrom(Player player) {
        Inventory inv = player.getInventory();
        main = NonNullList.withSize(inv.items.size(), ItemStack.EMPTY);
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack stack = inv.items.get(i);
            main.set(i, stack.copyAndClear());
        }

        armor = NonNullList.withSize(inv.armor.size(), ItemStack.EMPTY);
        for (int i = 0; i < inv.armor.size(); i++) {
            ItemStack stack = inv.armor.get(i);
            armor.set(i, stack.copyAndClear());
        }

        offHand = NonNullList.withSize(inv.offhand.size(), ItemStack.EMPTY);
        for (int i = 0; i < inv.offhand.size(); i++) {
            ItemStack stack = inv.offhand.get(i);
            offHand.set(i, stack.copyAndClear());
        }

        selectedSlot = inv.selected;

        return this;
    }

    @Override
    public String getId() {
        return "vanilla";
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {
        nbt.put("main", write(main));
        nbt.put("armor", write(armor));
        nbt.put("offhand", write(offHand));
        return nbt;
    }

    @Override
    public void read(CompoundTag nbt) {
        read(main, nbt.getCompound("main"));
        read(armor, nbt.getCompound("armor"));
        read(offHand, nbt.getCompound("offhand"));
    }

    @Override
    public void transferTo(LivingEntity entity) {
        if (entity instanceof Player player) {
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.items.size(); i++) {
                offer(inv.items, entity.level(), entity.blockPosition(), i, main.get(i));
            }
            for (int i = 0; i < inv.armor.size(); i++) {
                offer(inv.armor, entity.level(), entity.blockPosition(), i, armor.get(i));
            }
            for (int i = 0; i < inv.offhand.size(); i++) {
                offer(inv.offhand, entity.level(), entity.blockPosition(), i, offHand.get(i));
            }
        } else if (entity instanceof Skeleton skeleton) {
            skeleton.setItemSlot(EquipmentSlot.HEAD, armor.get(3));
            skeleton.setItemSlot(EquipmentSlot.CHEST, armor.get(2));
            skeleton.setItemSlot(EquipmentSlot.LEGS, armor.get(1));
            skeleton.setItemSlot(EquipmentSlot.FEET, armor.get(0));
            skeleton.setItemSlot(EquipmentSlot.MAINHAND, main.get(selectedSlot));
            skeleton.setItemSlot(EquipmentSlot.OFFHAND, offHand.get(0));
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player, DeathData data) {
        return new VanillaDeadBodyInventoryScreenHandler(syncId, playerInventory, data, this);
    }

    @Override
    public boolean isEmpty() {
        return Utils.isEmpty(main) && Utils.isEmpty(offHand) && Utils.isEmpty(armor);
    }

    @Override
    public DeadBodyData deepCopy() {
        VanillaDeadBodyData data = new VanillaDeadBodyData();
        for (int i = 0; i < data.main.size(); i++) {
            data.main.set(i, main.get(i).copy());
        }

        for (int i = 0; i < data.armor.size(); i++) {
            data.armor.set(i, armor.get(i).copy());
        }

        for (int i = 0; i < data.offHand.size(); i++) {
            data.offHand.set(i, offHand.get(i).copy());
        }
        return data;
    }
}
