package me.steven.bodiesbodies.mixin;

import me.steven.bodiesbodies.BodiesBodies;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {
    @Shadow @Final public Player player;

    @Shadow public abstract boolean isEmpty();

    @Inject(method = "dropAll", at = @At("INVOKE"))
    private void bodiesbodies_saveItems(CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayerEntity && !isEmpty())
            BodiesBodies.createDeadBody(serverPlayerEntity);
    }
}
