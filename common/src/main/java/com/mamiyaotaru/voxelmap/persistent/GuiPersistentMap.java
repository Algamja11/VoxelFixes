package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.GuiAddWaypoint;
import com.mamiyaotaru.voxelmap.gui.GuiMinimapOptions;
import com.mamiyaotaru.voxelmap.gui.GuiSubworldsSelect;
import com.mamiyaotaru.voxelmap.gui.GuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.IGuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.overridden.Popup;
import com.mamiyaotaru.voxelmap.gui.overridden.PopupGuiButton;
import com.mamiyaotaru.voxelmap.gui.overridden.PopupGuiScreen;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.BackgroundImageInfo;
import com.mamiyaotaru.voxelmap.util.BiomeMapData;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.EasingUtils;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.MessageUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.border.WorldBorder;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

public class GuiPersistentMap extends PopupGuiScreen implements IGuiWaypoints {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final Random generator = new Random();
    private final PersistentMap persistentMap;
    private final WaypointManager waypointManager;
    private final MapSettingsManager mapOptions;
    private final PersistentMapSettingsManager options;

    protected String screenTitle = "World Map";
    protected String worldNameDisplay = "";
    protected int worldNameDisplayLength;
    private String subworldName = "";

    private boolean closed;
    private boolean oldNorth;
    private boolean lastStill;
    private float mapPixelsX;
    private float mapPixelsY;
    private int centerX;
    private int centerY;
    private float mapCenterX;
    private float mapCenterZ;
    private float deltaX;
    private float deltaY;
    private float deltaXonRelease;
    private float deltaYonRelease;
    private long timeAtLastTick;
    private CachedRegion[] regions = new CachedRegion[0];
    private final BiomeMapData biomeMapData = new BiomeMapData(760, 360);
    private Waypoint newWaypoint;
    private Waypoint selectedWaypoint;
    private static boolean gotSkin;
    private final Object closedLock = new Object();

    private boolean keyboardInput;

    protected int mouseX;
    protected int mouseY;
    private float lastMouseX;
    private float lastMouseY;
    private boolean mouseCursorShown = true;
    private boolean currentDragging;
    private boolean leftMouseButtonDown;
    public boolean addClicked;
    public boolean editClicked;
    public boolean deleteClicked;
    private long timeOfRelease;

    private float zoom;
    private float zoomStart;
    private float zoomGoal;
    private float zoomDirectX;
    private float zoomDirectY;
    private long timeOfZoom;

    private float scScale = 1.0F;
    private float guiToMap = 2.0F;
    private float mapToGui = 0.5F;
    private float mouseDirectToMap = 1.0F;
    private float guiToDirectMouse = 2.0F;
    private int top;
    private int bottom;
    private int sideMargin = 10;
    private PopupGuiButton buttonWaypoints;
    private PopupGuiButton buttonMultiworld;
    private MutableComponent multiworldButtonName;

    private final SidebarPanel sidebarPanel = new SidebarPanel();

    private final ResourceLocation voxelmapSkinLocation = ResourceLocation.fromNamespaceAndPath("voxelmap", "persistentmap/playerskin");
    private final ResourceLocation crosshairResource = ResourceLocation.parse("textures/gui/sprites/hud/crosshair.png");
    private final ResourceLocation separatorHeaderResource = ResourceLocation.parse("textures/gui/inworld_header_separator.png");
    private final ResourceLocation separatorFooterResource = ResourceLocation.parse("textures/gui/inworld_footer_separator.png");

    public GuiPersistentMap(Screen parent) {
        this.parentScreen = parent;
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.persistentMap = VoxelConstants.getVoxelMapInstance().getPersistentMap();
        this.options = VoxelConstants.getVoxelMapInstance().getPersistentMapOptions();
        this.zoom = this.options.zoom;
        this.zoomStart = this.options.zoom;
        this.zoomGoal = this.options.zoom;
        this.persistentMap.setLightMapArray(VoxelConstants.getVoxelMapInstance().getMap().getLightmapArray());
        if (!gotSkin) {
            this.getSkin();
        }
    }

    private void getSkin() {
        BufferedImage skinImage = ImageUtils.createBufferedImageFromResourceLocation(VoxelConstants.getPlayer().getSkin().texture());

        if (skinImage == null) {
            MessageUtils.printDebugWarn("Got no player skin!");
            return;
        }

        gotSkin = true;

        boolean showHat = VoxelConstants.getPlayer().isModelPartShown(PlayerModelPart.HAT);
        if (showHat) {
            skinImage = ImageUtils.addImages(ImageUtils.loadImage(skinImage, 8, 8, 8, 8), ImageUtils.loadImage(skinImage, 40, 8, 8, 8), 0.0F, 0.0F, 8, 8);
        } else {
            skinImage = ImageUtils.loadImage(skinImage, 8, 8, 8, 8);
        }

        float scale = skinImage.getWidth() / 8.0F;
        skinImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(skinImage, 2.0F / scale)), true, 1);

        DynamicTexture texture = new DynamicTexture(() -> "Voxelmap player", ImageUtils.nativeImageFromBufferedImage(skinImage));
        minecraft.getTextureManager().register(voxelmapSkinLocation, texture);
    }

    @Override
    public void init() {
        this.screenTitle = I18n.get("voxelmap.worldmap.title");
        this.oldNorth = mapOptions.oldNorth;
        if (minecraft.screen == this) {
            this.closed = false;
        }
        this.top = 32;
        this.bottom = this.getHeight() - 32;
        this.centerX = this.getWidth() / 2;
        this.centerY = (this.bottom - this.top) / 2;
        this.scScale = (float) minecraft.getWindow().getGuiScale();
        this.mapPixelsX = minecraft.getWindow().getWidth();
        this.mapPixelsY = (minecraft.getWindow().getHeight() - (int) (64.0F * this.scScale));
        this.lastStill = false;
        this.timeAtLastTick = System.currentTimeMillis();

        this.buildWorldName();
        this.centerAt(this.options.mapX, this.options.mapZ);

        this.sideMargin = 10;
        int buttonCount = 5;
        int buttonSeparation = 4;
        int buttonWidth = (this.width - this.sideMargin * 2 - buttonSeparation * (buttonCount - 1)) / buttonCount;
        this.buttonWaypoints = new PopupGuiButton(this.sideMargin, this.getHeight() - 28, buttonWidth, 20, Component.translatable("options.voxelmap.waypoints"), button -> minecraft.setScreen(new GuiWaypoints(this)), this);
        this.addRenderableWidget(this.buttonWaypoints);
        this.multiworldButtonName = Component.translatable(VoxelConstants.isRealmServer() ? "menu.online" : "options.voxelmap.worldmap.multiworld");
        if (!minecraft.hasSingleplayerServer() && !waypointManager.receivedAutoSubworldName()) {
            this.addRenderableWidget(this.buttonMultiworld = new PopupGuiButton(this.sideMargin + (buttonWidth + buttonSeparation), this.getHeight() - 28, buttonWidth, 20, this.multiworldButtonName, button -> minecraft.setScreen(new GuiSubworldsSelect(this)), this));
        }
      
        this.addRenderableWidget(new PopupGuiButton(this.sideMargin + 3 * (buttonWidth + buttonSeparation), this.getHeight() - 28, buttonWidth, 20, Component.translatable("menu.options"), button -> minecraft.setScreen(new GuiMinimapOptions(this)), this));
        this.addRenderableWidget(new PopupGuiButton(this.sideMargin + 4 * (buttonWidth + buttonSeparation), this.getHeight() - 28, buttonWidth, 20, Component.translatable("gui.done"), button -> minecraft.setScreen(parentScreen), this));
    }

    private void centerAt(float x, float z) {
        if (this.oldNorth) {
            this.mapCenterX = (-z);
            this.mapCenterZ = x;
        } else {
            this.mapCenterX = x;
            this.mapCenterZ = z;
        }

    }

    private void buildWorldName() {
        final AtomicReference<String> worldName = new AtomicReference<>();

        VoxelConstants.getIntegratedServer().ifPresentOrElse(integratedServer -> {
            worldName.set(integratedServer.getWorldData().getLevelName());

            if (worldName.get() == null || worldName.get().isBlank()) {
                worldName.set("Singleplayer World");
            }
        }, () -> {
            ServerData info = minecraft.getCurrentServer();

            if (info != null) {
                worldName.set(info.name);
            }
            if (worldName.get() == null || worldName.get().isBlank()) {
                worldName.set("Multiplayer Server");
            }
            if (VoxelConstants.isRealmServer()) {
                worldName.set("Realms");
            }
        });

        StringBuilder worldNameBuilder = (new StringBuilder("§r")).append(worldName.get());
        String subworldName = waypointManager.getCurrentSubworldDescriptor(true);
        this.subworldName = subworldName;
        if (waypointManager.isMultiworld()) {
            if ((subworldName == null || subworldName.isEmpty())) {
                subworldName = I18n.get("selectWorld.versionUnknown");
            }
            worldNameBuilder.append(" - ").append(subworldName);
        }

        this.worldNameDisplay = worldNameBuilder.toString();
        this.worldNameDisplayLength = this.getFontRenderer().width(this.worldNameDisplay);

    }

    private float bindZoom(float zoom) {
        zoom = Math.max(this.options.minZoom, zoom);
        return Math.min(this.options.maxZoom, zoom);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        this.switchToMouseInput();
        float mouseDirectX = (float) minecraft.mouseHandler.xpos();
        float mouseDirectY = (float) minecraft.mouseHandler.ypos();
        if (amount != 0.0) {
            if (amount > 0.0) {
                this.zoomGoal *= 1.26F;
            } else if (amount < 0.0) {
                this.zoomGoal /= 1.26F;
            }

            this.zoomStart = this.zoom;
            this.zoomGoal = this.bindZoom(this.zoomGoal);
            this.timeOfZoom = System.currentTimeMillis();
            this.zoomDirectX = mouseDirectX;
            this.zoomDirectY = mouseDirectY;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean sidebarPressed = this.sidebarPanel.checkPressed(true);

        if (button == 0 && !sidebarPressed) {
            currentDragging = true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean sidebarPressed = this.sidebarPanel.checkPressed(false);
        currentDragging = false;
        if (mouseY > this.top && mouseY < this.bottom && button == 1 && !sidebarPressed) {
            this.keyboardInput = false;
            int mouseDirectX = (int) minecraft.mouseHandler.xpos();
            int mouseDirectY = (int) minecraft.mouseHandler.ypos();
            if (VoxelMap.mapOptions.worldmapAllowed) {
                this.createPopup((int) mouseX, (int) mouseY, mouseDirectX, mouseDirectY);
            }
        }
      
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft.options.keyJump.matches(keyCode, scanCode) || minecraft.options.keyShift.matches(keyCode, scanCode)) {
            if (minecraft.options.keyJump.matches(keyCode, scanCode)) {
                this.zoomGoal /= 1.26F;
            }

            if (minecraft.options.keyShift.matches(keyCode, scanCode)) {
                this.zoomGoal *= 1.26F;
            }

            this.zoomStart = this.zoom;
            this.zoomGoal = this.bindZoom(this.zoomGoal);
            this.timeOfZoom = System.currentTimeMillis();
            this.zoomDirectX = (minecraft.getWindow().getWidth() / 2f);
            this.zoomDirectY = (minecraft.getWindow().getHeight() - minecraft.getWindow().getHeight() / 2f);
            this.switchToKeyboardInput();
        }

        this.clearPopups();

        if (mapOptions.keyBindMenu.matches(keyCode, scanCode)) {
            keyCode = GLFW.GLFW_KEY_ESCAPE;
            scanCode = GLFW.GLFW_KEY_UNKNOWN;
            modifiers = GLFW.GLFW_KEY_UNKNOWN;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void switchToMouseInput() {
        this.keyboardInput = false;
        if (!this.mouseCursorShown) {
            GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }

        this.mouseCursorShown = true;
    }

    private void switchToKeyboardInput() {
        this.keyboardInput = true;
        this.mouseCursorShown = false;
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, -200);
        this.buttonWaypoints.active = VoxelMap.mapOptions.waypointsAllowed;
        this.zoomGoal = this.bindZoom(this.zoomGoal);
        if (this.mouseX != mouseX || this.mouseY != mouseY) {
            this.switchToMouseInput();
        }

        this.mouseX = mouseX;
        this.mouseY = mouseY;
        float mouseDirectX = (float) minecraft.mouseHandler.xpos();
        float mouseDirectY = (float) minecraft.mouseHandler.ypos();
        if (this.zoom != this.zoomGoal) {
            float previousZoom = this.zoom;
            long timeSinceZoom = System.currentTimeMillis() - this.timeOfZoom;
            if (timeSinceZoom < 700.0F) {
                this.zoom = EasingUtils.easeOutExpo(this.zoomStart, this.zoomGoal, timeSinceZoom, 700.0F);
            } else {
                this.zoom = this.zoomGoal;
            }

            float scaledZoom = this.zoom;
            if (minecraft.getWindow().getWidth() > 1600) {
                scaledZoom = this.zoom * minecraft.getWindow().getWidth() / 1600.0F;
            }

            float zoomDelta = this.zoom / previousZoom;
            float zoomOffsetX = this.centerX * this.guiToDirectMouse - this.zoomDirectX;
            float zoomOffsetY = (this.top + this.centerY) * this.guiToDirectMouse - this.zoomDirectY;
            float zoomDeltaX = zoomOffsetX - zoomOffsetX * zoomDelta;
            float zoomDeltaY = zoomOffsetY - zoomOffsetY * zoomDelta;
            this.mapCenterX += zoomDeltaX / scaledZoom;
            this.mapCenterZ += zoomDeltaY / scaledZoom;
        }

        this.options.zoom = this.zoomGoal;
        float scaledZoom = this.zoom;
        if (minecraft.getWindow().getScreenWidth() > 1600) {
            scaledZoom = this.zoom * minecraft.getWindow().getScreenWidth() / 1600.0F;
        }

        this.guiToMap = this.scScale / scaledZoom;
        this.mapToGui = 1.0F / this.scScale * scaledZoom;
        this.mouseDirectToMap = 1.0F / scaledZoom;
        this.guiToDirectMouse = this.scScale;
        guiGraphics.fill(0, 0, this.width, this.height, 0xFF000000);
        if (currentDragging) {
            if (!this.leftMouseButtonDown && this.overPopup(mouseX, mouseY)) {
                this.deltaX = 0.0F;
                this.deltaY = 0.0F;
                this.lastMouseX = mouseDirectX;
                this.lastMouseY = mouseDirectY;
                this.leftMouseButtonDown = true;
            } else if (this.leftMouseButtonDown) {
                this.deltaX = (this.lastMouseX - mouseDirectX) * this.mouseDirectToMap;
                this.deltaY = (this.lastMouseY - mouseDirectY) * this.mouseDirectToMap;
                this.lastMouseX = mouseDirectX;
                this.lastMouseY = mouseDirectY;
                this.deltaXonRelease = this.deltaX;
                this.deltaYonRelease = this.deltaY;
                this.timeOfRelease = System.currentTimeMillis();
            }
        } else {
            long timeSinceRelease = System.currentTimeMillis() - this.timeOfRelease;
            if (timeSinceRelease < 700.0F) {
                this.deltaX = EasingUtils.easeOutExpo(this.deltaXonRelease, 0.0f, timeSinceRelease, 700.0F);
                this.deltaY = EasingUtils.easeOutExpo(this.deltaYonRelease, 0.0F, timeSinceRelease, 700.0F);
            } else {
                this.deltaX = 0.0F;
                this.deltaY = 0.0F;
                this.deltaXonRelease = 0.0F;
                this.deltaYonRelease = 0.0F;
            }

            this.leftMouseButtonDown = false;
        }

        long timeSinceLastTick = System.currentTimeMillis() - this.timeAtLastTick;
        this.timeAtLastTick = System.currentTimeMillis();

        int kbDelta = 5;
        if (minecraft.options.keySprint.isDown()) {
            kbDelta = 10;
        }

        if (minecraft.options.keyUp.isDown()) {
            this.deltaY -= kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
            this.switchToKeyboardInput();
        }

        if (minecraft.options.keyDown.isDown()) {
            this.deltaY += kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
            this.switchToKeyboardInput();
        }

        if (minecraft.options.keyLeft.isDown()) {
            this.deltaX -= kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
            this.switchToKeyboardInput();
        }

        if (minecraft.options.keyRight.isDown()) {
            this.deltaX += kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
            this.switchToKeyboardInput();
        }

        this.mapCenterX += this.deltaX;
        this.mapCenterZ += this.deltaY;
        if (this.oldNorth) {
            this.options.mapX = (int) this.mapCenterZ;
            this.options.mapZ = -((int) this.mapCenterX);
        } else {
            this.options.mapX = (int) this.mapCenterX;
            this.options.mapZ = (int) this.mapCenterZ;
        }

        this.centerX = this.getWidth() / 2;
        this.centerY = (this.bottom - this.top) / 2;
        int left;
        int right;
        int top;
        int bottom;
        if (this.oldNorth) {
            left = (int) Math.floor((this.mapCenterZ - this.centerY * this.guiToMap) / 256.0F);
            right = (int) Math.floor((this.mapCenterZ + this.centerY * this.guiToMap) / 256.0F);
            top = (int) Math.floor((-this.mapCenterX - this.centerX * this.guiToMap) / 256.0F);
            bottom = (int) Math.floor((-this.mapCenterX + this.centerX * this.guiToMap) / 256.0F);
        } else {
            left = (int) Math.floor((this.mapCenterX - this.centerX * this.guiToMap) / 256.0F);
            right = (int) Math.floor((this.mapCenterX + this.centerX * this.guiToMap) / 256.0F);
            top = (int) Math.floor((this.mapCenterZ - this.centerY * this.guiToMap) / 256.0F);
            bottom = (int) Math.floor((this.mapCenterZ + this.centerY * this.guiToMap) / 256.0F);
        }

        synchronized (this.closedLock) {
            if (this.closed) {
                return;
            }

            this.regions = this.persistentMap.getRegions(left - 1, right + 1, top - 1, bottom + 1);
        }

        BackgroundImageInfo backGroundImageInfo = this.waypointManager.getBackgroundImageInfo();
        if (backGroundImageInfo != null) {
            guiGraphics.blitSprite(RenderType::guiTextured, backGroundImageInfo.getImageLocation(), backGroundImageInfo.left, backGroundImageInfo.top + 32, 0, 0, backGroundImageInfo.width, backGroundImageInfo.height, backGroundImageInfo.width, backGroundImageInfo.height);
        }

        guiGraphics.pose().translate(this.centerX - this.mapCenterX * this.mapToGui, (this.top + this.centerY) - this.mapCenterZ * this.mapToGui, 0.0f);
        if (this.oldNorth) {
            guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(90.0F));
        }

        float cursorCoordZ = 0.0f;
        float cursorCoordX = 0.0f;
        guiGraphics.pose().scale(this.mapToGui, this.mapToGui, 1);
        if (VoxelMap.mapOptions.worldmapAllowed) {
            for (CachedRegion region : this.regions) {
                ResourceLocation resource = region.getTextureLocation();
                if (resource != null) {
                    guiGraphics.blit(GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_FILTER_MIN, resource, region.getX() * 256, region.getZ() * 256, 0, 0, region.getWidth(), region.getWidth(), region.getWidth(), region.getWidth());
                }
            }

            if (VoxelMap.mapOptions.worldborder) {
                WorldBorder worldBorder = minecraft.level.getWorldBorder();
                float scale = 1.0f / (float) minecraft.getWindow().getGuiScale() / mapToGui;

                float x1 = (float) (worldBorder.getMinX());
                float z1 = (float) (worldBorder.getMinZ());
                float x2 = (float) (worldBorder.getMaxX());
                float z2 = (float) (worldBorder.getMaxZ());

                guiGraphics.drawSpecial(bufferSource -> {
                    Matrix4f matrix4f = guiGraphics.pose().last().pose();

                    RenderType renderType = RenderType.debugLineStrip(1.0);
                    VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

                    vertexConsumer.addVertex(matrix4f, x1, z1, 0).setColor(255, 0, 0, 255);
                    vertexConsumer.addVertex(matrix4f, x1, z2, 0).setColor(255, 0, 0, 255);
                    vertexConsumer.addVertex(matrix4f, x2, z2, 0).setColor(255, 0, 0, 255);
                    vertexConsumer.addVertex(matrix4f, x2, z1, 0).setColor(255, 0, 0, 255);
                    vertexConsumer.addVertex(matrix4f, x1, z1, 0).setColor(255, 0, 0, 255);

                    vertexConsumer.addVertex(matrix4f, x1 - scale, z1 - scale, 0).setColor(255, 0, 0, 255);
                    vertexConsumer.addVertex(matrix4f, x1 - scale, z2 + scale, 0).setColor(255, 0, 0, 255);
                    vertexConsumer.addVertex(matrix4f, x2 + scale, z2 + scale, 0).setColor(255, 0, 0, 255);
                    vertexConsumer.addVertex(matrix4f, x2 + scale, z1 - scale, 0).setColor(255, 0, 0, 255);
                    vertexConsumer.addVertex(matrix4f, x1 - scale, z1 - scale, 0).setColor(255, 0, 0, 255);

                    vertexConsumer.addVertex(matrix4f, x1 + scale, z1 + scale, 0).setColor(255, 0, 0, 255);
                    vertexConsumer.addVertex(matrix4f, x1 + scale, z2 - scale, 0).setColor(255, 0, 0, 255);
                    vertexConsumer.addVertex(matrix4f, x2 - scale, z2 - scale, 0).setColor(255, 0, 0, 255);
                    vertexConsumer.addVertex(matrix4f, x2 - scale, z1 + scale, 0).setColor(255, 0, 0, 255);
                    vertexConsumer.addVertex(matrix4f, x1 + scale, z1 + scale, 0).setColor(255, 0, 0, 255);
                });
            }

            float cursorX;
            float cursorY;
            if (this.mouseCursorShown) {
                cursorX = mouseDirectX;
                cursorY = mouseDirectY - this.top * this.guiToDirectMouse;
            } else {
                cursorX = (minecraft.getWindow().getWidth() / 2f);
                cursorY = (minecraft.getWindow().getHeight() - minecraft.getWindow().getHeight() / 2f) - this.top * this.guiToDirectMouse;
            }

            if (this.oldNorth) {
                cursorCoordX = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
                cursorCoordZ = -(cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap));
            } else {
                cursorCoordX = cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap);
                cursorCoordZ = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
            }

            if (VoxelMap.mapOptions.waypointsAllowed && this.options.showWaypoints) {
                TextureAtlas textureAtlas = this.waypointManager.getTextureAtlas();
                Waypoint highlightedPoint = this.waypointManager.getHighlightedWaypoint();

                for (Waypoint pt : this.waypointManager.getWaypoints()) {
                    this.drawWaypoint(guiGraphics, pt, false, cursorCoordZ, cursorCoordX, textureAtlas);
                }

                if (highlightedPoint != null) {
                    this.drawWaypoint(guiGraphics, highlightedPoint, true, cursorCoordZ, cursorCoordX, textureAtlas);
                }
            }

            if (gotSkin) {
                float playerX = (float) GameVariableAccessShim.xCoordDouble();
                float playerZ = (float) GameVariableAccessShim.zCoordDouble();
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(this.guiToMap, this.guiToMap, 1);
                if (this.oldNorth) {
                    guiGraphics.pose().translate(playerX * this.mapToGui, playerZ * this.mapToGui, 0.0f);
                    guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-90.0F));
                    guiGraphics.pose().translate(-(playerX * this.mapToGui), -(playerZ * this.mapToGui), 0.0f);
                }
                float x = -6.0F + playerX * this.mapToGui;
                float y = -6.0F + playerZ * this.mapToGui;
                float width = 12.0F;
                float height = 12.0F;
                guiGraphics.drawSpecial(bufferSource -> {
                    Matrix4f matrix4f = guiGraphics.pose().last().pose();

                    RenderType renderType = GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH.apply(voxelmapSkinLocation);
                    VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

                    vertexConsumer.addVertex(matrix4f, x + 0.0F, y + height, 0).setUv(0.0F, 1.0F).setColor(0xffffffff);
                    vertexConsumer.addVertex(matrix4f, x + width, y + height, 0).setUv(1.0F, 1.0F).setColor(0xffffffff);
                    vertexConsumer.addVertex(matrix4f, x + width, y + 0.0F, 0).setUv(1.0F, 0.0F).setColor(0xffffffff);
                    vertexConsumer.addVertex(matrix4f, x + 0.0F, y + 0.0F, 0).setUv(0.0F, 0.0F).setColor(0xffffffff);
                });

                guiGraphics.pose().popPose();
            }

            if (this.oldNorth) {
                guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }

            guiGraphics.pose().scale(this.guiToMap, this.guiToMap, 1);
            guiGraphics.pose().translate(-(this.centerX - this.mapCenterX * this.mapToGui), -((this.top + this.centerY) - this.mapCenterZ * this.mapToGui), 0.0f);
            if (mapOptions.biomeOverlay != 0) {
                float biomeScaleX = this.mapPixelsX / 760.0F;
                float biomeScaleY = this.mapPixelsY / 360.0F;
                boolean still = !this.leftMouseButtonDown;
                still = still && this.zoom == this.zoomGoal;
                still = still && this.deltaX == 0.0F && this.deltaY == 0.0F;
                still = still && ThreadManager.executorService.getActiveCount() == 0;
                if (still && !this.lastStill) {
                    int column;
                    if (this.oldNorth) {
                        column = (int) Math.floor(Math.floor(this.mapCenterZ - this.centerY * this.guiToMap) / 256.0) - (left - 1);
                    } else {
                        column = (int) Math.floor(Math.floor(this.mapCenterX - this.centerX * this.guiToMap) / 256.0) - (left - 1);
                    }

                    for (int x = 0; x < this.biomeMapData.getWidth(); ++x) {
                        for (int z = 0; z < this.biomeMapData.getHeight(); ++z) {
                            float floatMapX;
                            float floatMapZ;
                            if (this.oldNorth) {
                                floatMapX = z * biomeScaleY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
                                floatMapZ = -(x * biomeScaleX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap));
                            } else {
                                floatMapX = x * biomeScaleX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap);
                                floatMapZ = z * biomeScaleY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
                            }

                            int mapX = (int) Math.floor(floatMapX);
                            int mapZ = (int) Math.floor(floatMapZ);
                            int regionX = (int) Math.floor(mapX / 256.0F) - (left - 1);
                            int regionZ = (int) Math.floor(mapZ / 256.0F) - (top - 1);
                            if (!this.oldNorth && regionX != column || this.oldNorth && regionZ != column) {
                                this.persistentMap.compress();
                            }

                            column = !this.oldNorth ? regionX : regionZ;
                            CachedRegion region = this.regions[regionZ * (right + 1 - (left - 1) + 1) + regionX];
                            Biome biome = null;
                            if (region.getMapData() != null && region.isLoaded() && !region.isEmpty()) {
                                int inRegionX = mapX - region.getX() * region.getWidth();
                                int inRegionZ = mapZ - region.getZ() * region.getWidth();
                                int height = region.getMapData().getHeight(inRegionX, inRegionZ);
                                int light = region.getMapData().getLight(inRegionX, inRegionZ);
                                if (height != Short.MIN_VALUE || light != 0) {
                                    biome = region.getMapData().getBiome(inRegionX, inRegionZ);
                                }
                            }

                            this.biomeMapData.setBiome(x, z, biome);
                        }
                    }

                    this.persistentMap.compress();
                    this.biomeMapData.segmentBiomes();
                    this.biomeMapData.findCenterOfSegments(true);
                }

                this.lastStill = still;
                boolean displayStill = !this.leftMouseButtonDown;
                displayStill = displayStill && this.zoom == this.zoomGoal;
                displayStill = displayStill && this.deltaX == 0.0F && this.deltaY == 0.0F;
                if (displayStill) {
                    int minimumSize = (int) (20.0F * this.scScale / biomeScaleX);
                    minimumSize *= minimumSize;
                    ArrayList<AbstractMapData.BiomeLabel> labels = this.biomeMapData.getBiomeLabels();
                    for (AbstractMapData.BiomeLabel biomeLabel : labels) {
                        if (biomeLabel.segmentSize > minimumSize) {
                            String label = biomeLabel.name; // + " (" + biomeLabel.x + "," + biomeLabel.z + ")";
                            int nameWidth = this.chkLen(label);
                            float x = biomeLabel.x * biomeScaleX / this.scScale;
                            float z = biomeLabel.z * biomeScaleY / this.scScale;

                            this.write(guiGraphics, label, x - (nameWidth / 2f), this.top + z - 3.0F, 0xFFFFFF);
                        }
                    }
                }
            }
        } //TODO VoxelFixes: optimize biome overlay
        guiGraphics.pose().popPose();

        if (this.keyboardInput) {
            int scWidth = minecraft.getWindow().getGuiScaledWidth();
            int scHeight = minecraft.getWindow().getGuiScaledHeight();
            guiGraphics.blit(RenderType::crosshair, crosshairResource, scWidth / 2 - 8, scHeight / 2 - 8, 0.0f, 0.0f, 15, 15, 15, 15);
        } else {
            this.switchToMouseInput();
        }

        this.overlayBackground(guiGraphics);

        if (VoxelMap.mapOptions.worldmapAllowed) {
            guiGraphics.drawCenteredString(this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 16, 0xFFFFFF);
            int x = (int) Math.floor(cursorCoordX);
            int z = (int) Math.floor(cursorCoordZ);
            if (mapOptions.coordsMode != 0) {
                guiGraphics.drawString(this.getFontRenderer(), "X: " + x, this.sideMargin, 16, 0xFFFFFF);
                guiGraphics.drawString(this.getFontRenderer(), "Z: " + z, this.sideMargin + 64, 16, 0xFFFFFF);
            }

            String subworldDescriptor = waypointManager.getCurrentSubworldDescriptor(true);
            if (this.subworldName != null && !this.subworldName.equals(subworldDescriptor) || subworldDescriptor != null && !subworldDescriptor.equals(this.subworldName)) {
                this.buildWorldName();
            }

            if (this.buttonMultiworld != null) {
                if ((this.subworldName == null || this.subworldName.isEmpty()) && waypointManager.isMultiworld()) {
                    if ((int) (System.currentTimeMillis() / 1000L % 2L) == 0) {
                        this.buttonMultiworld.setMessage(this.multiworldButtonName.withStyle(ChatFormatting.RED));
                    } else {
                        this.buttonMultiworld.setMessage(this.multiworldButtonName);
                    }
                } else {
                    this.buttonMultiworld.setMessage(this.multiworldButtonName);
                }
            }

            guiGraphics.drawString(this.getFontRenderer(), this.worldNameDisplay, this.getWidth() - this.sideMargin - this.worldNameDisplayLength, 16, 0xFFFFFF);
        } else {
            guiGraphics.drawString(this.getFontRenderer(), Component.translatable("voxelmap.worldmap.disabled"), this.sideMargin, 16, 0xFFFFFF);
        }

        this.sidebarPanel.drawPanel(guiGraphics, mouseX, mouseY, delta, 200);

        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // nothing
    }

    private void drawWaypoint(GuiGraphics guiGraphics, Waypoint pt, boolean target, float cursorCoordZ, float cursorCoordX, TextureAtlas textureAtlas) {
        if (!pt.inWorld || !pt.inDimension) {
            return;
        }

        boolean showLabel = this.options.showWaypointNames;
        String name = pt.name;
        if (target) {
            if (pt.red == 2.0F && pt.green == 0.0F && pt.blue == 0.0F) {
                name = "X:" + pt.getX() + ", Y:" + pt.getY() + ", Z:" + pt.getZ();
            } else {
                showLabel = false;
            }
        }

        Sprite icon = null;
        if (target) {
            icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/target.png");
        }
        if (pt.isDeathpoint) {
            icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypointskull.png");
        }

        boolean uprightIcon = icon != null;

        float ptX = pt.getX() + 0.5F;
        float ptZ = pt.getZ() + 0.5F;

        boolean hover = cursorCoordX > ptX - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordX < ptX + 18.0F * this.guiToMap / this.guiToDirectMouse
                && cursorCoordZ > ptZ - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ < ptZ + 18.0F * this.guiToMap / this.guiToDirectMouse;

        int wayX = (int) ((ptX - this.mapCenterX) * this.mapToGui);
        int wayY = (int) ((ptZ - this.mapCenterZ) * this.mapToGui);
        float locate = (float) Math.toDegrees(Math.atan2(wayX, wayY));

        float rangeX = this.width / 2.0F - 4.0F;
        float rangeY = this.height / 2.0F - 32.0F - 4.0F;
        boolean far = Math.abs(wayX) > rangeX || Math.abs(wayY) > rangeY;
        if (far) {
            rangeX *= this.guiToMap;
            rangeY *= this.guiToMap;
            ptX = Math.max(this.mapCenterX - rangeX, Math.min(this.mapCenterX + rangeX, ptX));
            ptZ = Math.max(this.mapCenterZ - rangeY, Math.min(this.mapCenterZ + rangeY, ptZ));
        }


        if (far) {
            if (icon == null) {
                icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/marker.png");
            }
            int color = target ? 0xFFFF0000 : pt.getUnifiedColor(!pt.enabled ? 0.5F : 1.0F);

            try {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(this.guiToMap, this.guiToMap, 1.0F);
                if (!uprightIcon) {
                    guiGraphics.pose().translate(ptX * this.mapToGui, ptZ * this.mapToGui, 0.0F);
                    guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(180.0F - locate));
                    guiGraphics.pose().translate(-ptX * this.mapToGui, -ptZ * this.mapToGui, 0.0F);
                }
                icon.blit(guiGraphics, GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH, ptX * this.mapToGui - 8, ptZ * this.mapToGui - 8, 16, 16, color);
            } catch (Exception ignored) {
            } finally {
                guiGraphics.pose().popPose();
            }
        } else {
            if (icon == null) {
                icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + pt.imageSuffix + ".png");

                if (icon == textureAtlas.getMissingImage()) {
                    icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
                }
            }
            int color = target ? 0xFFFF0000 : pt.getUnifiedColor(!pt.enabled ? 0.5F : 1.0F);

            try {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().scale(this.guiToMap, this.guiToMap, 1.0F);
                icon.blit(guiGraphics, GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH, ptX * this.mapToGui - 8, ptZ * this.mapToGui - 8, 16, 16, color);
            } catch (Exception ignored) {
            } finally {
                guiGraphics.pose().popPose();
            }
        }

        if (showLabel && !far) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().scale(this.guiToMap, this.guiToMap, 1);
            int halfStringWidth = this.chkLen(name) / 2;
            this.write(guiGraphics, name, ptX * this.mapToGui - halfStringWidth, ptZ * this.mapToGui + 8, !pt.enabled && !target && !hover ? 0x55FFFFFF : 0xFFFFFFFF);
            guiGraphics.pose().popPose();
        }
    }

    private void overlayBackground(GuiGraphics guiGraphics) {
        guiGraphics.drawSpecial(bufferSource -> {
            Matrix4f matrix4f = guiGraphics.pose().last().pose();

            float renderedTextureSize = this.width / 32.0F;

            VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.gui());
            vertexConsumer.addVertex(matrix4f, 0, this.top, 0.0F).setColor(0x80000000);
            vertexConsumer.addVertex(matrix4f, this.width, this.top, 0.0F).setColor(0x80000000);
            vertexConsumer.addVertex(matrix4f, this.width, 0, 0.0F).setColor(0x80000000);
            vertexConsumer.addVertex(matrix4f, 0, 0, 0.0F).setColor(0x80000000);

            vertexConsumer.addVertex(matrix4f, 0, this.height, 0.0F).setColor(0x80000000);
            vertexConsumer.addVertex(matrix4f, this.width, this.height, 0.0F).setColor(0x80000000);
            vertexConsumer.addVertex(matrix4f, this.width, this.bottom, 0.0F).setColor(0x80000000);
            vertexConsumer.addVertex(matrix4f, 0, this.bottom, 0.0F).setColor(0x80000000);

            vertexConsumer = bufferSource.getBuffer(RenderType.guiTextured(separatorHeaderResource));
            vertexConsumer.addVertex(matrix4f, 0, this.top + 1, 0.0F).setUv(0.0F, 1.0F).setColor(0xFFFFFFFF);
            vertexConsumer.addVertex(matrix4f, this.width, this.top + 1, 0.0F).setUv(renderedTextureSize, 1.0F).setColor(0xFFFFFFFF);
            vertexConsumer.addVertex(matrix4f, this.width, this.top - 1, 0.0F).setUv(renderedTextureSize, 0.0F).setColor(0xFFFFFFFF);
            vertexConsumer.addVertex(matrix4f, 0, this.top - 1, 0.0F).setUv(0.0F, 0.0F).setColor(0xFFFFFFFF);

            vertexConsumer = bufferSource.getBuffer(RenderType.guiTextured(separatorFooterResource));
            vertexConsumer.addVertex(matrix4f, 0, this.bottom + 1, 0.0F).setUv(0.0F, 1.0F).setColor(0xFFFFFFFF);
            vertexConsumer.addVertex(matrix4f, this.width, this.bottom + 1, 0.0F).setUv(renderedTextureSize, 1.0F).setColor(0xFFFFFFFF);
            vertexConsumer.addVertex(matrix4f, this.width, this.bottom - 1, 0.0F).setUv(renderedTextureSize, 0.0F).setColor(0xFFFFFFFF);
            vertexConsumer.addVertex(matrix4f, 0, this.bottom - 1, 0.0F).setUv(0.0F, 0.0F).setColor(0xFFFFFFFF);
        });
    }

    @Override
    public void tick() {
    }

    @Override
    public void removed() {
        synchronized (this.closedLock) {
            this.closed = true;
            this.persistentMap.getRegions(0, -1, 0, -1);
            this.regions = new CachedRegion[0];
        }
    }

    private void createPopup(int x, int y, int directX, int directY) {
        ArrayList<Popup.PopupEntry> entries = new ArrayList<>();
        float cursorX = directX;
        float cursorY = directY - this.top * this.guiToDirectMouse;
        float cursorCoordX;
        float cursorCoordZ;
        if (this.oldNorth) {
            cursorCoordX = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
            cursorCoordZ = -(cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap));
        } else {
            cursorCoordX = cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap);
            cursorCoordZ = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
        }

        Waypoint hovered = this.getHovered(cursorCoordX, cursorCoordZ);
        Popup.PopupEntry entry;
        if (hovered != null && this.waypointManager.getWaypoints().contains(hovered)) {
            entry = new Popup.PopupEntry(I18n.get("selectServer.edit"), 4, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.get("selectServer.delete"), 5, true, true);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.get(hovered != this.waypointManager.getHighlightedWaypoint() ? "voxelmap.waypoints.highlight" : "voxelmap.waypoints.remove_highlight"), 1, true, true);
        } else {
            entry = new Popup.PopupEntry(I18n.get("voxelmap.waypoints.new_waypoint"), 0, true, VoxelMap.mapOptions.waypointsAllowed);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.get(hovered == null ? "voxelmap.waypoints.highlight" : "voxelmap.waypoints.remove_highlight"), 1, true, VoxelMap.mapOptions.waypointsAllowed);
        }
        entries.add(entry);
        entry = new Popup.PopupEntry(I18n.get("voxelmap.waypoints.teleport_to"), 3, true, true);
        entries.add(entry);
        entry = new Popup.PopupEntry(I18n.get("voxelmap.waypoint_share"), 2, true, true);
        entries.add(entry);

        this.createPopup(x, y, directX, directY, entries);
        if (VoxelConstants.DEBUG) {
            persistentMap.debugLog((int) cursorCoordX, (int) cursorCoordZ);
        }
    }

    private Waypoint getHovered(float cursorCoordX, float cursorCoordZ) {
        if (!VoxelMap.mapOptions.waypointsAllowed) {
            return null;
        }
        Waypoint waypoint = null;

        for (Waypoint pt : this.waypointManager.getWaypoints()) {
            float ptX = pt.getX() + 0.5F;
            float ptZ = pt.getZ() + 0.5F;
            boolean hover = pt.inDimension && pt.inWorld && cursorCoordX > ptX - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordX < ptX + 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ > ptZ - 18.0F * this.guiToMap / this.guiToDirectMouse
                    && cursorCoordZ < ptZ + 18.0F * this.guiToMap / this.guiToDirectMouse;
            if (hover) {
                waypoint = pt;
            }
        }

        if (waypoint == null) {
            Waypoint pt = this.waypointManager.getHighlightedWaypoint();
            if (pt != null) {
                float ptX = pt.getX() + 0.5F;
                float ptZ = pt.getZ() + 0.5F;
                boolean hover = pt.inDimension && pt.inWorld && cursorCoordX > ptX - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordX < ptX + 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ > ptZ - 18.0F * this.guiToMap / this.guiToDirectMouse
                        && cursorCoordZ < ptZ + 18.0F * this.guiToMap / this.guiToDirectMouse;
                if (hover) {
                    waypoint = pt;
                }
            }
        }

        return waypoint;
    }

    @Override
    public void popupAction(Popup popup, int action) {
        int mouseDirectX = popup.getClickedDirectX();
        int mouseDirectY = popup.getClickedDirectY();
        float cursorX = mouseDirectX;
        float cursorY = mouseDirectY - this.top * this.guiToDirectMouse;
        float cursorCoordX;
        float cursorCoordZ;
        if (this.oldNorth) {
            cursorCoordX = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
            cursorCoordZ = -(cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap));
        } else {
            cursorCoordX = cursorX * this.mouseDirectToMap + (this.mapCenterX - this.centerX * this.guiToMap);
            cursorCoordZ = cursorY * this.mouseDirectToMap + (this.mapCenterZ - this.centerY * this.guiToMap);
        }

        int x = (int) Math.floor(cursorCoordX);
        int z = (int) Math.floor(cursorCoordZ);
        int y = this.persistentMap.getHeightAt(x, z);
        Waypoint hovered = this.getHovered(cursorCoordX, cursorCoordZ);
        this.editClicked = false;
        this.addClicked = false;
        this.deleteClicked = false;
        double dimensionScale = VoxelConstants.getPlayer().level().dimensionType().coordinateScale();
        switch (action) {
            case 0 -> {
                if (hovered != null) {
                    x = hovered.getX();
                    z = hovered.getZ();
                }
                this.addClicked = true;
                float r;
                float g;
                float b;
                if (this.waypointManager.getWaypoints().isEmpty()) {
                    r = 0.0F;
                    g = 1.0F;
                    b = 0.0F;
                } else {
                    r = this.generator.nextFloat();
                    g = this.generator.nextFloat();
                    b = this.generator.nextFloat();
                }
                TreeSet<DimensionContainer> dimensions = new TreeSet<>();
                dimensions.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));
                y = y > VoxelConstants.getPlayer().level().getMinY() ? y : 64;
                this.newWaypoint = new Waypoint("", (int) (x * dimensionScale), (int) (z * dimensionScale), y, true, r, g, b, false, "", waypointManager.getCurrentSubworldDescriptor(false), dimensions);
                minecraft.setScreen(new GuiAddWaypoint(this, this.newWaypoint, false));
            }
            case 1 -> {
                if (hovered != null) {
                    this.waypointManager.setHighlightedWaypoint(hovered, true);
                } else {
                    y = y > VoxelConstants.getPlayer().level().getMinY() ? y : 64;
                    TreeSet<DimensionContainer> dimensions2 = new TreeSet<>();
                    dimensions2.add(VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level()));
                    Waypoint fakePoint = new Waypoint("", (int) (x * dimensionScale), (int) (z * dimensionScale), y,
                            true, 1.0F, 0.0F, 0.0F, false, "", waypointManager.getCurrentSubworldDescriptor(false), dimensions2);
                    this.waypointManager.setHighlightedWaypoint(fakePoint, true);
                }
            }
            case 2 -> {
                if (hovered != null) {
                    CommandUtils.sendWaypoint(hovered);
                } else {
                    y = y > VoxelConstants.getPlayer().level().getMinY() ? y : 64;
                    CommandUtils.sendCoordinate(x, y, z);
                }
            }
            case 3 -> {
                if (hovered == null) {
                    if (y < VoxelConstants.getPlayer().level().getMinY()) {
                        y = (!(VoxelConstants.getPlayer().level().dimensionType().hasCeiling()) ? VoxelConstants.getPlayer().level().getMaxY() : 64);
                    }
                    VoxelConstants.playerRunTeleportCommand(x, y, z);
                    break;
                }

                this.selectedWaypoint = hovered;
                y = selectedWaypoint.getY() > VoxelConstants.getPlayer().level().getMinY() ? selectedWaypoint.getY() : (!(VoxelConstants.getPlayer().level().dimensionType().hasCeiling()) ? VoxelConstants.getPlayer().level().getMaxY() : 64);
                VoxelConstants.playerRunTeleportCommand(selectedWaypoint.getX(), y, selectedWaypoint.getZ());
            }
            case 4 -> {
                if (hovered != null) {
                    this.editClicked = true;
                    this.selectedWaypoint = hovered;
                    minecraft.setScreen(new GuiAddWaypoint(this, hovered, true));
                }
            }
            case 5 -> {
                if (hovered != null) {
                    this.deleteClicked = true;
                    this.selectedWaypoint = hovered;
                    Component title = Component.translatable("voxelmap.waypoints.delete_confirm");
                    Component explanation = Component.translatable("selectServer.deleteWarning", this.selectedWaypoint.name);
                    Component affirm = Component.translatable("selectServer.deleteButton");
                    Component deny = Component.translatable("gui.cancel");
                    ConfirmScreen confirmScreen = new ConfirmScreen(this, title, explanation, affirm, deny);
                    minecraft.setScreen(confirmScreen);
                }
            }
            default -> VoxelConstants.getLogger().warn("unimplemented command");
        }

    }

    @Override
    public boolean isEditing() {
        return this.editClicked;
    }

    @Override
    public void accept(boolean b) {
        if (this.deleteClicked) {
            this.deleteClicked = false;
            if (b) {
                this.waypointManager.deleteWaypoint(this.selectedWaypoint);
                this.selectedWaypoint = null;
            }
        }

        if (this.editClicked) {
            this.editClicked = false;
            if (b) {
                this.waypointManager.saveWaypoints();
            }
        }

        if (this.addClicked) {
            this.addClicked = false;
            if (b) {
                this.waypointManager.addWaypoint(this.newWaypoint);
            }
        }

        minecraft.setScreen(this);
    }

    private int chkLen(String string) {
        return this.getFontRenderer().width(string);
    }

    private void write(GuiGraphics drawContext, String string, float x, float y, int color) {
        drawContext.drawString(this.font, string, (int) x, (int) y, color);
    }

    private class SidebarPanel {
        private enum ElementType {
            SIDEBAR_PANEL,
            BUTTON_WAYPOINT_LIST,
            WAYPOINT_LIST_LEFT_ARROW,
            WAYPOINT_LIST_RIGHT_ARROW
        }

        private final GuiPersistentMap parentGui;

        private int sidebarLeft;
        private int sidebarRight;
        private int sidebarMiddle;
        private boolean sidebarClicked;

        private boolean waypointListOpen;
        private int waypointListPage;
        private final List<Waypoint> filteredWaypoints = new ArrayList<>();

        private float moveStartX;
        private float moveStartZ;
        private float moveGoalX;
        private float moveGoalZ;
        private long timeOfMoveStart = -1L;

        private final EnumMap<ElementType, Boolean> buttonStates = new EnumMap<>(ElementType.class);
        private final ElementType[] sidebarButtons = { ElementType.BUTTON_WAYPOINT_LIST };

        private final ResourceLocation arrowIcon = ResourceLocation.fromNamespaceAndPath("voxelmap", "images/sidebar/arrow.png");
        private final ResourceLocation waypointListIcon = ResourceLocation.fromNamespaceAndPath("voxelmap", "images/sidebar/waypoint_list.png");

        private SidebarPanel() {
            parentGui = GuiPersistentMap.this;
        }

        private void drawPanel(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, int panelWidth) {
            sidebarRight = parentGui.width - 40;
            sidebarLeft = sidebarRight - panelWidth;
            sidebarMiddle = (sidebarLeft + sidebarRight) / 2;

            boolean sidebarOpen = waypointListOpen;
            if (sidebarOpen) {
                boolean hover = drawElement(guiGraphics, sidebarLeft, parentGui.top, parentGui.width, parentGui.bottom, mouseX, mouseY, 0xB4000000);
                drawElement(guiGraphics, sidebarLeft + 1, parentGui.top + 1, sidebarRight - 1, parentGui.bottom - 1, mouseX, mouseY, 0x20FFFFFF);
                setButtonState(ElementType.SIDEBAR_PANEL, hover);
            } else {
                setButtonState(ElementType.SIDEBAR_PANEL, false);
            }

            for (int i = 0; i < sidebarButtons.length; ++i) {
                int x = parentGui.width - 5 - 28;
                int y = parentGui.bottom - 5 - (i + 1) * 28;
                int color = getButtonState(sidebarButtons[i]) ? 0xFFCCCCCC : 0xFFFFFFFF;
                setButtonState(sidebarButtons[i], drawTexturedElement(guiGraphics, GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH, waypointListIcon, x, y, x + 28, y + 28, mouseX, mouseY, color));
            }

            if (waypointListOpen) {
                drawWaypointList(guiGraphics, mouseX, mouseY, sidebarClicked);
            }
            sidebarClicked = false;

            if (timeOfMoveStart != -1L) {
                long timeSinceMoveStart = System.currentTimeMillis() - timeOfMoveStart;
                if (timeSinceMoveStart < 700.0F) {
                    float centerX = EasingUtils.easeOutExpo(moveStartX, moveGoalX, timeSinceMoveStart, 700.0F);
                    float centerZ = EasingUtils.easeOutExpo(moveStartZ, moveGoalZ, timeSinceMoveStart, 700.0F);
                    parentGui.centerAt(centerX, centerZ);
                } else {
                    parentGui.centerAt(moveGoalX, moveGoalZ);
                    timeOfMoveStart = -1L;
                }
            }
        }

        private void drawWaypointList(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean clicked) {
            filteredWaypoints.clear();
            for (Waypoint waypoint : parentGui.waypointManager.getWaypoints()) {
                if (waypoint.inDimension && waypoint.inWorld) {
                    filteredWaypoints.add(waypoint);
                }
            }

            if (!filteredWaypoints.isEmpty()) {
                int itemHeight = 20;
                int itemCount = Math.max(1, Math.round((float) (parentGui.height - 80) / itemHeight));
                int pageCount = Mth.ceil((float) filteredWaypoints.size() / itemCount);
                if (waypointListPage < 0) {
                    waypointListPage = 0;
                } else if (waypointListPage >= pageCount) {
                    waypointListPage = pageCount - 1;
                }
                int startIndex = waypointListPage * itemCount;
                int lastIndex = Math.min(filteredWaypoints.size(), (this.waypointListPage + 1) * itemCount);

                for (int i = startIndex; i < lastIndex; ++i) {
                    Waypoint waypoint = filteredWaypoints.get(i);

                    int newIndex = i - startIndex;
                    int itemY = parentGui.top + (itemHeight * newIndex);
                    boolean hover = mouseX >= sidebarLeft && mouseX <= sidebarRight && mouseY >= itemY && mouseY <= itemY + itemHeight;
                    float alpha = waypoint.enabled ? 1.0F : 0.3F;
                    float blue = hover ? 0.65F : 1.0F;
                    Sprite icon = parentGui.waypointManager.getTextureAtlas().getAtlasSprite("voxelmap:images/waypoints/waypoint" + waypoint.imageSuffix + ".png");
                    icon.blit(guiGraphics, GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH, sidebarLeft, itemY, 20, 20, waypoint.getUnifiedColor(alpha));
                    guiGraphics.drawString(parentGui.getFontRenderer(), waypoint.name, sidebarLeft + 20, itemY + 7, ARGB.colorFromFloat(alpha, 1.0F, 1.0F, blue));

                    if (hover && clicked) setupMove(waypoint.getX(), waypoint.getZ());
                }

                int buttonX = sidebarMiddle - 30;
                int buttonY = parentGui.bottom - 10;
                int buttonColor = getButtonState(ElementType.WAYPOINT_LIST_LEFT_ARROW) ? 0xFFCCCCCC : 0xFFFFFFFF;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(buttonX, buttonY, 0.0F);
                guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-90.0F));
                guiGraphics.pose().translate(-buttonX, -buttonY, 0.0F);
                setButtonState(ElementType.WAYPOINT_LIST_LEFT_ARROW, drawTexturedElement(guiGraphics, GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH, arrowIcon, buttonX - 8, buttonY - 8, buttonX + 8, buttonY + 8, mouseX, mouseY, buttonColor));
                guiGraphics.pose().popPose();

                buttonX = sidebarMiddle + 30;
                buttonColor = getButtonState(ElementType.WAYPOINT_LIST_RIGHT_ARROW) ? 0xFFCCCCCC : 0xFFFFFFFF;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(buttonX, buttonY, 0.0F);
                guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(90.0F));
                guiGraphics.pose().translate(-buttonX, -buttonY, 0.0F);
                setButtonState(ElementType.WAYPOINT_LIST_RIGHT_ARROW, drawTexturedElement(guiGraphics, GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH, arrowIcon, buttonX - 8, buttonY - 8, buttonX + 8, buttonY + 8, mouseX, mouseY, buttonColor));
                guiGraphics.pose().popPose();

                guiGraphics.drawCenteredString(parentGui.getFontRenderer(), (this.waypointListPage + 1) + " / " + pageCount, sidebarMiddle, parentGui.bottom - 15, 0xFFFFFF);

            } else {
                guiGraphics.drawCenteredString(parentGui.getFontRenderer(), "§E" + I18n.get("voxelmap.waypoints.no_waypoints_exist"), sidebarMiddle, parentGui.bottom - 15, 0xFFFFFF);
            }
        }

        private boolean drawElement(GuiGraphics guiGraphics, float x, float y, float x2, float y2, int mouseX, int mouseY, int color) {
            guiGraphics.drawSpecial(bufferSource -> {
                Matrix4f matrix4f = guiGraphics.pose().last().pose();

                VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.gui());

                vertexConsumer.addVertex(matrix4f, x, y2, 0).setUv(0.0F, 1.0F).setColor(color);
                vertexConsumer.addVertex(matrix4f, x2, y2, 0).setUv(1.0F, 1.0F).setColor(color);
                vertexConsumer.addVertex(matrix4f, x2, y, 0).setUv(1.0F, 0.0F).setColor(color);
                vertexConsumer.addVertex(matrix4f, x, y, 0).setUv(0.0F, 0.0F).setColor(color);
            });

            return mouseX >= x && mouseX <= x2 && mouseY >= y && mouseY <= y2;
        }

        private boolean drawTexturedElement(GuiGraphics guiGraphics, Function<ResourceLocation, RenderType> renderTypeMap, ResourceLocation resourceLocation, float x, float y, float x2, float y2, int mouseX, int mouseY, int color) {
            guiGraphics.drawSpecial(bufferSource -> {
                Matrix4f matrix4f = guiGraphics.pose().last().pose();

                VertexConsumer vertexConsumer = bufferSource.getBuffer(renderTypeMap.apply(resourceLocation));

                vertexConsumer.addVertex(matrix4f, x, y2, 0).setUv(0.0F, 1.0F).setColor(color);
                vertexConsumer.addVertex(matrix4f, x2, y2, 0).setUv(1.0F, 1.0F).setColor(color);
                vertexConsumer.addVertex(matrix4f, x2, y, 0).setUv(1.0F, 0.0F).setColor(color);
                vertexConsumer.addVertex(matrix4f, x, y, 0).setUv(0.0F, 0.0F).setColor(color);
            });

            return mouseX >= x && mouseX <= x2 && mouseY >= y && mouseY <= y2;
        }

        private boolean getButtonState(ElementType type) {
            if (buttonStates.containsKey(type)) {
                return buttonStates.get(type);
            }
            return false;
        }

        private void setButtonState(ElementType type, boolean state) {
            if (buttonStates.containsKey(type)) {
                buttonStates.replace(type, state);
            } else {
                buttonStates.put(type, state);
            }
        }

        private void setupMove(float goalX, float goalZ) {
            timeOfMoveStart = System.currentTimeMillis();
            moveStartX = parentGui.mapCenterX;
            moveStartZ = parentGui.mapCenterZ;
            moveGoalX = goalX;
            moveGoalZ = goalZ;
        }

        private boolean checkPressed(boolean doEvents) {
            boolean pressed = false;

            for (Map.Entry<ElementType, Boolean> state : buttonStates.entrySet()) {
                if (doEvents && state.getValue()) {
                    switch (state.getKey()) {
//                        case SIDEBAR_PANEL -> {}
                        case BUTTON_WAYPOINT_LIST -> waypointListOpen = !waypointListOpen;
                        case WAYPOINT_LIST_LEFT_ARROW -> --waypointListPage;
                        case WAYPOINT_LIST_RIGHT_ARROW -> ++waypointListPage;
                    }
                }
                pressed = state.getValue() || pressed;
            }

            if (doEvents && pressed) sidebarClicked = true;

            return pressed;
        }
    }
}
