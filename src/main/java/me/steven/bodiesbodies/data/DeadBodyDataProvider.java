package me.steven.bodiesbodies.data;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.Player;

public interface DeadBodyDataProvider {
    List<DeadBodyDataProvider> PROVIDERS = new ArrayList<>();

    static void register(DeadBodyDataProvider provider) {
        PROVIDERS.add(provider);
    }

    static List<DeadBodyData> init(Player player) {
        List<DeadBodyData> data = new ArrayList<>();
        for (DeadBodyDataProvider provider : PROVIDERS) {
            data.add(provider.create().transferFrom(player));
        }
        return data;
    }

    static List<DeadBodyData> initEmpty() {
        List<DeadBodyData> data = new ArrayList<>();
        for (DeadBodyDataProvider provider : PROVIDERS) {
            data.add(provider.create());
        }
        return data;
    }
    DeadBodyData create();
}
