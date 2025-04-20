package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiButtonText;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiOptionButtonMinimap;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.persistent.GuiPersistentMapOptions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class GuiMinimapOptions extends GuiScreenMinimap {
    private static final EnumOptionsMinimap[] relevantOptions = {
            EnumOptionsMinimap.HIDE_MINIMAP, EnumOptionsMinimap.SHOW_COORDINATES,
            EnumOptionsMinimap.OLD_NORTH, EnumOptionsMinimap.SHOW_BIOME_LABEL,
            EnumOptionsMinimap.SIZE, EnumOptionsMinimap.SQUAREMAP,
            EnumOptionsMinimap.ROTATES, EnumOptionsMinimap.LOCATION,
            EnumOptionsMinimap.CAVE_MODE, EnumOptionsMinimap.INGAME_WAYPOINTS,
            EnumOptionsMinimap.MOVE_SCOREBOARD_BELOW_MAP, EnumOptionsMinimap.MOVE_MAP_BELOW_STATUS_EFFECT,
            EnumOptionsMinimap.DYNAMIC_LIGHTING, EnumOptionsMinimap.TERRAIN_DEPTH,
            EnumOptionsMinimap.WATER_TRANSPARENCY, EnumOptionsMinimap.BLOCK_TRANSPARENCY,
            EnumOptionsMinimap.BIOME_TINT, EnumOptionsMinimap.FILTERING,
            EnumOptionsMinimap.CHUNK_GRID, EnumOptionsMinimap.BIOME_OVERLAY,
            EnumOptionsMinimap.SLIME_CHUNKS, EnumOptionsMinimap.WORLD_BORDER,
            EnumOptionsMinimap.WORLD_SEED, EnumOptionsMinimap.TELEPORT_COMMAND
    };
    private GuiButtonText worldSeedButton;
    private GuiButtonText teleportCommandButton;
    private GuiOptionButtonMinimap slimeChunksButton;
    private int page = 0;
    private final MapSettingsManager options;
    protected String screenTitle = "Minimap Options";

    public GuiMinimapOptions(Screen parent) {
        this.parentScreen = parent;
        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
    }

    public void init() {
        clearWidgets();
        getButtonList().clear();
        children().clear();

        int buttonCount = 10;
        int buttonStart = page * buttonCount;
        int buttonEnd = buttonStart + buttonCount;
        if (buttonEnd > relevantOptions.length) {
            buttonEnd = relevantOptions.length;
        }
        int maxPages = (int) Math.ceil((float) relevantOptions.length / buttonCount);
        this.screenTitle = I18n.get("options.voxelmap.title") + " [" + (page + 1) + "/" + maxPages + "]";

        for (int i = buttonStart; i < buttonEnd; ++i) {
            int buttonPos = i - buttonStart;
            EnumOptionsMinimap option = relevantOptions[i];

            if (option == EnumOptionsMinimap.WORLD_SEED) {
                String worldSeedDisplay = VoxelConstants.getVoxelMapInstance().getWorldSeed();
                if (worldSeedDisplay.isEmpty()) {
                    worldSeedDisplay = I18n.get("selectWorld.versionUnknown");
                }
                String buttonSeedText = I18n.get("options.voxelmap.worldseed") + ": " + worldSeedDisplay;
                this.worldSeedButton = new GuiButtonText(this.getFontRenderer(), getWidth() / 2 - 155 + buttonPos % 2 * 160, getHeight() / 6 + 24 * (buttonPos >> 1), 150, 20, Component.literal(buttonSeedText), button -> this.worldSeedButton.setEditing(true));
                this.worldSeedButton.setText(VoxelConstants.getVoxelMapInstance().getWorldSeed());
                this.worldSeedButton.active = !VoxelConstants.getMinecraft().hasSingleplayerServer();
                this.addRenderableWidget(this.worldSeedButton);
            } else if (option == EnumOptionsMinimap.TELEPORT_COMMAND) {
                String buttonTeleportText = I18n.get("options.voxelmap.teleportcommand") + ": " + VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand;
                this.teleportCommandButton = new GuiButtonText(this.getFontRenderer(), getWidth() / 2 - 155 + buttonPos % 2 * 160, getHeight() / 6 + 24 * (buttonPos >> 1), 150, 20, Component.literal(buttonTeleportText), button -> this.teleportCommandButton.setEditing(true));
                this.teleportCommandButton.setText(VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand);
                this.teleportCommandButton.active = VoxelConstants.getVoxelMapInstance().getMapOptions().serverTeleportCommand == null;
                this.addRenderableWidget(this.teleportCommandButton);
            } else {
                StringBuilder text = new StringBuilder().append(this.options.getKeyText(option));
                if ((option == EnumOptionsMinimap.WATER_TRANSPARENCY || option == EnumOptionsMinimap.BLOCK_TRANSPARENCY || option == EnumOptionsMinimap.BIOME_TINT) && !this.options.multicore && this.options.getBooleanValue(option)) {
                    text.append("§c").append(text);
                }

                GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(getWidth() / 2 - 155 + buttonPos % 2 * 160, getHeight() / 6 + 24 * (buttonPos >> 1), option, Component.literal(text.toString()), this::optionClicked);
                this.addRenderableWidget(optionButton);

                if (option == EnumOptionsMinimap.SLIME_CHUNKS) {
                    this.slimeChunksButton = optionButton;
                    this.slimeChunksButton.active = VoxelConstants.getMinecraft().hasSingleplayerServer() || !VoxelConstants.getVoxelMapInstance().getWorldSeed().isEmpty();
                } else if (option == EnumOptionsMinimap.HIDE_MINIMAP) {
                    optionButton.active = this.options.minimapAllowed;
                } else if (option == EnumOptionsMinimap.INGAME_WAYPOINTS) {
                    optionButton.active = this.options.waypointsAllowed;
                } else if (option == EnumOptionsMinimap.CAVE_MODE) {
                    optionButton.active = this.options.cavesAllowed;
                }
            }
        }

        Button radarOptionsButton = new Button.Builder(Component.translatable("options.voxelmap.radar"), button -> VoxelConstants.getMinecraft().setScreen(new GuiRadarOptions(this))).bounds(this.getWidth() / 2 - 155, this.getHeight() / 6 + 120, 150, 20).build();
        radarOptionsButton.active = VoxelConstants.getVoxelMapInstance().getRadarOptions().radarAllowed || VoxelConstants.getVoxelMapInstance().getRadarOptions().radarMobsAllowed || VoxelConstants.getVoxelMapInstance().getRadarOptions().radarPlayersAllowed;
        this.addRenderableWidget(radarOptionsButton);
        Button worldMapButton = new Button.Builder(Component.translatable("options.voxelmap.worldmap"), button -> VoxelConstants.getMinecraft().setScreen(new GuiPersistentMapOptions(this))).bounds(this.getWidth() / 2 + 5, this.getHeight() / 6 + 120, 150, 20).build();
        worldMapButton.active = VoxelMap.mapOptions.worldmapAllowed;
        this.addRenderableWidget(worldMapButton);
        this.addRenderableWidget(new Button.Builder(Component.translatable("options.controls"), button -> VoxelConstants.getMinecraft().setScreen(new GuiMinimapControls(this))).bounds(this.getWidth() / 2 - 75, this.getHeight() / 6 + 144, 150, 20).build());

        this.addRenderableWidget(new Button.Builder(Component.translatable("<"), button -> {
            if (--page < 0) page = maxPages - 1;
            init();
        }).bounds(this.getWidth() / 2 - 160, this.getHeight() / 6 + 144, 60, 20).build());
        this.addRenderableWidget(new Button.Builder(Component.translatable(">"), button -> {
            if (++page >= maxPages) page = 0;
            init();
        }).bounds(this.getWidth() / 2 + 100, this.getHeight() / 6 + 144, 60, 20).build());

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parentScreen)).bounds(this.getWidth() / 2 - 100, this.getHeight() / 6 + 168, 200, 20).build());
    }

    protected void optionClicked(Button par1GuiButton) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) par1GuiButton).returnEnumOptions();
        this.options.setValue(option);
        String warningTint = "";
        if ((option == EnumOptionsMinimap.WATER_TRANSPARENCY || option == EnumOptionsMinimap.BLOCK_TRANSPARENCY || option == EnumOptionsMinimap.BIOME_TINT) && !this.options.multicore && this.options.getBooleanValue(option)) {
            warningTint = "§c";
        }

        par1GuiButton.setMessage(Component.literal(warningTint + this.options.getKeyText(option)));
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            this.worldSeedButton.keyPressed(keyCode, scanCode, modifiers);
            this.teleportCommandButton.keyPressed(keyCode, scanCode, modifiers);
        }

        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            if (this.worldSeedButton.isEditing()) {
                this.newSeed();
            } else if (this.teleportCommandButton.isEditing()) {
                this.newTeleportCommand();
            }

        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        if (chr == '\r') {
            if (this.worldSeedButton.isEditing()) {
                this.newSeed();
            } else if (this.teleportCommandButton.isEditing()) {
                this.newTeleportCommand();
            }

        }

        return super.charTyped(chr, modifiers);
    }

    private void newSeed() {
        String newSeed = this.worldSeedButton.getText();
        VoxelConstants.getVoxelMapInstance().setWorldSeed(newSeed);
        String worldSeedDisplay = VoxelConstants.getVoxelMapInstance().getWorldSeed();
        if (worldSeedDisplay.isEmpty()) {
            worldSeedDisplay = I18n.get("selectWorld.versionUnknown");
        }

        String buttonText = I18n.get("options.voxelmap.worldseed") + ": " + worldSeedDisplay;
        this.worldSeedButton.setMessage(Component.literal(buttonText));
        this.worldSeedButton.setText(VoxelConstants.getVoxelMapInstance().getWorldSeed());
        VoxelConstants.getVoxelMapInstance().getMap().forceFullRender(true);
        this.slimeChunksButton.active = VoxelConstants.getMinecraft().hasSingleplayerServer() || !VoxelConstants.getVoxelMapInstance().getWorldSeed().isEmpty();
    }

    private void newTeleportCommand() {
        String newTeleportCommand = this.teleportCommandButton.getText().isEmpty() ? "tp %p %x %y %z" : this.teleportCommandButton.getText();
        VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand = newTeleportCommand;

        String buttonText = I18n.get("options.voxelmap.teleportcommand") + ": " + newTeleportCommand;
        this.teleportCommandButton.setMessage(Component.literal(buttonText));
        this.teleportCommandButton.setText(VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand);
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.renderDefaultBackground(drawContext);
        drawContext.drawCenteredString(this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        super.render(drawContext, mouseX, mouseY, delta);
    }
}
