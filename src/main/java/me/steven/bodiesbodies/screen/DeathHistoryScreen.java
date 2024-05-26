package me.steven.bodiesbodies.screen;

import me.steven.bodiesbodies.BodiesBodies;
import me.steven.bodiesbodies.data.persistentstate.DeathData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import javax.tools.Tool;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeathHistoryScreen extends Screen {

    protected int x;
    protected int y;
    protected int backgroundWidth;
    protected int backgroundHeight;
    public static final ResourceLocation DEATH_HISTORY_BACKGROUND = new ResourceLocation("bodiesbodies","textures/gui/death_history_background.png");
    private final List<DeathData> deaths;

    public DeathHistoryScreen(Component title, List<DeathData> deaths) {
        super(title);
        this.deaths = deaths;
    }

    private void showDeath(DeathData data, int index) {

        int yPos = this.y + this.backgroundHeight / 2 - (deaths.size() / 2 - index) * 23;
        int xPos = this.x + 5;
        this.addRenderableOnly((context, mouseX, mouseY, delta) -> context.blit(DEATH_HISTORY_BACKGROUND, xPos, yPos, 0f, 226f, 166, 23, 256, 256));

        StringWidget txt = new StringWidget(Component.literal("ID " + data.id()), font);
        txt.setPosition(xPos + 3, yPos + 7);
        this.addRenderableOnly(txt);

        this.addRenderableWidget(Button.builder(Component.literal("Inventory"), button -> {
                    FriendlyByteBuf buf = PacketByteBufs.create();
                    buf.writeInt(data.id());
                    buf.writeUtf("vanilla");
                    ClientPlayNetworking.send(BodiesBodies.OPEN_DEAD_BODY_INV, buf);
                })
                .pos(xPos + 166 - 63, yPos + 2)
                .size(60, 18)
                .tooltip(Tooltip.create(Component.literal("Click to open inventory")))
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Teleport"), button -> {
                    ChatScreen screen = new ChatScreen("/execute in " + data.dimension() + " run tp @p " + data.pos().getX() + " " + data.pos().getY() + " " + data.pos().getZ());
                    Minecraft.getInstance().setScreen(screen);
                })
                .pos(xPos + 52, yPos + 2)
                .size(50, 18)
                .tooltip(Tooltip.create(Component.literal("Teleport to death location: \nX: " + data.pos().getX() + " Y: " + data.pos().getY() + " Z: " + data.pos().getZ() + " (" + data.dimension() + ")")))
                .build());
    }

    @Override
    protected void init() {
        super.init();
        this.backgroundWidth = 175;
        this.backgroundHeight = 223;
        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;
        for (int i = 0; i < deaths.size(); i++) {
            DeathData death = deaths.get(i);
            showDeath(death, i);
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawString(this.font, Component.literal("Death History"), this.x + 8, this.y + 6, 4210752, false);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics context) {
        context.blit(DEATH_HISTORY_BACKGROUND, x, y, 0, 0.0F, 0.0F, backgroundWidth, backgroundHeight, 256, 256);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
