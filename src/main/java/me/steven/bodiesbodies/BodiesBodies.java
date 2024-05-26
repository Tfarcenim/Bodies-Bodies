package me.steven.bodiesbodies;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.steven.bodiesbodies.compat.TrinketCompat;
import me.steven.bodiesbodies.data.DeadBodyData;
import me.steven.bodiesbodies.data.DeadBodyDataProvider;
import me.steven.bodiesbodies.data.VanillaDeadBodyData;
import me.steven.bodiesbodies.data.persistentstate.DeathData;
import me.steven.bodiesbodies.data.persistentstate.DeathHistory;
import me.steven.bodiesbodies.entity.DeadBodyEntity;
import me.steven.bodiesbodies.screen.VanillaDeadBodyInventoryScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

public class BodiesBodies implements ModInitializer {

    public static final EntityType<DeadBodyEntity> DEAD_BODY_ENTITY_TYPE = Registry.register(BuiltInRegistries.ENTITY_TYPE, new ResourceLocation("bodiesbodies", "dead_body"), EntityType.Builder.of(DeadBodyEntity::new, MobCategory.MISC).sized(0.6F, 1.8F).build("dead_body"));

    public static final MenuType<VanillaDeadBodyInventoryScreenHandler> VANILLA_DEAD_BODY_SH = Registry.register(BuiltInRegistries.MENU, new ResourceLocation("bodiesbodies", "vanilla_dead_body"), new ExtendedScreenHandlerType<>((syncId, inventory, buf) -> {
        int id = buf.readInt();
        DeathData deathData = DeathData.readNbt(buf.readNbt());
        for (DeadBodyData data : deathData.savedData()) {
            if (data instanceof VanillaDeadBodyData vanillaDeadBodyData) {
                return new VanillaDeadBodyInventoryScreenHandler(syncId, inventory, deathData, vanillaDeadBodyData);
            }
        }
        return null;
    }));

    public static final ResourceLocation TRANSFER_ALL_ITEMS_PACKET = new ResourceLocation("bodiesbodies", "transfer_all");
    public static final ResourceLocation OPEN_DEAD_BODY_INV = new ResourceLocation("bodiesbodies", "open_inv");
    public static final ResourceLocation OPEN_DEATH_HISTORY = new ResourceLocation("bodiesbodies", "death_history");

    @Override
    public void onInitialize() {
        File file = new File(FabricLoader.getInstance().getConfigDir().toFile(), "bodiesbodies.json");
        try {
            if (!file.exists()) {
                file.createNewFile();
                Files.writeString(file.toPath(), new GsonBuilder().setPrettyPrinting().create().toJson(Config.CONFIG));

            } else {
                Config.CONFIG = new GsonBuilder().create().fromJson(Files.readString(file.toPath(), Charset.defaultCharset()), Config.class);
            }
        } catch (IOException e) {
            Config.CONFIG = new Config();
            System.out.println("Error while loading Bodies! Bodies! config, loading default");
        }

        DeadBodyDataProvider.register(VanillaDeadBodyData::new);

        ServerPlayNetworking.registerGlobalReceiver(TRANSFER_ALL_ITEMS_PACKET, (server, player, handler, buf, responseSender) -> {
            int deathId = buf.readInt();
            server.execute(() -> {
                DeathData deathData = DeathHistory.getState(player.serverLevel()).getDeathData(player.getUUID(), deathId);
                if (deathData != null)
                    for (DeadBodyData data : deathData.savedData()) {
                        data.transferTo(player);
                    }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(OPEN_DEAD_BODY_INV, (server, player, handler, buf, responseSender) -> {
            int deathId = buf.readInt();
            String invId = buf.readUtf();
            server.execute(() -> {
                DeathData deathData = DeathHistory.getState(player.serverLevel()).getDeathData(player.getUUID(), deathId);
                if (deathData != null)
                    for (DeadBodyData data : deathData.savedData()) {
                        if (data.getId().equals(invId)) {
                            player.openMenu(new ExtendedScreenHandlerFactory() {
                                @Override
                                public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
                                    buf.writeInt(deathId);
                                    buf.writeNbt(deathData.writeNbt());
                                }

                                @Override
                                public Component getDisplayName() {
                                    return Component.literal("Dead body");
                                }

                                @Nullable
                                @Override
                                public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                                    return data.createMenu(syncId, playerInventory, player, deathData);
                                }
                            });
                        }

                    }
            });
        });

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            ServerLevel world = server.overworld();
            DeathHistory data = DeathHistory.getState(world);
            DeathHistory purged = world.getDataStorage()
                    .computeIfAbsent(nbt -> DeathHistory.readNbt(world, nbt), () -> new DeathHistory(world), "death_history_purged");
            int purgedCount = data.purgeOldEntries(purged);
            if (purgedCount > 0)
                purged.setDirty();
        });

        BodiesBodiesCommands.registerCommands();

        if (FabricLoader.getInstance().isModLoaded("trinkets"))
            TrinketCompat.load();
    }

    public static void createDeadBody(ServerPlayer player) {
        DeadBodyEntity deadBodyEntity = DeadBodyEntity.create(player);
        player.level().addFreshEntity(deadBodyEntity);
    }
}
