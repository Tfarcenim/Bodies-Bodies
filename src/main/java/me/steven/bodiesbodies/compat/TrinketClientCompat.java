package me.steven.bodiesbodies.compat;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TrinketClientCompat {
    public static void loadClient() {
        MenuScreens.register(TrinketCompat.TRINKETS_DEAD_BODY_SH, new MenuScreens.ScreenConstructor<TrinketsDeadBodyInventoryScreenHandler, TrinketsDeadBodyInventoryScreen>() {
            @Override
            public TrinketsDeadBodyInventoryScreen create(TrinketsDeadBodyInventoryScreenHandler handler, Inventory playerInventory, Component title) {
                return new TrinketsDeadBodyInventoryScreen(handler, playerInventory, title, handler.deathData);
            }
        });
    }
}
