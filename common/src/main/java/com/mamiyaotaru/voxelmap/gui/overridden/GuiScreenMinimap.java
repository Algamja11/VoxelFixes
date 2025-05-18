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

    protected GuiScreenMinimap() {
        this (Component.literal(""));
    }

    protected GuiScreenMinimap(Component title) {
        super (title);
    }

    @Override
    public void removed() {
        MapSettingsManager.instance.saveAll();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBlurredBackground();
        this.renderMenuBackground(guiGraphics);
        guiGraphics.flush();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            VoxelConstants.getMinecraft().setScreen(this.parentScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}