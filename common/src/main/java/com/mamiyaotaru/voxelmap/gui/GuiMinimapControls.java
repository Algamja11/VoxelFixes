package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

public class GuiMinimapControls extends GuiScreenMinimap {
    protected String screenTitle = "Controls";
    private GuiSlotKeyMapping keymapList;

    public GuiMinimapControls(Screen par1GuiScreen) {
        this.parentScreen = par1GuiScreen;
    }

    public void init() {
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)).bounds(this.getWidth() / 2 - 100, this.getHeight() - 28, 200, 20).build());
        this.screenTitle = I18n.get("controls.voxelmap.title");

        this.keymapList = new GuiSlotKeyMapping(this);
        this.addRenderableWidget(this.keymapList);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.keymapList.keyEditing()) {
            return this.keymapList.keyPressed(keyCode, scanCode, modifiers);
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderDefaultBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.getFont(), I18n.get("controls.voxelmap.unbind1"), this.getWidth() / 2, this.getHeight() - 64, 16777215);
        guiGraphics.drawCenteredString(this.getFont(), I18n.get("controls.voxelmap.unbind2"), this.getWidth() / 2, this.getHeight() - 48, 16777215);
        guiGraphics.drawCenteredString(this.getFont(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        super.render(guiGraphics, mouseX, mouseY, delta);
    }
}