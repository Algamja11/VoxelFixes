package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionSliderMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.util.GuiUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;

public class GuiWaypointsOptions extends GuiScreenMinimap {
    private static final EnumOptionsMinimap[] relevantOptions = { EnumOptionsMinimap.WAYPOINT_DISTANCE, EnumOptionsMinimap.WAYPOINT_ICON_SIZE, EnumOptionsMinimap.WAYPOINT_FONT_SIZE, EnumOptionsMinimap.SHOW_WAYPOINT_NAMES_ON_MAP, EnumOptionsMinimap.DEATHPOINTS, EnumOptionsMinimap.AUTO_UNIT_CONVERSION, EnumOptionsMinimap.SHOW_WAYPOINT_NAMES, EnumOptionsMinimap.SHOW_WAYPOINT_DISTANCES };
    private final MapSettingsManager options;
    protected Component screenTitle;
    private String tooltip;
    private final String tooltipDeathpoints = I18n.get("options.voxelmap.waypoints.deathpoints.tooltip");

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
        super.render(drawContext, mouseX, mouseY, delta);
        drawContext.drawCenteredString(this.getFont(), this.screenTitle, this.getWidth() / 2, 20, 16777215);

        this.tooltip = null;

        for (Object element : this.getButtonList()) {
            if (element instanceof GuiOptionButtonMinimap button) {
                EnumOptionsMinimap option = button.returnEnumOptions();

                if (button.isHovered()) {
                    if (option == EnumOptionsMinimap.DEATHPOINTS) {
                        this.tooltip = this.tooltipDeathpoints;
                    }
                }
            }

            if (element instanceof GuiOptionSliderMinimap slider) {
                EnumOptionsMinimap option = slider.returnEnumOptions();
                float fValue = this.convertFloatValue(option, this.options.getFloatValue(option));

                if (this.getFocused() != slider) {
                    slider.setValue(fValue);
                }
            }
        }

        if (this.tooltip != null) {
            GuiUtils.renderTooltip(drawContext, this.tooltip, mouseX, mouseY);
        }
    }

    private void iterateButtonOptions() {
        for (GuiEventListener element : this.getButtonList()) {
            if (!(element instanceof GuiOptionButtonMinimap button)) continue;

            EnumOptionsMinimap option = button.returnEnumOptions();

            if (option == EnumOptionsMinimap.SHOW_WAYPOINT_DISTANCES) {
                button.setMessage(Component.literal(this.options.getKeyText(option)));
            }
        }

        for (GuiEventListener element : this.getButtonList()) {
            if (!(element instanceof GuiOptionSliderMinimap slider)) continue;

            EnumOptionsMinimap option = slider.returnEnumOptions();

            if (option == EnumOptionsMinimap.WAYPOINT_FONT_SIZE) {
                slider.active = this.options.showWaypointNamesOnMap;
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
            case WAYPOINT_ICON_SIZE -> {
                return (sValue - 0.5F) / 1.5F;
            }
            case WAYPOINT_FONT_SIZE -> {
                return (sValue - 0.75F) / 1.25F;
            }
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        }
    }
}
