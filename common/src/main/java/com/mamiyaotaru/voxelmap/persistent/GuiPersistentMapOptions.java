package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionSliderMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiPersistentMapOptions extends GuiScreenMinimap {
    private final PersistentMapSettingsManager options;
    private final Component screenTitle = Component.translatable("options.voxelmap.worldmap.title");
    private final Component cacheSettings = Component.translatable("options.voxelmap.worldmap.cachesettings");
    private final Component warning = Component.translatable("options.voxelmap.worldmap.warning");

    public GuiPersistentMapOptions(Screen parent) {
        this.parentScreen = parent;
        this.options = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions();
    }

    public void init() {
        EnumOptionsMinimap[] relevantOptions = { EnumOptionsMinimap.SHOW_WAYPOINTS, EnumOptionsMinimap.SHOW_WAYPOINT_NAMES};

        int counter = 0;

        for (EnumOptionsMinimap option : relevantOptions) {
            GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + counter % 2 * 160, this.getHeight() / 6 + 24 * (counter >> 1), option, Component.literal(this.options.getKeyText(option)), this::optionClicked);
            this.addRenderableWidget(optionButton);
            
            if (option == EnumOptionsMinimap.SHOW_WAYPOINTS) optionButton.active = VoxelMap.mapOptions.waypointsAllowed;
            if (option == EnumOptionsMinimap.SHOW_WAYPOINT_NAMES) optionButton.active = VoxelMap.mapOptions.waypointsAllowed;
            counter++;
        }

        EnumOptionsMinimap[] relevantOptions2 = { EnumOptionsMinimap.MIN_ZOOM, EnumOptionsMinimap.MAX_ZOOM, EnumOptionsMinimap.CACHE_SIZE};
        counter += 2;

        for (EnumOptionsMinimap option : relevantOptions2) {
            if (option.isFloat()) {
                float sValue = this.options.getFloatValue(option);

                this.addRenderableWidget(new GuiOptionSliderMinimap(this.getWidth() / 2 - 155 + counter % 2 * 160, this.getHeight() / 6 + 24 * (counter >> 1), option, this.convertFloatValue(option, sValue), this.options));
            } else {
                this.addRenderableWidget(new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + counter % 2 * 160, this.getHeight() / 6 + 24 * (counter >> 1), option, Component.literal(this.options.getKeyText(option)), this::optionClicked));
            }

            counter++;
        }

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), buttonx -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)).bounds(this.getWidth() / 2 - 100, this.getHeight() / 6 + 168, 200, 20).build());

        for (Object buttonObj : this.getButtonList()) {
            if (buttonObj instanceof GuiOptionButtonMinimap button) {
                if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_WAYPOINT_NAMES) {
                    button.active = this.options.showWaypoints && VoxelMap.mapOptions.waypointsAllowed;
                }
            }
        }

    }

    protected void optionClicked(Button par1GuiButton) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) par1GuiButton).returnEnumOptions();
        this.options.setValue(option);
        par1GuiButton.setMessage(Component.literal(this.options.getKeyText(option)));

        for (Object buttonObj : this.getButtonList()) {
            if (buttonObj instanceof GuiOptionButtonMinimap button) {
                if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_WAYPOINT_NAMES) {
                    button.active = this.options.showWaypoints && VoxelMap.mapOptions.waypointsAllowed;
                }
            }
        }

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
        drawContext.drawCenteredString(this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        drawContext.drawCenteredString(this.getFontRenderer(), this.cacheSettings, this.getWidth() / 2, this.getHeight() / 6 + 24, 16777215);
        drawContext.drawCenteredString(this.getFontRenderer(), this.warning, this.getWidth() / 2, this.getHeight() / 6 + 34, 16777215);
        super.render(drawContext, mouseX, mouseY, delta);
    }

    private float convertFloatValue(EnumOptionsMinimap option, float sValue) {
        return switch (option) {
            case MIN_ZOOM, MAX_ZOOM -> (sValue + 3.0F) / (5 + 3);
            case CACHE_SIZE -> sValue / 5000.0F;
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        };
    }
}