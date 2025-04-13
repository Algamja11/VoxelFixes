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
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

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

    protected int mouseX;
    protected int mouseY;
    private float lastMouseX;
    private float lastMouseY;
    private boolean mouseCursorShown = true;
    private int pressedMouseButton = -1;
    private int pressedMouseButtonRaw = -1;
    private boolean leftMouseButtonDown;
    public boolean addClicked;
    public boolean editClicked;
    public boolean deleteClicked;
    private long timeOfRelease;

    private boolean keyboardInput;
    private boolean sprintKeyPressed;
    private boolean upKeyPressed;
    private boolean downKeyPressed;
    private boolean leftKeyPressed;
    private boolean rightKeyPressed;

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

    private int sidebarLeft;
    private int sidebarRight;
    private final boolean[] sidebarButtons = new boolean[2];

    private boolean waypointListOpen;
    private int waypointListPage;
    private final boolean[] waypointListButtons = new boolean[2];

    private final ResourceLocation voxelmapSkinLocation = ResourceLocation.fromNamespaceAndPath("voxelmap", "persistentmap/playerskin");
    private final ResourceLocation crosshairResource = ResourceLocation.parse("textures/gui/sprites/hud/crosshair.png");
    private final ResourceLocation separatorHeaderResource = ResourceLocation.parse("textures/gui/inworld_header_separator.png");
    private final ResourceLocation separatorFooterResource = ResourceLocation.parse("textures/gui/inworld_footer_separator.png");
    private final ResourceLocation arrowResource = ResourceLocation.parse("textures/gui/sprites/container/villager/trade_arrow.png");

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
            if (VoxelConstants.DEBUG) {
                VoxelConstants.getLogger().warn("Got no player skin!");
            }
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
        this.buttonWaypoints = new PopupGuiButton(this.sideMargin, this.getHeight() - 28, buttonWidth, 20, Component.translatable("options.voxelmap.waypoints"), buttonWidget_1 -> minecraft.setScreen(new GuiWaypoints(this)), this);
        this.addRenderableWidget(this.buttonWaypoints);
        this.multiworldButtonName = Component.translatable(VoxelConstants.isRealmServer() ? "menu.online" : "options.voxelmap.worldmap.multiworld");
        if (!minecraft.hasSingleplayerServer() && !waypointManager.receivedAutoSubworldName()) {
            this.addRenderableWidget(this.buttonMultiworld = new PopupGuiButton(this.sideMargin + (buttonWidth + buttonSeparation), this.getHeight() - 28, buttonWidth, 20, this.multiworldButtonName, buttonWidget_1 -> minecraft.setScreen(new GuiSubworldsSelect(this)), this));
        }

        this.addRenderableWidget(new PopupGuiButton(this.sideMargin + 3 * (buttonWidth + buttonSeparation), this.getHeight() - 28, buttonWidth, 20, Component.translatable("menu.options"), null, this) {
            @Override
            public void onPress() {
                minecraft.setScreen(new GuiMinimapOptions(GuiPersistentMap.this));
            }
        });
        this.addRenderableWidget(new PopupGuiButton(this.sideMargin + 4 * (buttonWidth + buttonSeparation), this.getHeight() - 28, buttonWidth, 20, Component.translatable("mco.selectServer.close"), null, this) {
            @Override
            public void onPress() {
                minecraft.setScreen(GuiPersistentMap.this.parentScreen);
            }
        });
    }

    private void centerAt(int x, int z) {
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

    private float easeOut(float elapsedTime, float startValue, float finalDelta, float totalTime) {
        float value;
        if (elapsedTime == totalTime) {
            value = startValue + finalDelta;
        } else {
            value = finalDelta * (-((float) Math.pow(2.0, -10.0F * elapsedTime / totalTime)) + 1.0F) + startValue;
        }

        return value;
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
        this.pressedMouseButtonRaw = button;

        boolean blockInput = this.checkSelectedButton(true);
        if (blockInput) {
            return false;
        }

        this.pressedMouseButton = button;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean blockInput = this.checkSelectedButton(false);

        if (mouseY > this.top && mouseY < this.bottom && button == 1 && !blockInput) {
            this.keyboardInput = false;
            int mouseDirectX = (int) minecraft.mouseHandler.xpos();
            int mouseDirectY = (int) minecraft.mouseHandler.ypos();
            if (VoxelMap.mapOptions.worldmapAllowed) {
                this.createPopup((int) mouseX, (int) mouseY, mouseDirectX, mouseDirectY);
            }
        }

        this.pressedMouseButton = -1;
        this.pressedMouseButtonRaw = -1;

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

        this.checkMovementKeyPressed(keyCode, scanCode);

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        this.checkMovementKeyReleased(keyCode, scanCode);

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private boolean checkSelectedButton(boolean doEvents) {
        boolean selected = false;
        int count = 0;
        for (boolean state : this.waypointListButtons) {
            if (state && doEvents) {
                switch (count) {
                    case 0 -> --this.waypointListPage;
                    case 1 -> ++this.waypointListPage;
                }
            }
            selected = selected || state;
            ++count;
        }
        count = 0;
        for (boolean state : this.sidebarButtons) {
            if (state && doEvents) {
                switch (count) {
                    // case 0 = panel background
                    case 1 -> this.waypointListOpen = !this.waypointListOpen;
                }
            }
            selected = selected || state;
            ++count;
        }

        return selected;
    }

    private void checkMovementKeyPressed(int keyCode, int scanCode) {
        if (minecraft.options.keySprint.matches(keyCode, scanCode)) {
            this.sprintKeyPressed = true;
        }
        if (minecraft.options.keyUp.matches(keyCode, scanCode)) {
            this.upKeyPressed = true;
        }
        if (minecraft.options.keyDown.matches(keyCode, scanCode)) {
            this.downKeyPressed = true;
        }
        if (minecraft.options.keyLeft.matches(keyCode, scanCode)) {
            this.leftKeyPressed = true;
        }
        if (minecraft.options.keyRight.matches(keyCode, scanCode)) {
            this.rightKeyPressed = true;
        }
    }

    private void checkMovementKeyReleased(int keyCode, int scanCode) {
        if (minecraft.options.keySprint.matches(keyCode, scanCode)) {
            this.sprintKeyPressed = false;
        }
        if (minecraft.options.keyUp.matches(keyCode, scanCode)) {
            this.upKeyPressed = false;
        }
        if (minecraft.options.keyDown.matches(keyCode, scanCode)) {
            this.downKeyPressed = false;
        }
        if (minecraft.options.keyLeft.matches(keyCode, scanCode)) {
            this.leftKeyPressed = false;
        }
        if (minecraft.options.keyRight.matches(keyCode, scanCode)) {
            this.rightKeyPressed = false;
        }
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
                this.zoom = this.easeOut(timeSinceZoom, this.zoomStart, this.zoomGoal - this.zoomStart, 700.0F);
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
        if (this.pressedMouseButton == 0) {
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
                this.deltaX = this.easeOut(timeSinceRelease, this.deltaXonRelease, -this.deltaXonRelease, 700.0F);
                this.deltaY = this.easeOut(timeSinceRelease, this.deltaYonRelease, -this.deltaYonRelease, 700.0F);
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
        if (this.sprintKeyPressed) {
            kbDelta = 10;
        }

        if (this.upKeyPressed) {
            this.deltaY -= kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
            this.switchToKeyboardInput();
        }

        if (this.downKeyPressed) {
            this.deltaY += kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
            this.switchToKeyboardInput();
        }

        if (this.leftKeyPressed) {
            this.deltaX -= kbDelta / scaledZoom * timeSinceLastTick / 12.0F;
            this.switchToKeyboardInput();
        }

        if (this.rightKeyPressed) {
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
                for (Waypoint pt : this.waypointManager.getWaypoints()) {
                    this.drawWaypoint(guiGraphics, pt, cursorCoordX, cursorCoordZ, null);
                }

                if (this.waypointManager.getHighlightedWaypoint() != null) {
                    this.drawWaypoint(guiGraphics, this.waypointManager.getHighlightedWaypoint(), cursorCoordX, cursorCoordZ, waypointManager.getTextureAtlas().getAtlasSprite("voxelmap:images/waypoints/target.png"));
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

        if (this.waypointListOpen) {
            this.sidebarButtons[0] = this.drawSidePanel(guiGraphics, 200, mouseX, mouseY);
            this.drawWaypointList(guiGraphics, mouseX, mouseY);
        } else {
            this.sidebarButtons[0] = false;
        }

        int sideButtonX = this.width - 5 - 28;
        int sideButtonY = this.height - 40 - 28;
        this.sidebarButtons[1] = mouseX >= sideButtonX && mouseX <= sideButtonX + 28 && mouseY >= sideButtonY && mouseY <= sideButtonY + 28;
        Sprite waypointIcon = waypointManager.getTextureAtlas().getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
        waypointIcon.blit(guiGraphics, GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH, sideButtonX, sideButtonY, 28, 28, this.sidebarButtons[1] ? 0xFFCCCCCC : 0xFFFFFFFF);

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
        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // nothing
    }

    private void drawWaypoint(GuiGraphics guiGraphics, Waypoint pt, float cursorCoordX, float cursorCoordZ, Sprite icon) {
        if (!pt.inWorld || !pt.inDimension) {
            return;
        }
        float ptX = pt.getX();
        float ptZ = pt.getZ();

        PoseStack poseStack = guiGraphics.pose();
        String name = pt.name;

        ptX += 0.5F;
        ptZ += 0.5F;
        boolean target = false;

        boolean hover = cursorCoordX > ptX - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordX < ptX + 18.0F * this.guiToMap / this.guiToDirectMouse
                && cursorCoordZ > ptZ - 18.0F * this.guiToMap / this.guiToDirectMouse && cursorCoordZ < ptZ + 18.0F * this.guiToMap / this.guiToDirectMouse;

        int ptXScreen = (int) ((ptX - this.mapCenterX) * this.mapToGui);
        int ptZScreen = (int) ((ptZ - this.mapCenterZ) * this.mapToGui);
        int halfWidthX = this.width / 2 - 4;
        int halfWidthZ = this.height / 2 - 32 - 4;
        boolean far = !(ptXScreen >= -halfWidthX && ptXScreen <= halfWidthX && ptZScreen >= -halfWidthZ && ptZScreen <= halfWidthZ);
        if (far) {
            ptX = Math.max(this.mapCenterX - (halfWidthX * this.guiToMap), Math.min(this.mapCenterX + (halfWidthX * this.guiToMap), ptX));
            ptZ = Math.max(this.mapCenterZ - (halfWidthZ * this.guiToMap), Math.min(this.mapCenterZ + (halfWidthZ * this.guiToMap), ptZ));
        }
        float locate = (float) Math.toDegrees(Math.atan2(pt.getX() - ptX, -(pt.getZ() - ptZ)));

        TextureAtlas atlas = waypointManager.getTextureAtlas();
        if (icon != null) {
            name = "";
            target = true;
        } else if (far) {
            if (!pt.isDeathpoint) {
                icon = atlas.getAtlasSprite("voxelmap:images/waypoints/marker.png");
            } else {
                icon = atlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + pt.imageSuffix + ".png");

                if (icon == atlas.getMissingImage()) {
                    icon = atlas.getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
                }
            }
        } else {
            icon = atlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + pt.imageSuffix + ".png");

            if (icon == atlas.getMissingImage()) {
                icon = atlas.getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
            }
        }

        int color = pt.getUnifiedColor(!pt.enabled && !target && !hover ? 0.3F : 1.0F);

        poseStack.pushPose();
        poseStack.scale(this.guiToMap, this.guiToMap, 1.0F);
        if (!target && !pt.isDeathpoint && far) {
            poseStack.translate(ptX * this.mapToGui, ptZ * this.mapToGui, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(locate));
            poseStack.translate(-ptX * this.mapToGui, -ptZ * this.mapToGui, 0.0F);
        }
        icon.blit(guiGraphics, GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH, ptX * this.mapToGui - 8, ptZ * this.mapToGui - 8, 16, 16, color);

        poseStack.popPose();

        if (this.options.showWaypointNames && !far) {
            int labelWidth = this.chkLen(name) / 2;
            poseStack.pushPose();
            poseStack.scale(this.guiToMap, this.guiToMap, 1);
            this.write(guiGraphics, name, ptX * this.mapToGui - labelWidth, ptZ * this.mapToGui + 8, !pt.enabled && !target && !hover ? 0x55FFFFFF : 0xFFFFFFFF);
            poseStack.popPose();
        }
    }

    private void overlayBackground(GuiGraphics guiGraphics) {
        guiGraphics.drawSpecial(bufferSource -> {
            Matrix4f matrix4f = guiGraphics.pose().last().pose();

            float renderedTextureSize = this.width / 32.0F;

            VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.guiTextured(separatorHeaderResource));
            vertexConsumer.addVertex(matrix4f, 0.0F, 32.0F + 2.0F, 0.0F).setUv(0.0F, 1.0F).setColor(0xFFFFFFFF);
            vertexConsumer.addVertex(matrix4f, this.width, 32.0F + 2.0F, 0.0F).setUv(renderedTextureSize, 1.0F).setColor(0xFFFFFFFF);
            vertexConsumer.addVertex(matrix4f, this.width, 32.0F, 0.0F).setUv(renderedTextureSize, 0.0F).setColor(0xFFFFFFFF);
            vertexConsumer.addVertex(matrix4f, 0.0F, 32.0F, 0.0F).setUv(0.0F, 0.0F).setColor(0xFFFFFFFF);

            vertexConsumer = bufferSource.getBuffer(RenderType.guiTextured(separatorFooterResource));
            vertexConsumer.addVertex(matrix4f, 0.0F, this.height - 32.0F, 0.0F).setUv(0.0F, 1.0F).setColor(0xFFFFFFFF);
            vertexConsumer.addVertex(matrix4f, this.width, this.height - 32.0F, 0.0F).setUv(renderedTextureSize, 1.0F).setColor(0xFFFFFFFF);
            vertexConsumer.addVertex(matrix4f, this.width, this.height - 32.0F - 2.0F, 0.0F).setUv(renderedTextureSize, 0.0F).setColor(0xFFFFFFFF);
            vertexConsumer.addVertex(matrix4f, 0.0F, this.height - 32.0F - 2.0F, 0.0F).setUv(0.0F, 0.0F).setColor(0xFFFFFFFF);

            vertexConsumer = bufferSource.getBuffer(RenderType.gui());
            vertexConsumer.addVertex(matrix4f, 0.0F, 32.0F, 0.0F).setColor(0, 0, 0, 128);
            vertexConsumer.addVertex(matrix4f, this.width, 32.0F, 0.0F).setColor(0, 0, 0, 128);
            vertexConsumer.addVertex(matrix4f, this.width, 0.0F, 0.0F).setColor(0, 0, 0, 128);
            vertexConsumer.addVertex(matrix4f, 0.0F, 0.0F, 0.0F).setColor(0, 0, 0, 128);
            vertexConsumer.addVertex(matrix4f, 0.0F, this.height, 0.0F).setColor(0, 0, 0, 128);
            vertexConsumer.addVertex(matrix4f, this.width, this.height, 0.0F).setColor(0, 0, 0, 128);
            vertexConsumer.addVertex(matrix4f, this.width, this.height - 32.0F, 0.0F).setColor(0, 0, 0, 128);
            vertexConsumer.addVertex(matrix4f, 0.0F, this.height - 32.0F, 0.0F).setColor(0, 0, 0, 128);
        });
    }

    private boolean drawSidePanel(GuiGraphics guiGraphics, int width, int mouseX, int mouseY) {
        this.sidebarRight = this.width - 40;
        this.sidebarLeft = this.sidebarRight - width;
        guiGraphics.drawSpecial(bufferSource -> {
            Matrix4f matrix4f = guiGraphics.pose().last().pose();
            VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.gui());

            vertexConsumer.addVertex(matrix4f, this.sidebarLeft, this.height - 34, 0.0F).setColor(0, 0, 0, 128);
            vertexConsumer.addVertex(matrix4f, this.width, this.height - 34, 0.0F).setColor(0, 0, 0, 128);
            vertexConsumer.addVertex(matrix4f, this.width, 34, 0.0F).setColor(0, 0, 0, 128);
            vertexConsumer.addVertex(matrix4f, this.sidebarLeft, 34, 0.0F).setColor(0, 0, 0, 128);

            vertexConsumer.addVertex(matrix4f, this.sidebarLeft + 1, this.height - 35, 0.0F).setColor(255, 255, 255, 32);
            vertexConsumer.addVertex(matrix4f, this.sidebarRight - 1, this.height - 35, 0.0F).setColor(255, 255, 255, 32);
            vertexConsumer.addVertex(matrix4f, this.sidebarRight - 1, 35, 0.0F).setColor(255, 255, 255, 32);
            vertexConsumer.addVertex(matrix4f, this.sidebarLeft + 1, 35, 0.0F).setColor(255, 255, 255, 32);
        });
        return mouseX >= this.sidebarLeft && mouseX <= this.width && mouseY >= 34 && mouseY <= this.height - 34;
    }

    private void drawWaypointList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        ArrayList<Waypoint> waypoints = this.waypointManager.getWaypoints();
        int filteredCount = 0;
        for (Waypoint point : waypoints) {
            if (point.inDimension && point.inWorld) {
                filteredCount++;
            }
        }
        int itemHeight = 20;
        int itemCount = Math.max(1, (this.height - 80) / itemHeight);
        int itemMax = Math.min(waypoints.size(), (this.waypointListPage + 1) * itemCount);
        int maxPageCount = (int) Math.ceil((float) filteredCount / itemCount);
        if (this.waypointListPage < 0) {
            this.waypointListPage = 0;
        } else if (this.waypointListPage >= maxPageCount) {
            this.waypointListPage = maxPageCount - 1;
        }
        int skipCount = 0;
        for (int i = this.waypointListPage * itemCount; i < itemMax; ++i) {
            Waypoint point = waypoints.get(i);
            if (!point.inDimension || !point.inWorld) {
                ++skipCount;
                continue;
            }

            int countInPage = (i - skipCount) - this.waypointListPage * itemCount;
            int itemY = 35 + (itemHeight * countInPage);
            boolean hover = mouseX >= this.sidebarLeft && mouseX <= this.sidebarRight && mouseY >= itemY && mouseY <= itemY + itemHeight;
            float alpha = point.enabled ? 1.0F : 0.3F;
            float blue = hover ? 0.65F : 1.0F;
            Sprite icon = this.waypointManager.getTextureAtlas().getAtlasSprite("voxelmap:images/waypoints/waypoint" + point.imageSuffix + ".png");
            icon.blit(guiGraphics, GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH, this.sidebarLeft, itemY, 20, 20, point.getUnifiedColor(alpha));
            guiGraphics.drawString(this.getFontRenderer(), point.name, this.sidebarLeft + 20, itemY + 7, ARGB.colorFromFloat(alpha, 1.0F, 1.0F, blue));

            if (this.pressedMouseButtonRaw == 0 && hover) {
                this.centerAt(point.getX(), point.getZ());
            }
        }

        int buttonCenter = (this.sidebarLeft + this.sidebarRight) / 2;
        int buttonX = buttonCenter - 30;
        int buttonY = this.height - 40;
        this.waypointListButtons[0] = mouseX >= buttonX - 4 && mouseX <= buttonX + 4 && mouseY >= buttonY - 4 && mouseY <= buttonY + 4;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(buttonX, buttonY, 0.0F);
        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
        guiGraphics.pose().translate(-buttonX, -buttonY, 0.0F);
        guiGraphics.blit(RenderType::guiTextured, arrowResource, buttonX - 4, buttonY - 4, 0.0F, 0.0F, 8, 8, 8, 8);
        guiGraphics.pose().popPose();

        buttonX = buttonCenter + 30;
        this.waypointListButtons[1] = mouseX >= buttonX - 4 && mouseX <= buttonX + 4 && mouseY >= buttonY - 4 && mouseY <= buttonY + 4;
        guiGraphics.blit(RenderType::guiTextured, arrowResource, buttonX - 4, buttonY - 4, 0.0F, 0.0F, 8, 8, 8, 8);

        buttonX = buttonCenter;
        buttonY = this.height - 45;
        guiGraphics.drawCenteredString(this.getFontRenderer(), (this.waypointListPage + 1) + " / " + maxPageCount, buttonX, buttonY, 0xFFFFFF);
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
            entry = new Popup.PopupEntry(I18n.get(hovered != this.waypointManager.getHighlightedWaypoint() ? "voxelmap.waypoints.highlight" : "voxelmap.waypoints.removehighlight"), 1, true, true);
        } else {
            entry = new Popup.PopupEntry(I18n.get("voxelmap.waypoints.newwaypoint"), 0, true, VoxelMap.mapOptions.waypointsAllowed);
            entries.add(entry);
            entry = new Popup.PopupEntry(I18n.get(hovered == null ? "voxelmap.waypoints.highlight" : "voxelmap.waypoints.removehighlight"), 1, true, VoxelMap.mapOptions.waypointsAllowed);
        }
        entries.add(entry);
        entry = new Popup.PopupEntry(I18n.get("voxelmap.waypoints.teleportto"), 3, true, true);
        entries.add(entry);
        entry = new Popup.PopupEntry(I18n.get("voxelmap.waypoints.share"), 2, true, true);
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
                    Component title = Component.translatable("voxelmap.waypoints.deleteconfirm");
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
}
