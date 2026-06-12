package com.vestalihy.client.gui;

import com.vestalihy.network.ModNetwork;
import com.vestalihy.network.OpticsOffsetPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class OpticsModuleScreen extends Screen {

    private final BlockPos blockPos;
    private float currentX;
    private float currentY;
    private float currentZ;

    public OpticsModuleScreen(BlockPos pos, float startX, float startY, float startZ) {
        super(Component.literal("Optics Module Calibration"));
        this.blockPos = pos;
        this.currentX = startX;
        this.currentY = startY;
        this.currentZ = startZ;
    }

    @Override
    protected void init() {
        float step = 0.0625f; // 1 pixel increment

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // X Axis
        this.addRenderableWidget(Button.builder(Component.literal("-X (East)"), b -> adjust(-step, 0, 0))
                .bounds(centerX - 100, centerY - 40, 80, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+X (West)"), b -> adjust(step, 0, 0))
                .bounds(centerX + 20, centerY - 40, 80, 20).build());

        // Y Axis
        this.addRenderableWidget(Button.builder(Component.literal("-Y (Down)"), b -> adjust(0, -step, 0))
                .bounds(centerX - 100, centerY - 10, 80, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+Y (Up)"), b -> adjust(0, step, 0))
                .bounds(centerX + 20, centerY - 10, 80, 20).build());

        // Z Axis
        this.addRenderableWidget(Button.builder(Component.literal("-Z (North)"), b -> adjust(0, 0, -step))
                .bounds(centerX - 100, centerY + 20, 80, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("+Z (South)"), b -> adjust(0, 0, step))
                .bounds(centerX + 20, centerY + 20, 80, 20).build());
    }

    private void adjust(float dx, float dy, float dz) {
        this.currentX += dx;
        this.currentY += dy;
        this.currentZ += dz;
        // Inform server
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new OpticsOffsetPacket(this.blockPos, this.currentX, this.currentY, this.currentZ));
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        String coords = String.format("X: %.3f   Y: %.3f   Z: %.3f", currentX, currentY, currentZ);
        pGuiGraphics.drawCenteredString(this.font, coords, this.width / 2, 40, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
