package com.mamiyaotaru.voxelmap.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;

public class GuiUtils {
    private static final Minecraft minecraft = Minecraft.getInstance();
    private static final ArrayList<Component> tooltipList = new ArrayList<>();

    public static void renderTooltip(GuiGraphics guiGraphics, Component tooltip, int mouseX, int mouseY) {
        renderTooltip(guiGraphics, tooltip.getString(), mouseX, mouseY);
    }

    public static void renderTooltip(GuiGraphics guiGraphics, String tooltip, int mouseX, int mouseY) {
        tooltipList.clear();
        for (FormattedText text : minecraft.font.getSplitter().splitLines(tooltip, 250, Style.EMPTY)) {
            tooltipList.add(Component.literal(text.getString()));
        }

        guiGraphics.renderComponentTooltip(minecraft.font, tooltipList, mouseX, mouseY);
    }

    public static void drawString(GuiGraphics drawContext, String text, float x, float y, int color, boolean shadow) {
        drawString(drawContext, Component.nullToEmpty(text), x, y, color, shadow);
    }

    public static void drawString(GuiGraphics drawContext, Component text, float x, float y, int color, boolean shadow) {
        drawContext.drawString(minecraft.font, text, (int) x, (int) y, color, shadow);
    }

    public static void drawCenteredString(GuiGraphics drawContext, String text, float x, float y, int color, boolean shadow) {
        drawCenteredString(drawContext, Component.nullToEmpty(text), x, y, color, shadow);
    }

    public static void drawCenteredString(GuiGraphics drawContext, Component text, float x, float y, int color, boolean shadow) {
        int halfWidth = minecraft.font.width(text) / 2;
        drawString(drawContext, text, x - halfWidth, y, color, shadow);
    }
}
