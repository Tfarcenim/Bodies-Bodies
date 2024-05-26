package me.steven.bodiesbodies.compat;

import dev.emi.trinkets.TrinketScreenManager;
import me.steven.bodiesbodies.BodiesBodies;
import me.steven.bodiesbodies.data.DeadBodyData;
import me.steven.bodiesbodies.data.DeadBodyDataProvider;
import me.steven.bodiesbodies.data.VanillaDeadBodyData;
import me.steven.bodiesbodies.data.persistentstate.DeathData;
import me.steven.bodiesbodies.entity.DeadBodyEntity;
import me.steven.bodiesbodies.screen.VanillaDeadBodyInventoryScreen;
import me.steven.bodiesbodies.screen.VanillaDeadBodyInventoryScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

public class TrinketCompat {
    public static final MenuType<TrinketsDeadBodyInventoryScreenHandler> TRINKETS_DEAD_BODY_SH = Registry.register(BuiltInRegistries.MENU, new ResourceLocation("bodiesbodies", "trinkets_dead_body"), new ExtendedScreenHandlerType<>((syncId, inventory, buf) -> {
        int id = buf.readInt();
        DeathData deathData = DeathData.readNbt(buf.readNbt());
        for (DeadBodyData data : deathData.savedData()) {
            if (data instanceof TrinketsDeadBodyData trinketsDeadBodyData) {
                return new TrinketsDeadBodyInventoryScreenHandler(syncId, inventory, deathData, trinketsDeadBodyData);
            }
        }
        return null;
    }));

    public static void load() {
        DeadBodyDataProvider.register(TrinketsDeadBodyData::new);
    }
}
