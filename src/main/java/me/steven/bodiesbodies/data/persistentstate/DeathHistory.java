package me.steven.bodiesbodies.data.persistentstate;

import me.steven.bodiesbodies.data.DeadBodyData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DeathHistory extends SavedData {


    private final ServerLevel world;
    private int deathCounter = 0;
    private final Map<UUID, List<DeathData>> storedDeaths = new HashMap<>();
    private final Map<Integer, PlayerBackup> playerNbtBackup = new HashMap<>();

    public DeathHistory(ServerLevel world) {
        this.world = world;
    }

    public List<DeathData> getDeathsFor(UUID uuid) {
        return storedDeaths.get(uuid);
    }


    public Map<Integer, PlayerBackup> getPlayerNbtBackup() {
        return playerNbtBackup;
    }

    public void removeDeathData(UUID uuid, int id) {
        storedDeaths.get(uuid).removeIf(d -> d.id() == id);
        playerNbtBackup.remove(id);
        setDirty();
    }

    @Nullable
    public DeathData getDeathData(UUID uuid, int id) {
        if (!storedDeaths.containsKey(uuid)) return null;

        for (DeathData deathData : storedDeaths.get(uuid)) {
            if (deathData.id() == id) return deathData;
        }
        return null;
    }

    public int save(int id, ServerPlayer player, BlockPos pos, List<DeadBodyData> data) {
        DeathData deathData = new DeathData(id, pos, player.level().dimensionTypeId().location(), data, System.currentTimeMillis());
        List<DeathData> deaths = storedDeaths.computeIfAbsent(player.getUUID(), (x) -> new ArrayList<>());
        deaths.add(deathData);
        setDirty();
        return deathData.id();
    }

    public int backup(ServerPlayer player) {
        int id = deathCounter++;
        playerNbtBackup.put(id, new PlayerBackup(player.saveWithoutId(new CompoundTag()), System.currentTimeMillis()));
        setDirty();
        return id;
    }

    public int purgeOldEntries(DeathHistory purged) {
        AtomicInteger i = new AtomicInteger(0);
        playerNbtBackup.entrySet().removeIf(e -> {
            boolean remove = e.getValue().createdTime() + TimeUnit.DAYS.toMillis(7) < System.currentTimeMillis();
            if (remove) {
                i.getAndIncrement();
                purged.playerNbtBackup.put(e.getKey(), e.getValue());
            }
            return remove;
        });
        storedDeaths.forEach((uuid, deaths) -> deaths.removeIf(d -> {
            boolean remove = d.createdTime() + TimeUnit.DAYS.toMillis(7) < System.currentTimeMillis();
            if (remove) {
                i.getAndIncrement();
                purged.storedDeaths.computeIfAbsent(uuid, (x) -> new ArrayList<>()).add(d);
            }
            return remove;
        }));
        if (i.get()>0) {
            System.out.println("Moved 1 week old entries to death_history_purged.dat");
            setDirty();
        }
        return i.get();

    }

    @Override
    public CompoundTag save(CompoundTag nbt) {

        CompoundTag rawPlayerNbt = new CompoundTag();
        playerNbtBackup.forEach((id, playerNbt) -> {
            rawPlayerNbt.put(id.toString(), playerNbt.writeNbt());

        });

        nbt.put("RawNbt", rawPlayerNbt);
        nbt.putInt("deathCounter", deathCounter);
        CompoundTag deathsNbt = new CompoundTag();
        for (Map.Entry<UUID, List<DeathData>> entry : storedDeaths.entrySet()) {
            UUID uuid = entry.getKey();
            List<DeathData> deaths = entry.getValue();
            ListTag list = new ListTag();
            for (DeathData death : deaths) {
                list.add(death.writeNbt());
            }

            deathsNbt.put(uuid.toString(), list);
        }
        nbt.put("deaths", deathsNbt);


        return nbt;
    }

    public static DeathHistory readNbt(ServerLevel world, CompoundTag nbt) {
        DeathHistory data = new DeathHistory(world);

        CompoundTag rawNbt = nbt.getCompound("RawNbt");
        for (String key : rawNbt.getAllKeys()) {
            int id = Integer.parseInt(key);
            data.playerNbtBackup.put(id, PlayerBackup.readNbt(rawNbt.getCompound(key)));
        }

        data.deathCounter = nbt.getInt("deathCounter");
        CompoundTag deathsNbt = nbt.getCompound("deaths");
        for (String uuidAsString : deathsNbt.getAllKeys()) {
            UUID uuid = UUID.fromString(uuidAsString);
            List<DeathData> deaths = new ArrayList<>();
            ListTag list = deathsNbt.getList(uuidAsString, 10);
            for (Tag element : list) {
                deaths.add(DeathData.readNbt((CompoundTag) element));
            }
            data.storedDeaths.put(uuid, deaths);
        }

        return data;
    }

    public static DeathHistory getState(ServerLevel world) {
        return world.getServer().overworld().getDataStorage()
                .computeIfAbsent(nbt -> DeathHistory.readNbt(world, nbt), () -> new DeathHistory(world), "death_history");
    }
}
