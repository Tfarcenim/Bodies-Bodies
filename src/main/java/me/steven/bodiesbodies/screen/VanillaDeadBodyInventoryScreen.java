package me.steven.bodiesbodies.screen;

import me.steven.bodiesbodies.BodiesBodies;
import me.steven.bodiesbodies.data.DeadBodyData;
import me.steven.bodiesbodies.data.persistentstate.DeathData;
import me.steven.bodiesbodies.entity.DeadBodyEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import java.util.List;

public class VanillaDeadBodyInventoryScreen extends AbstractContainerScreen<VanillaDeadBodyInventoryScreenHandler> {
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("bodiesbodies", "textures/gui/dead_body_inventory.png");
    private final DeathData deathData;

    public VanillaDeadBodyInventoryScreen(VanillaDeadBodyInventoryScreenHandler handler, Inventory inventory, Component title, DeathData deathData) {
        super(handler, inventory, title);
        this.imageWidth = 175;
        this.imageHeight = 223;
        this.inventoryLabelY = 130;
        this.deathData = deathData;
    }

    @Override
    protected void init() {
        super.init();
        ImageButton btn = new ImageButton(this.leftPos + this.imageWidth - 5 - 20, this.topPos + 5, 20, 18, 178, 0, 19, INVENTORY_LOCATION, (button) -> {
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeInt(deathData.id());
            ClientPlayNetworking.send(BodiesBodies.TRANSFER_ALL_ITEMS_PACKET, buf);
        });
        btn.setTooltip(Tooltip.create(Component.literal("Transfer all items")));
        this.addRenderableWidget(btn);

        List<DeadBodyData> savedData = deathData.savedData();
        for (int i = 0; i < savedData.size(); i++) {
            DeadBodyData data = savedData.get(i);
            ImageButton dataBtn = new ImageButton(this.leftPos + this.imageWidth + 2, this.topPos + i * 22, 65, 20, 178, 38, 21, INVENTORY_LOCATION, (button) -> {
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeInt(deathData.id());
                buf.writeUtf(data.getId());
                ClientPlayNetworking.send(BodiesBodies.OPEN_DEAD_BODY_INV, buf);
            }) {
                @Override
                public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
                    super.renderWidget(context, mouseX, mouseY, delta);
                    MutableComponent txt = Component.translatable("bodiesbodies.data." + data.getId());
                    context.drawString(font, txt, this.getX() + this.getWidth() / 2 - font.width(txt)/2, this.getY() + this.getHeight()/2 - font.lineHeight/2, -1, false);
                }
            };
            dataBtn.setTooltip(Tooltip.create(Component.translatable("bodiesbodies.data." + data.getId())));
            this.addRenderableWidget(dataBtn);
        }
    }

    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        context.blit(INVENTORY_LOCATION, leftPos, topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}
