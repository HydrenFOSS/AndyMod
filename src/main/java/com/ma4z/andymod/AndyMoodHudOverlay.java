package com.ma4z.andymod.client;

import com.ma4z.andymod.AndyEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.List;

public class AndyMoodHudOverlay {
    public static final ResourceLocation LOGO_TEXTURE = new ResourceLocation("andymod", "textures/gui/logo.png");

    public static final IGuiOverlay HUD_BAR = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        List<AndyEntity> andies = player.level().getEntitiesOfClass(AndyEntity.class, player.getBoundingBox().inflate(30.0D));
        if (andies.isEmpty()) return;

        AndyEntity andy = andies.get(0);
        int mood = andy.getMood();

        int barWidth = 6;
        int barHeight = 80;
        int x = width - barWidth - 20;
        int y = (height / 2) - (barHeight / 2);

        drawRoundedRect(guiGraphics, x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0xFF000000);

        int fillColor;
        if (mood >= 80) {
            fillColor = 0xFF55FF55;
        } else if (mood >= 60) {
            fillColor = 0xFF22AA22;
        } else if (mood >= 40) {
            fillColor = 0xFFFFD700;
        } else if (mood >= 20) {
            fillColor = 0xFFFF8C00;
        } else {
            fillColor = 0xFFFF2222;
        }

        int scaledHeight = (int) (barHeight * (mood / 100.0F));
        if (scaledHeight > 0) {
            int fillY = y + barHeight - scaledHeight;
            drawRoundedRect(guiGraphics, x, fillY, x + barWidth, y + barHeight, fillColor);
        }

        int logoSize = 16;
        int logoX = x - ((logoSize - barWidth) / 2);
        int logoY = y - logoSize - 6;

        RenderSystem.setShaderTexture(0, LOGO_TEXTURE);
        RenderSystem.enableBlend();
        guiGraphics.blit(LOGO_TEXTURE, logoX, logoY, 0, 0, logoSize, logoSize, logoSize, logoSize);
        RenderSystem.disableBlend();
    };

    private static void drawRoundedRect(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left + 1, top, right - 1, bottom, color);
        graphics.fill(left, top + 1, right, bottom - 1, color);
    }
}