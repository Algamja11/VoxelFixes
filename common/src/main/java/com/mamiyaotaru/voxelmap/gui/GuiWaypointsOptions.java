package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionSliderMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiWaypointsOptions extends GuiScreenMinimap {
    private static final EnumOptionsMinimap[] relevantOptions = { EnumOptionsMinimap.WAYPOINT_DISTANCE, EnumOptionsMinimap.AUTO_UNIT_CONVERSION, EnumOptionsMinimap.SHOW_NAME_LABEL, EnumOptionsMinimap.SHOW_DISTANCE_LABEL, EnumOptionsMinimap.DEATHPOINTS };
    private final MapSettingsManager options;
    protected Component screenTitle;

    public GuiWaypointsOptions(Screen parent, MapSettingsManager options) {
        this.parentScreen = parent;
        this.options = options;
    }

    public void init() {
        int var2 = 0;
        this.screenTitle = Component.translatable("options.voxelmap.waypoints.title");

        for (EnumOptionsMinimap option : relevantOptions) {
            if (option.isFloat()) {
                float sValue = this.options.getFloatValue(option);

                this.addRenderableWidget(new GuiOptionSliderMinimap(this.getWidth() / 2 - 155 + var2 % 2 * 160, this.getHeight() / 6 + 24 * (var2 >> 1), option, this.convertFloatValue(option, sValue), this.options));
            } else {
                GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + var2 % 2 * 160, this.getHeight() / 6 + 24 * (var2 >> 1), option, Component.literal(this.options.getKeyText(option)), this::optionClicked);
                this.addRenderableWidget(optionButton);
            }

            ++var2;
        }

        iterateButtonOptions();

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)).bounds(this.getWidth() / 2 - 100, this.getHeight() / 6 + 168, 200, 20).build());
    }

    protected void optionClicked(Button par1GuiButton) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) par1GuiButton).returnEnumOptions();
        this.options.setValue(option);
        par1GuiButton.setMessage(Component.literal(this.options.getKeyText(option)));

        iterateButtonOptions();
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        for (Object buttonObj : this.getButtonList()) {
            if (buttonObj instanceof GuiOptionSliderMinimap slider) {
                EnumOptionsMinimap option = slider.returnEnumOptions();
                float fValue = this.convertFloatValue(option, this.options.getFloatValue(option));

                if (this.getFocused() != slider) {
                    slider.setValue(fValue);
                }
            }
        }

        this.renderDefaultBackground(drawContext);
        drawContext.flush();
        drawContext.drawCenteredString(this.font, this.screenTitle, this.getWidth() / 2, 20, 16777215);

        super.render(drawContext, mouseX, mouseY, delta);
    }

    private void iterateButtonOptions() {
        for (Object buttonObj : this.getButtonList()) {
            if (!(buttonObj instanceof GuiOptionButtonMinimap button)) continue;

            EnumOptionsMinimap option = button.returnEnumOptions();

            if (option == EnumOptionsMinimap.SHOW_DISTANCE_LABEL) {
                button.active = this.options.showWaypointName != 0;
            }
        }
    }

    private float convertFloatValue(EnumOptionsMinimap option, float sValue) {
        switch (option) {
            case WAYPOINT_DISTANCE -> {
                if (sValue < 0.0F) {
                    sValue = 10001.0F;
                }

                sValue = (sValue - 50.0F) / 9951.0F;

                return sValue;
            }
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        }
    }
}
