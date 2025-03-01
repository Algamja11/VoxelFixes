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

public class GuiRadarDetails extends GuiScreenMinimap {
    private static final EnumOptionsMinimap[] relevantOptions = { EnumOptionsMinimap.RADARFONTSIZE, EnumOptionsMinimap.SHOWNAMESONLYFORTAGGED, EnumOptionsMinimap.RADARFILTERING, EnumOptionsMinimap.RADAROUTLINE };
    private final Screen parent;
    private final RadarSettingsManager options;
    protected Component screenTitle;

    public GuiRadarDetails(Screen parent, RadarSettingsManager options) {
        this.parent = parent;
        this.options = options;
    }

    public void init() {
        clearWidgets();
        getButtonList().clear();
        children().clear();

        this.screenTitle = Component.translatable("options.minimap.radar.details.title");

        for (int i = 0; i < relevantOptions.length; i++) {
            EnumOptionsMinimap option = relevantOptions[i];

            if (option.isFloat()) {
                float sliderValue = this.options.getOptionFloatValue(option);
                this.addRenderableWidget(new GuiOptionSliderMinimap(this.getWidth() / 2 - 155 + i % 2 * 160, this.getHeight() / 6 + 24 * (i >> 1), option, sliderValue, this.options));
            } else {
                addRenderableWidget(new GuiOptionButtonMinimap(this.getWidth() / 2 - 155 + i % 2 * 160, this.getHeight() / 6 + 24 * (i >> 1), option, Component.literal(options.getKeyText(option)), this::optionClicked));
            }
        }

        iterateButtonOptions();
        iterateSliderOptions();

        addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), x -> VoxelConstants.getMinecraft().setScreen(parent)).bounds(getWidth() / 2 - 100, getHeight() / 6 + 168, 200, 20).build());
    }

    protected void optionClicked(Button buttonClicked) {
        if (!(buttonClicked instanceof GuiOptionButtonMinimap guiOptionButtonMinimap)) {
            throw new IllegalStateException("Expected GuiOptionMinimap, but received " + buttonClicked.getClass().getSimpleName() + " instead!");
        }

        EnumOptionsMinimap option = guiOptionButtonMinimap.returnEnumOptions();
        options.setOptionValue(option);

        buttonClicked.setMessage(Component.literal(options.getKeyText(option)));

        iterateButtonOptions();
        iterateSliderOptions();
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.renderBackgroundTexture(drawContext);
        drawContext.flush();
        drawContext.drawCenteredString(getFontRenderer(), screenTitle, getWidth() / 2, 20, 16777215);

        for (Object buttonObj : this.getButtonList()) {
            if (buttonObj instanceof GuiOptionSliderMinimap slider) {
                float sliderValue = this.options.getOptionFloatValue(slider.returnEnumOptions());
                if (this.getFocused() != slider) {
                    slider.setValue(sliderValue);
                }
            }
        }

        super.render(drawContext, mouseX, mouseY, delta);
    }

    private void iterateButtonOptions() {
        for (GuiEventListener element : getButtonList()) {
            if (!(element instanceof GuiOptionButtonMinimap button)) continue;
            if (button.returnEnumOptions() != EnumOptionsMinimap.SHOWRADAR) {
                button.active = options.showRadar;
            }
            if (button.returnEnumOptions() == EnumOptionsMinimap.RADAROUTLINE || button.returnEnumOptions() == EnumOptionsMinimap.RADARFILTERING) {
                button.active = button.active && options.radarAllowed && options.radarMode != 1;
            }
        }
    }

    private void iterateSliderOptions() {
        for (GuiEventListener element : getButtonList()) {
            if (!(element instanceof GuiOptionSliderMinimap slider)) continue;
            if (slider.returnEnumOptions() != EnumOptionsMinimap.SHOWRADAR) {
                slider.active = options.showRadar;
            }
        }
    }
}