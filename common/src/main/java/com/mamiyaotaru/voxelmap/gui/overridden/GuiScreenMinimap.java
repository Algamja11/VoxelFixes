package com.mamiyaotaru.voxelmap.gui.overridden;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;

public class GuiScreenMinimap extends Screen {
    protected Screen parentScreen;
    private final ArrayList<Component> tooltipList = new ArrayList<>();

    protected GuiScreenMinimap() {
        this (Component.literal(""));
    }

    protected GuiScreenMinimap(Component title) {
        super (title);
    }

    public void removed() {
        MapSettingsManager.instance.saveAll();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<? extends GuiEventListener> getButtonList() {
        return children();
    }

    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
    }

    public void renderDefaultBackground(GuiGraphics context) {
        this.renderBlurredBackground();
        this.renderMenuBackground(context);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            VoxelConstants.getMinecraft().setScreen(this.parentScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void renderTooltip(GuiGraphics guiGraphics, Component tooltip, int mouseX, int mouseY) {
        renderTooltip(guiGraphics, tooltip.getString(), mouseX, mouseY);
    }

    public void renderTooltip(GuiGraphics guiGraphics, String tooltip, int mouseX, int mouseY) {
        tooltipList.clear();
        for (FormattedText text : font.getSplitter().splitLines(tooltip, 250, Style.EMPTY)) {
            tooltipList.add(Component.literal(text.getString()));
        }

        guiGraphics.renderComponentTooltip(font, tooltipList, mouseX, mouseY);
    }
}