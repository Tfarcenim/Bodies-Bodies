package me.steven.bodiesbodies.utils;

import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public class Utils {
    public static SimpleContainer toSimpleInventory(NonNullList<ItemStack> stacks) {
        SimpleContainer inv = new SimpleContainer(stacks.size());
        for (int i = 0; i < inv.getContainerSize(); i++) {
            inv.setItem(i, stacks.get(i));
        }
        return inv;
    }
    public static boolean isEmpty(NonNullList<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }
}
