package me.steven.bodiesbodies;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.steven.bodiesbodies.data.persistentstate.DeathData;
import me.steven.bodiesbodies.data.persistentstate.DeathHistory;
import me.steven.bodiesbodies.data.persistentstate.PlayerBackup;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class BodiesBodiesCommands {
    public static void registerCommands() {
        registerDeathListCommand();
    }
    public static void registerDeathListCommand() {

        CommandRegistrationCallback.EVENT.register((commandDispatcher, b, env) ->
                commandDispatcher.register(Commands.literal("deathhistory")
                        .requires(s -> s.hasPermission(2))
                        .executes((ctx) -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            showDeathData(ctx, player.getUUID(), ctx.getSource().getLevel());
                            return 1;
                        }).then(Commands.argument("player", EntityArgument.player()).executes((ctx) -> {
                            Player player = EntityArgument.getPlayer(ctx, "player");
                            showDeathData(ctx, player.getUUID(), ctx.getSource().getLevel());
                            return 1;
                        }))));

        CommandRegistrationCallback.EVENT.register((commandDispatcher, b, env) ->
                commandDispatcher.register(Commands.literal("restorebackup")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("deathId", IntegerArgumentType.integer()).executes((ctx) -> {
                            restoreNbtBackup(ctx, IntegerArgumentType.getInteger(ctx, "deathId"), ctx.getSource().getLevel());
                            return 1;
                        }))));
    }

    private static void showDeathData(CommandContext<CommandSourceStack> ctx, UUID uuid, ServerLevel world) {
        List<DeathData> deathsFor = DeathHistory.getState(world).getDeathsFor(uuid);
        if (deathsFor == null || deathsFor.isEmpty()) {
            ctx.getSource().sendSystemMessage(Component.literal("No deaths for that player."));
            return;
        }

        FriendlyByteBuf buf = PacketByteBufs.create();

        buf.writeInt(deathsFor.size());
        List<DeathData> list = deathsFor.stream().sorted(Collections.reverseOrder(Comparator.comparingInt(DeathData::id))).limit(8).toList();
        for (int i = 0; i < list.size(); i++) {
            DeathData deathData = list.get(i);
            buf.writeInt(i);
            buf.writeNbt(deathData.writeNbt());
        }

        ServerPlayNetworking.send(ctx.getSource().getPlayer(), BodiesBodies.OPEN_DEATH_HISTORY, buf);
    }

    private static void restoreNbtBackup(CommandContext<CommandSourceStack> ctx, int id, ServerLevel world) {
        PlayerBackup backup = DeathHistory.getState(world).getPlayerNbtBackup().get(id);
        if (backup == null) {
            ctx.getSource().sendSystemMessage(Component.literal("No backup found for death id " + id));
            return;
        }

        CompoundTag nbt = backup.data();
        UUID uuid = nbt.getUUID("UUID");
        ServerPlayer player = ctx.getSource().getServer().getPlayerList().getPlayer(uuid);
        if (player == null) {
            ctx.getSource().sendSystemMessage(Component.literal("No player found for UUID " + uuid));
            return;
        }

        nbt.remove("Health");
        player.load(nbt);
    }
}
