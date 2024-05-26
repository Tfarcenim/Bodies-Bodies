package me.steven.bodiesbodies.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import me.steven.bodiesbodies.Config;
import me.steven.bodiesbodies.data.DeadBodyData;
import me.steven.bodiesbodies.data.DeadBodyDataProvider;
import me.steven.bodiesbodies.data.persistentstate.DeathData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Skeleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DeadBodyEntityRenderer extends EntityRenderer<DeadBodyEntity> {
    public static Skeleton fakeSkeleton = null;


    public static Skeleton getFakeSkeleton() {
        if (fakeSkeleton == null) {
            fakeSkeleton = new Skeleton(EntityType.SKELETON, Minecraft.getInstance().level);
            fakeSkeleton.setPose(Pose.SLEEPING);
            fakeSkeleton.yHeadRotO = 25;
            fakeSkeleton.setYHeadRot(25);
        }
        return fakeSkeleton;
    }

    public DeadBodyEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(DeadBodyEntity entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
        Minecraft client = Minecraft.getInstance();
        matrices.pushPose();
        matrices.translate(1.0, 0.0, 0.0);
        List<DeadBodyData> savedData = new ArrayList<>(DeadBodyDataProvider.initEmpty());
        savedData.addAll(DeadBodyDataProvider.initEmpty());
        for (DeadBodyData data : savedData) {
            data.read(entity.getEntityData().get(DeadBodyEntity.INVENTORY_DATA).getCompound(data.getId()));
        }
        Optional<UUID> playerUuid = entity.getPlayerUuid();
        if (playerUuid.isPresent() && entity.tickCount < Config.CONFIG.bodyTurnSkeletonTime) {
            UUID uuid = playerUuid.get();
            LocalPlayer copyPlayer = new LocalPlayer(client, client.level, new ClientPacketListener(null, null, new Connection(PacketFlow.CLIENTBOUND), null, new GameProfile(uuid, "null"), null), null, null, false, false) {
                @Override
                public boolean shouldShowName() {
                    return false;
                }
            };

            copyPlayer.setPose(Pose.SLEEPING);
            copyPlayer.yHeadRotO = 25;
            copyPlayer.setYHeadRot(25);
            for (DeadBodyData data : savedData) {
                data.transferTo(copyPlayer);
            }
            copyPlayer.setUUID(uuid);

            client.getEntityRenderDispatcher().getRenderer(copyPlayer).render(copyPlayer, 0f, tickDelta, matrices, vertexConsumers, light);
            matrices.popPose();
            return;

        }

        Skeleton fakeSkeleton = getFakeSkeleton();
        for (DeadBodyData data :  savedData) {
            data.transferTo(fakeSkeleton);
        }

        client.getEntityRenderDispatcher().getRenderer(fakeSkeleton).render(fakeSkeleton, 0f, tickDelta, matrices, vertexConsumers, light);
        matrices.popPose();

    }

    @Override
    public ResourceLocation getTexture(DeadBodyEntity entity) {
        return null;
    }
}
