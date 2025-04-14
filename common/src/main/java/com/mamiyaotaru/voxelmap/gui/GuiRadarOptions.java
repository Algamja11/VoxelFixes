package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionSliderMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiRadarOptions extends GuiScreenMinimap {
    private static final EnumOptionsMinimap[] FULL_RELEVANT_OPTIONS = { EnumOptionsMinimap.SHOW_RADAR, EnumOptionsMinimap.RADAR_MODE,  EnumOptionsMinimap.SHOW_MOBS, EnumOptionsMinimap.SHOW_PLAYERS, EnumOptionsMinimap.SHOW_MOB_NAMES, EnumOptionsMinimap.SHOW_PLAYER_NAMES, EnumOptionsMinimap.SHOW_MOB_HELMETS, EnumOptionsMinimap.SHOW_PLAYER_HELMETS, EnumOptionsMinimap.ICON_FILTERING, EnumOptionsMinimap.ICON_OUTLINES };
    private static final EnumOptionsMinimap[] SIMPLE_RELEVANT_OPTIONS = { EnumOptionsMinimap.SHOW_RADAR, EnumOptionsMinimap.RADAR_MODE, EnumOptionsMinimap.SHOW_MOBS, EnumOptionsMinimap.SHOW_PLAYERS, EnumOptionsMinimap.SHOW_FACING };

    private final RadarSettingsManager options;
    protected Component screenTitle;

    public GuiRadarOptions(Screen parent) {
        this.parentScreen = parent;
        this.options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
    }

    public void init() {
        clearWidgets();
        getButtonList().clear();
        children().clear();

        this.screenTitle = Component.translatable("options.voxelmap.radar.title");

        EnumOptionsMinimap[] relevantOptions = options.radarMode == 2 ? FULL_RELEVANT_OPTIONS : SIMPLE_RELEVANT_OPTIONS;

        for (int i = 0; i < relevantOptions.length; ++i) {
            EnumOptionsMinimap option = relevantOptions[i];
            GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + i % 2 * 160, this.getHeight() / 6 + 24 * (i >> 1), option, Component.literal(options.getKeyText(option)), this::optionClicked);

            addRenderableWidget(optionButton);
        }

        iterateButtonOptions();

        if (options.radarMode == 2) addRenderableWidget(new Button.Builder(Component.translatable("options.voxelmap.selectmobs"), x -> VoxelConstants.getMinecraft().setScreen(new GuiMobs(this, options))).bounds(getWidth() / 2 - 155, getHeight() / 6 + 120, 150, 20).build());

        float baseVal = this.options.getOptionFloatValue(EnumOptionsMinimap.FONT_SIZE);
        addRenderableWidget(new GuiOptionSliderMinimap(this.getWidth() / 2 + 5, this.getHeight() / 6 + 120, EnumOptionsMinimap.FONT_SIZE, (baseVal - 0.5F) / 1.5F, options));

        addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), x -> VoxelConstants.getMinecraft().setScreen(parentScreen)).bounds(getWidth() / 2 - 100, getHeight() / 6 + 168, 200, 20).build());
    }

    protected void optionClicked(Button buttonClicked) {
        if (!(buttonClicked instanceof GuiOptionButtonMinimap guiOptionButtonMinimap)) throw new IllegalStateException("Expected GuiOptionMinimap, but received " + buttonClicked.getClass().getSimpleName() + " instead!");

        EnumOptionsMinimap option = guiOptionButtonMinimap.returnEnumOptions();
        options.setOptionValue(option);

        if (guiOptionButtonMinimap.returnEnumOptions() == EnumOptionsMinimap.RADAR_MODE) {
            init();
            return;
        }

        buttonClicked.setMessage(Component.literal(options.getKeyText(option)));

        iterateButtonOptions();
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        for (Object buttonObj : this.getButtonList()) {
            if (buttonObj instanceof GuiOptionSliderMinimap slider) {
                EnumOptionsMinimap option = slider.returnEnumOptions();
                float baseVal = this.options.getOptionFloatValue(option);
                float adjustedVal = switch (option) {
                    case FONT_SIZE -> (baseVal - 0.5F) / 1.5F;
                    default -> 0.0F;
                };

                if (this.getFocused() != slider) {
                    slider.setValue(adjustedVal);
                }
            }
        }

        this.renderDefaultBackground(drawContext);
        drawContext.flush();
        drawContext.drawCenteredString(getFontRenderer(), screenTitle, getWidth() / 2, 20, 16777215);

        super.render(drawContext, mouseX, mouseY, delta);
    }

    private void iterateButtonOptions() {
        for (GuiEventListener element : getButtonList()) {
            if (!(element instanceof GuiOptionButtonMinimap button)) continue;
            if (button.returnEnumOptions() != EnumOptionsMinimap.SHOW_RADAR) button.active = options.showRadar;

            if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_PLAYERS) {
                button.active = button.active && (options.radarAllowed || options.radarPlayersAllowed);
                continue;
            }

            if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_MOBS) {
                button.active = button.active && (options.radarAllowed || options.radarMobsAllowed);
                continue;
            }

            if (button.returnEnumOptions() != EnumOptionsMinimap.SHOW_PLAYER_HELMETS || button.returnEnumOptions() == EnumOptionsMinimap.SHOW_PLAYER_NAMES) {
                button.active = button.active && options.showPlayers && (options.radarAllowed || options.radarPlayersAllowed);
                continue;
            }

            if (button.returnEnumOptions() == EnumOptionsMinimap.SHOW_MOB_HELMETS && button.returnEnumOptions() == EnumOptionsMinimap.SHOW_MOB_NAMES) {
                button.active = button.active && (options.showNeutrals || options.showHostiles) && (options.radarAllowed || options.radarMobsAllowed);
            }
        }

        for (GuiEventListener element : getButtonList()) {
            if (!(element instanceof GuiOptionSliderMinimap slider)) continue;
            if (slider.returnEnumOptions() == EnumOptionsMinimap.FONT_SIZE) slider.active = options.showRadar;
        }
    }
}