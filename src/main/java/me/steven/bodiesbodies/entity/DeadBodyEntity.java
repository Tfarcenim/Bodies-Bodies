package me.steven.bodiesbodies.entity;

import me.steven.bodiesbodies.BodiesBodies;
import me.steven.bodiesbodies.Config;
import me.steven.bodiesbodies.data.DeadBodyData;
import me.steven.bodiesbodies.data.DeadBodyDataProvider;
import me.steven.bodiesbodies.data.persistentstate.DeathData;
import me.steven.bodiesbodies.data.persistentstate.DeathHistory;
import me.steven.bodiesbodies.data.VanillaDeadBodyData;
import me.steven.bodiesbodies.screen.VanillaDeadBodyInventoryScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DeadBodyEntity extends Entity {

    public static final EntityDataAccessor<CompoundTag> INVENTORY_DATA = SynchedEntityData.defineId(DeadBodyEntity.class, EntityDataSerializers.COMPOUND_TAG);
    public static final EntityDataAccessor<Optional<UUID>> PLAYER_UUID = SynchedEntityData.defineId(DeadBodyEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private int deathDataId;
    private int emptyTimer = 0;

    public DeadBodyEntity(EntityType<?> type, Level world) {
        super(type, world);
        this.entityData.define(PLAYER_UUID, Optional.empty());
        this.entityData.define(INVENTORY_DATA, new CompoundTag());
        this.blocksBuilding = false;
        this.setPose(Pose.SLEEPING);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        ServerLevel world = (ServerLevel) level();
        if (Config.CONFIG.nonEmptyBodyDisappearAfter > 0 && tickCount > Config.CONFIG.nonEmptyBodyDisappearAfter) {
            discard();
        }
        DeathData deathData = getDeathData(world);

        if (deathData != null) {
            for (DeadBodyData data : deathData.savedData()) {
                if (!data.isEmpty()) return;
            }
        }

        emptyTimer++;
        if (Config.CONFIG.emptyBodyDisappearAfter > 0 && emptyTimer > Config.CONFIG.emptyBodyDisappearAfter) {
            discard();
        }
    }

    public DeathData getDeathData(ServerLevel world) {
        return DeathHistory.getState(world).getDeathData(getPlayerUUID(), deathDataId);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    public static DeadBodyEntity create(ServerPlayer player) {
        DeathHistory history = DeathHistory.getState(player.serverLevel());
        int id = history.backup(player);
        System.out.println("Player " + player + " died. Death ID: " + id);

        DeadBodyEntity deadBody = new DeadBodyEntity(BodiesBodies.DEAD_BODY_ENTITY_TYPE, player.level());
        deadBody.setPosRaw(player.getX(), player.getY(), player.getZ());
        deadBody.entityData.set(PLAYER_UUID, Optional.of(player.getUUID()));
        List<DeadBodyData> savedData = new ArrayList<>(DeadBodyDataProvider.init(player));

        CompoundTag nbt = new CompoundTag();
        for (DeadBodyData data : savedData) {
            nbt.put(data.getId(), data.write(new CompoundTag()));
        }
        deadBody.entityData.set(INVENTORY_DATA, nbt);

        deadBody.setOldPosAndRot();
        deadBody.reapplyPosition();


        deadBody.deathDataId = history.save(id, player,deadBody.blockPosition(), savedData);

        return deadBody;
    }

    public UUID getPlayerUUID() {
        return entityData.get(PLAYER_UUID).get();
    }
    @Override
    protected AABB makeBoundingBox() {
        AABB box = super.makeBoundingBox();
        return new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ).contract(0.0, 1.2, 0.0).inflate(0.6, 0.0, 0.1);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;

        if (Config.CONFIG.bodyAccessibleByAnyoneAfter > 0 && tickCount < Config.CONFIG.bodyAccessibleByAnyoneAfter && entityData.get(PLAYER_UUID).isPresent() && !player.getUUID().equals(entityData.get(PLAYER_UUID).get())){
            player.sendSystemMessage(Component.literal("This body does not belong to you!"));
            return InteractionResult.PASS;
        }

        ServerLevel world = (ServerLevel) player.level();
        DeathData deathData = getDeathData(world);

        if (player.isShiftKeyDown()) {
            for (DeadBodyData data : deathData.savedData()) {
                data.transferTo(player);
            }
        } else {
            for (DeadBodyData data : deathData.savedData()) {
                if (data instanceof VanillaDeadBodyData vanillaDeadBodyData) {
                    player.openMenu(new ExtendedScreenHandlerFactory() {
                        @Override
                        public void writeScreenOpeningData(ServerPlayer player, FriendlyByteBuf buf) {
                            buf.writeInt(deathData.id());
                            buf.writeNbt(deathData.writeNbt());
                        }

                        @Override
                        public Component getDisplayName() {
                            return Component.literal("Dead body");
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                            return new VanillaDeadBodyInventoryScreenHandler(syncId, playerInventory, getDeathData(world), vanillaDeadBodyData);
                        }
                    });
                }
            }

        }
        return InteractionResult.SUCCESS;
    }

    public Optional<UUID> getPlayerUuid() {
        return entityData.get(PLAYER_UUID);
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.entityData.set(PLAYER_UUID, Optional.of(nbt.getUUID("PlayerUUID")));
        this.deathDataId = nbt.getInt("DeathDataId");
        ServerLevel world = (ServerLevel) level();
        DeathData deathData = getDeathData(world);
        if (deathData != null) {
            CompoundTag newNbt = new CompoundTag();
            for (DeadBodyData data : deathData.savedData()) {
                newNbt.put(data.getId(), data.write(new CompoundTag()));
            }
            this.entityData.set(INVENTORY_DATA, newNbt);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("DeathDataId", deathDataId);
        nbt.putUUID("PlayerUUID", this.entityData.get(PLAYER_UUID).orElse(null));
    }
}
