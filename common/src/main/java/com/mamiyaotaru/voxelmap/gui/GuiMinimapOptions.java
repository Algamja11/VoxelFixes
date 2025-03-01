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

public class GuiMinimapOptions extends GuiScreenMinimap {
    private final Screen parent;
    private final MapSettingsManager options;
    protected String screenTitle = "Minimap Options";
    private static final EnumOptionsMinimap[] relevantOptions = {
            EnumOptionsMinimap.DISPLAY,
            EnumOptionsMinimap.COORDS,
            EnumOptionsMinimap.OLDNORTH,
            EnumOptionsMinimap.SHOWBIOMELABEL,
            EnumOptionsMinimap.SIZE,
            EnumOptionsMinimap.SHAPE,
            EnumOptionsMinimap.ROTATES,
            EnumOptionsMinimap.LOCATION,
            EnumOptionsMinimap.CAVEMODE,
            EnumOptionsMinimap.INGAMEWAYPOINTS,
            EnumOptionsMinimap.LIGHTING,
            EnumOptionsMinimap.TERRAINDEPTH,
            EnumOptionsMinimap.WATERTRANSPARENCY,
            EnumOptionsMinimap.BLOCKTRANSPARENCY,
            EnumOptionsMinimap.BIOMETINT,
            EnumOptionsMinimap.FILTERING,
            EnumOptionsMinimap.CHUNKGRID,
            EnumOptionsMinimap.BIOMEOVERLAY,
            EnumOptionsMinimap.MOVESCOREBOARDDOWN,
            EnumOptionsMinimap.MOVEMAPDOWNWHILESTATSUEFFECT,
            EnumOptionsMinimap.SLIMECHUNKS,
            EnumOptionsMinimap.WORLDBORDER,
            EnumOptionsMinimap.WORLDSEED,
            EnumOptionsMinimap.TELEPORTCOMMAND
    };
    private int currentPage = 0;
    private GuiButtonText worldSeedButton;
    private GuiButtonText teleportCommandButton;
    private GuiOptionButtonMinimap slimeChunksButton;

    public GuiMinimapOptions(Screen parent) {
        this.parent = parent;
        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
    }

    public void init() {
        int buttonsPerPage = 10;
        int pageStart = this.currentPage * buttonsPerPage;
        int pageEnd = Math.min(pageStart + buttonsPerPage, relevantOptions.length);
        int lastPage = (int) Math.ceil((float) relevantOptions.length / buttonsPerPage);
        this.screenTitle = I18n.get("options.minimap.title") + " [" + (this.currentPage + 1) + "/" + lastPage + "]";
        for (int i = pageStart; i < pageEnd; i++) {
            int buttonPos = i - pageStart;
            EnumOptionsMinimap option = relevantOptions[i];

            if (option == EnumOptionsMinimap.WORLDSEED) {
                String worldSeedDisplay = VoxelConstants.getVoxelMapInstance().getWorldSeed();
                if (worldSeedDisplay.isEmpty()) {
                    worldSeedDisplay = I18n.get("selectWorld.versionUnknown");
                }
                String buttonSeedText = I18n.get("options.minimap.worldseed") + ": " + worldSeedDisplay;
                this.worldSeedButton = new GuiButtonText(this.getFontRenderer(), getWidth() / 2 - 155 + buttonPos % 2 * 160, getHeight() / 6 + 24 * (buttonPos >> 1), 150, 20, Component.literal(buttonSeedText), button -> this.worldSeedButton.setEditing(true));
                this.worldSeedButton.setText(VoxelConstants.getVoxelMapInstance().getWorldSeed());
                this.worldSeedButton.active = !VoxelConstants.getMinecraft().hasSingleplayerServer();
                this.addRenderableWidget(this.worldSeedButton);
            } else if (option == EnumOptionsMinimap.TELEPORTCOMMAND) {
                String buttonTeleportText = I18n.get("options.minimap.teleportcommand") + ": " + VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand;
                this.teleportCommandButton = new GuiButtonText(this.getFontRenderer(), getWidth() / 2 - 155 + buttonPos % 2 * 160, getHeight() / 6 + 24 * (buttonPos >> 1), 150, 20, Component.literal(buttonTeleportText), button -> this.teleportCommandButton.setEditing(true));
                this.teleportCommandButton.setText(VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand);
                this.teleportCommandButton.active = VoxelConstants.getVoxelMapInstance().getMapOptions().serverTeleportCommand == null;
                this.addRenderableWidget(this.teleportCommandButton);
            } else if(option == EnumOptionsMinimap.LOCATION) {
                GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(getWidth() / 2 - 155 + buttonPos % 2 * 160, getHeight() / 6 + 24 * (buttonPos >> 1), 130, 20, option, Component.literal(this.options.getKeyText(option)), this::optionClicked);
                GuiOptionButtonMinimap moveButton = new GuiOptionButtonMinimap(getWidth() / 2 - 155 + buttonPos % 2 * 290, getHeight() / 6 + 24 * (buttonPos >> 1), 20, 20, option, Component.literal("..."), button -> VoxelConstants.getMinecraft().setScreen(new GuiMoveMinimap(this)));
                this.addRenderableWidget(optionButton);
                this.addRenderableWidget(moveButton);
            } else {
                StringBuilder text = new StringBuilder().append(this.options.getKeyText(option));
                if ((option == EnumOptionsMinimap.WATERTRANSPARENCY || option == EnumOptionsMinimap.BLOCKTRANSPARENCY || option == EnumOptionsMinimap.BIOMETINT) && !this.options.multicore && this.options.getOptionBooleanValue(option)) {
                    text.append("§c").append(text);
                }

                GuiOptionButtonMinimap optionButton = new GuiOptionButtonMinimap(getWidth() / 2 - 155 + buttonPos % 2 * 160, getHeight() / 6 + 24 * (buttonPos >> 1), option, Component.literal(text.toString()), this::optionClicked);
                this.addRenderableWidget(optionButton);

                if (option == EnumOptionsMinimap.DISPLAY) optionButton.active = this.options.minimapAllowed;
                if (option == EnumOptionsMinimap.INGAMEWAYPOINTS) optionButton.active = this.options.waypointsAllowed;
                if (option == EnumOptionsMinimap.CAVEMODE) optionButton.active = this.options.cavesAllowed;
                if (option == EnumOptionsMinimap.SLIMECHUNKS) {
                    this.slimeChunksButton = optionButton;
                    this.slimeChunksButton.active = VoxelConstants.getMinecraft().hasSingleplayerServer() || !VoxelConstants.getVoxelMapInstance().getWorldSeed().isEmpty();
                }
            }
        }

        Button radarOptionsButton = new Button.Builder(Component.translatable("options.minimap.radar"), button -> VoxelConstants.getMinecraft().setScreen(new GuiRadarOptions(this))).bounds(this.getWidth() / 2 - 155, this.getHeight() / 6 + 135 - 6, 150, 20).build();
        radarOptionsButton.active = VoxelConstants.getVoxelMapInstance().getRadarOptions().radarAllowed || VoxelConstants.getVoxelMapInstance().getRadarOptions().radarMobsAllowed || VoxelConstants.getVoxelMapInstance().getRadarOptions().radarPlayersAllowed;
        this.addRenderableWidget(radarOptionsButton);
        Button worldMapButton = new Button.Builder(Component.translatable("options.minimap.worldmap"), button -> VoxelConstants.getMinecraft().setScreen(new GuiPersistentMapOptions(this))).bounds(this.getWidth() / 2 + 5, this.getHeight() / 6 + 135 - 6, 150, 20).build();
        worldMapButton.active = VoxelMap.mapOptions.worldmapAllowed;
        this.addRenderableWidget(worldMapButton);
        this.addRenderableWidget(new Button.Builder(Component.translatable("options.controls"), button -> VoxelConstants.getMinecraft().setScreen(new GuiMinimapControls(this))).bounds(this.getWidth() / 2 - 75, this.getHeight() / 6 + 159 - 6, 150, 20).build());
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> VoxelConstants.getMinecraft().setScreen(this.parent)).bounds(this.getWidth() / 2 - 100, this.getHeight() / 6 + 183, 200, 20).build());
        this.addRenderableWidget(new Button.Builder(Component.literal("<"), button -> {
            this.currentPage = Math.max(0, --this.currentPage);
            VoxelConstants.getMinecraft().setScreen(this);
        }).bounds(this.getWidth() / 2 - 160, this.getHeight() / 6 + 159 - 6, 50, 20).build());
        this.addRenderableWidget(new Button.Builder(Component.literal(">"), button -> {
            this.currentPage = Math.min(lastPage - 1, ++this.currentPage);
            VoxelConstants.getMinecraft().setScreen(this);
        }).bounds(this.getWidth() / 2 + 160 - 50, this.getHeight() / 6 + 159 - 6, 50, 20).build());
    }

    protected void optionClicked(Button par1GuiButton) {
        EnumOptionsMinimap option = ((GuiOptionButtonMinimap) par1GuiButton).returnEnumOptions();
        this.options.setOptionValue(option);
        par1GuiButton.setMessage(Component.literal(this.options.getKeyText(option)));
        if (option == EnumOptionsMinimap.OLDNORTH) {
            VoxelConstants.getVoxelMapInstance().getWaypointManager().setOldNorth(this.options.oldNorth);
        }

    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.renderBackgroundTexture(drawContext);
        drawContext.drawCenteredString(this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);
        super.render(drawContext, mouseX, mouseY, delta);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 258) {
            this.worldSeedButton.keyPressed(keyCode, scanCode, modifiers);
            this.teleportCommandButton.keyPressed(keyCode, scanCode, modifiers);
        }

        if ((keyCode == 257 || keyCode == 335)) {
            if (this.worldSeedButton.isEditing()) {
                this.newSeed();
            } else if (this.teleportCommandButton.isEditing()) {
                this.newTeleportCommand();
            }

        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        boolean OK = super.charTyped(chr, modifiers);
        if (chr == '\r') {
            if (this.worldSeedButton.isEditing()) {
                this.newSeed();
            } else if (this.teleportCommandButton.isEditing()) {
                this.newTeleportCommand();
            }

        }

        return OK;
    }

    private void newSeed() {
        String newSeed = this.worldSeedButton.getText();
        VoxelConstants.getVoxelMapInstance().setWorldSeed(newSeed);
        String worldSeedDisplay = VoxelConstants.getVoxelMapInstance().getWorldSeed();
        if (worldSeedDisplay.isEmpty()) {
            worldSeedDisplay = I18n.get("selectWorld.versionUnknown");
        }

        String buttonText = I18n.get("options.minimap.worldseed") + ": " + worldSeedDisplay;
        this.worldSeedButton.setMessage(Component.literal(buttonText));
        this.worldSeedButton.setText(VoxelConstants.getVoxelMapInstance().getWorldSeed());
        VoxelConstants.getVoxelMapInstance().getMap().forceFullRender(true);
        this.slimeChunksButton.active = VoxelConstants.getMinecraft().hasSingleplayerServer() || !VoxelConstants.getVoxelMapInstance().getWorldSeed().isEmpty();
    }

    private void newTeleportCommand() {
        String newTeleportCommand = this.teleportCommandButton.getText().isEmpty() ? "tp %p %x %y %z" : this.teleportCommandButton.getText();
        VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand = newTeleportCommand;

        String buttonText = I18n.get("options.minimap.teleportcommand") + ": " + newTeleportCommand;
        this.teleportCommandButton.setMessage(Component.literal(buttonText));
        this.teleportCommandButton.setText(VoxelConstants.getVoxelMapInstance().getMapOptions().teleportCommand);
    }
}
