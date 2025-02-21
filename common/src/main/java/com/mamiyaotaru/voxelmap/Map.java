package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.gui.GuiAddWaypoint;
import com.mamiyaotaru.voxelmap.gui.GuiWaypoints;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.interfaces.IChangeObserver;
import com.mamiyaotaru.voxelmap.persistent.GuiPersistentMap;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.BlockRepository;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.FullMapData;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mamiyaotaru.voxelmap.util.MapChunkCache;
import com.mamiyaotaru.voxelmap.util.MapUtils;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mamiyaotaru.voxelmap.util.MutableBlockPosCache;
import com.mamiyaotaru.voxelmap.util.MutableNativeImageBackedTexture;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mamiyaotaru.voxelmap.util.ScaledMutableNativeImageBackedTexture;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.OutOfMemoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11C;

public class Map implements Runnable, IChangeObserver {
    private final float[] lastLightBrightnessTable = new float[16];
    private final Object coordinateLock = new Object();
    private final ResourceLocation directionArrow = ResourceLocation.fromNamespaceAndPath("voxelmap", "images/mmarrow.png");
    private final ResourceLocation roundMapFrame = ResourceLocation.fromNamespaceAndPath("voxelmap", "images/roundmap.png");
    private final ResourceLocation squareMapFrame = ResourceLocation.fromNamespaceAndPath("voxelmap", "images/squaremap.png");
    private final ResourceLocation fullscreenMapFrame = ResourceLocation.fromNamespaceAndPath("voxelmap", "images/fullscreenmap.png");
    private final ResourceLocation circleStencil = ResourceLocation.fromNamespaceAndPath("voxelmap", "images/circle.png");
    private final ResourceLocation squareStencil = ResourceLocation.fromNamespaceAndPath("voxelmap", "images/square.png");
    private ClientLevel world;
    private final MapSettingsManager options;
    private final LayoutVariables layoutVariables;
    private final ColorManager colorManager;
    private final WaypointManager waypointManager;
    private final int availableProcessors = Runtime.getRuntime().availableProcessors();
    private final boolean multicore = this.availableProcessors > 1;
    private final int heightMapResetHeight = this.multicore ? 2 : 5;
    private final int heightMapResetTime = this.multicore ? 300 : 3000;
    private final boolean threading = this.multicore;
    private final FullMapData[] mapData = new FullMapData[5];
    private final MapChunkCache[] chunkCache = new MapChunkCache[5];
    private MutableNativeImageBackedTexture[] mapImages;
    private final MutableNativeImageBackedTexture[] mapImagesFiltered = new MutableNativeImageBackedTexture[5];
    private final MutableNativeImageBackedTexture[] mapImagesUnfiltered = new MutableNativeImageBackedTexture[5];
    private BlockState transparentBlockState;
    private BlockState surfaceBlockState;
    private boolean imageChanged = true;
    private LightTexture lightmapTexture;
    private boolean needLightmapRefresh = true;
    private int tickWithLightChange;
    private boolean lastPaused = true;
    private double lastGamma;
    private float lastSunBrightness;
    private float lastLightning;
    private float lastPotion;
    private final int[] lastLightmapValues = {-16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216, -16777216};
    private boolean lastBeneathRendering;
    private boolean needSkyColor;
    private boolean lastAboveHorizon = true;
    private int lastBiome;
    private int lastSkyColor;
    private final Random generator = new Random();
    private boolean showWelcomeScreen;
    private Screen lastGuiScreen;
    private boolean fullscreenMap;
    private int zoom;
    private int scWidth;
    private int scHeight;
    private String message = "";
    private final Component[] welcomeText = new Component[8];
    private int zTimer;
    private int heightMapFudge;
    private int timer;
    private boolean doFullRender = true;
    private boolean zoomChanged;
    private int lastX;
    private int lastZ;
    private int lastY;
    private int lastImageX;
    private int lastImageZ;
    private boolean lastFullscreen;
    private float direction;
    private int northRotate;
    private Thread zCalc = new Thread(this, "Voxelmap LiveMap Calculation Thread");
    private int zCalcTicker;
    private final Font fontRenderer;
    private final int[] lightmapColors = new int[256];
    private double zoomScale = 1.0;
    private double zoomScaleAdjusted = 1.0;
    private static double minTablistOffset;
    private static float statusIconOffset = 0.0F;
    private int deltaTime = 0;
    private long deltaTimeBase = 0;

    public Map() {
        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.colorManager = VoxelConstants.getVoxelMapInstance().getColorManager();
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        this.layoutVariables = new LayoutVariables();
        ArrayList<KeyMapping> tempBindings = new ArrayList<>();
        tempBindings.addAll(Arrays.asList(VoxelConstants.getMinecraft().options.keyMappings));
        tempBindings.addAll(Arrays.asList(this.options.keyBindings));
        VoxelConstants.getMinecraft().options.keyMappings = tempBindings.toArray(new KeyMapping[0]);

        java.util.Map<String, Integer> categoryOrder = KeyMapping.CATEGORY_SORT_ORDER;
        VoxelConstants.getLogger().warn("CATEGORY ORDER IS " + categoryOrder.size());
        Integer categoryPlace = categoryOrder.get("controls.minimap.title");
        if (categoryPlace == null) {
            int currentSize = categoryOrder.size();
            categoryOrder.put("controls.minimap.title", currentSize + 1);
        }

        this.showWelcomeScreen = this.options.welcome;
        this.zCalc.start();
        this.mapData[0] = new FullMapData(32, 32);
        this.mapData[1] = new FullMapData(64, 64);
        this.mapData[2] = new FullMapData(128, 128);
        this.mapData[3] = new FullMapData(256, 256);
        this.mapData[4] = new FullMapData(512, 512);
        this.chunkCache[0] = new MapChunkCache(3, 3, this);
        this.chunkCache[1] = new MapChunkCache(5, 5, this);
        this.chunkCache[2] = new MapChunkCache(9, 9, this);
        this.chunkCache[3] = new MapChunkCache(17, 17, this);
        this.chunkCache[4] = new MapChunkCache(33, 33, this);
        this.mapImagesFiltered[0] = new MutableNativeImageBackedTexture(32, 32, true);
        this.mapImagesFiltered[1] = new MutableNativeImageBackedTexture(64, 64, true);
        this.mapImagesFiltered[2] = new MutableNativeImageBackedTexture(128, 128, true);
        this.mapImagesFiltered[3] = new MutableNativeImageBackedTexture(256, 256, true);
        this.mapImagesFiltered[4] = new MutableNativeImageBackedTexture(512, 512, true);
        this.mapImagesUnfiltered[0] = new ScaledMutableNativeImageBackedTexture(32, 32, true);
        this.mapImagesUnfiltered[1] = new ScaledMutableNativeImageBackedTexture(64, 64, true);
        this.mapImagesUnfiltered[2] = new ScaledMutableNativeImageBackedTexture(128, 128, true);
        this.mapImagesUnfiltered[3] = new ScaledMutableNativeImageBackedTexture(256, 256, true);
        this.mapImagesUnfiltered[4] = new ScaledMutableNativeImageBackedTexture(512, 512, true);
        if (this.options.filtering) {
            this.mapImages = this.mapImagesFiltered;
        } else {
            this.mapImages = this.mapImagesUnfiltered;
        }

        OpenGL.Utils.setupFramebuffer();
        this.fontRenderer = VoxelConstants.getMinecraft().font;
        this.zoom = this.options.zoom;
        this.setZoomScale();
    }

    public void forceFullRender(boolean forceFullRender) {
        this.doFullRender = forceFullRender;
        VoxelConstants.getVoxelMapInstance().getSettingsAndLightingChangeNotifier().notifyOfChanges();
    }

    @Override
    public void run() {
        if (VoxelConstants.getMinecraft() != null) {
            while (true) {
                if (this.world != null) {
                    if (this.options.displayMode != 0 && this.options.minimapAllowed) {
                        try {
                            this.mapCalc(this.doFullRender);
                            if (!this.doFullRender) {
                                MutableBlockPos blockPos = MutableBlockPosCache.get();
                                this.chunkCache[this.zoom].centerChunks(blockPos.withXYZ(this.lastX, 0, this.lastZ));
                                MutableBlockPosCache.release(blockPos);
                                this.chunkCache[this.zoom].checkIfChunksChanged();
                            }
                        } catch (Exception exception) {
                            VoxelConstants.getLogger().error("Voxelmap LiveMap Calculation Thread", exception);
                        }
                    }

                    this.doFullRender = this.zoomChanged;
                    this.zoomChanged = false;
                }

                this.zCalcTicker = 0;
                synchronized (this.zCalc) {
                    try {
                        this.zCalc.wait(0L);
                    } catch (InterruptedException exception) {
                        VoxelConstants.getLogger().error("Voxelmap LiveMap Calculation Thread", exception);
                    }
                }
            }
        }

    }

    public void newWorld(ClientLevel world) {
        this.world = world;
        this.lightmapTexture = this.getLightmapTexture();
        this.mapData[this.zoom].blank();
        this.doFullRender = true;
        VoxelConstants.getVoxelMapInstance().getSettingsAndLightingChangeNotifier().notifyOfChanges();
    }

    public void newWorldName() {
        String subworldName = this.waypointManager.getCurrentSubworldDescriptor(true);
        StringBuilder subworldNameBuilder = (new StringBuilder("§r")).append(I18n.get("worldmap.multiworld.newworld")).append(":").append(" ");
        if (subworldName.isEmpty() && this.waypointManager.isMultiworld()) {
            subworldNameBuilder.append("???");
        } else if (!subworldName.isEmpty()) {
            subworldNameBuilder.append(subworldName);
        }

        this.message = subworldNameBuilder.toString();
    }

    public void onTickInGame(GuiGraphics drawContext) {
        this.northRotate = this.options.oldNorth ? 90 : 0;

        if (this.lightmapTexture == null) {
            this.lightmapTexture = this.getLightmapTexture();
        }

        if (VoxelConstants.getMinecraft().screen == null && this.options.keyBindMenu.consumeClick()) {
            this.showWelcomeScreen = false;
            if (this.options.welcome) {
                this.options.welcome = false;
                this.options.saveAll();
            }

            VoxelConstants.getMinecraft().setScreen(new GuiPersistentMap(null));
        }

        if (VoxelConstants.getMinecraft().screen == null && this.options.keyBindWaypointMenu.consumeClick()) {
            this.showWelcomeScreen = false;
            if (this.options.welcome) {
                this.options.welcome = false;
                this.options.saveAll();
            }
            if (VoxelMap.mapOptions.waypointsAllowed) {
                VoxelConstants.getMinecraft().setScreen(new GuiWaypoints(null));
            }
        }

        if (VoxelConstants.getMinecraft().screen == null && this.options.keyBindWaypoint.consumeClick()) {
            this.showWelcomeScreen = false;
            if (this.options.welcome) {
                this.options.welcome = false;
                this.options.saveAll();
            }

            if (VoxelMap.mapOptions.waypointsAllowed) {
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
                double dimensionScale = VoxelConstants.getPlayer().level().dimensionType().coordinateScale();
                Waypoint newWaypoint = new Waypoint("", (int) (GameVariableAccessShim.xCoord() * dimensionScale), (int) (GameVariableAccessShim.zCoord() * dimensionScale), GameVariableAccessShim.yCoord(), true, r, g, b, "",
                        VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentSubworldDescriptor(false), dimensions);
                VoxelConstants.getMinecraft().setScreen(new GuiAddWaypoint(null, newWaypoint, false));
            }
        }

        if (VoxelConstants.getMinecraft().screen == null && this.options.keyBindRadarToggle.consumeClick()) {
            VoxelConstants.getVoxelMapInstance().getRadarOptions().setOptionValue(EnumOptionsMinimap.SHOWRADAR);
            this.options.saveAll();
        }

        if (VoxelConstants.getMinecraft().screen == null && this.options.keyBindWaypointToggle.consumeClick()) {
            this.options.toggleIngameWaypoints();
        }

        if (VoxelConstants.getMinecraft().screen == null && this.options.keyBindZoom.consumeClick()) {
            this.changeZoom(-1);
        }

        if (VoxelConstants.getMinecraft().screen == null && this.options.keyBindZoomOut.consumeClick()) {
            this.changeZoom(1);
        }

        if (VoxelConstants.getMinecraft().screen == null && this.options.keyBindFullscreen.consumeClick()) {
            this.showWelcomeScreen = false;
            if (this.options.welcome) {
                this.options.welcome = false;
                this.options.saveAll();
            } else {
                this.fullscreenMap = !this.fullscreenMap;
                this.showZoomLevel();
            }
        }

        this.checkForChanges();
        if (VoxelMap.mapOptions.deathWaypointAllowed && VoxelConstants.getMinecraft().screen instanceof DeathScreen && !(this.lastGuiScreen instanceof DeathScreen)) {
            this.waypointManager.handleDeath();
        }

        this.lastGuiScreen = VoxelConstants.getMinecraft().screen;
        this.calculateCurrentLightAndSkyColor();
        if (this.threading) {
            if (!this.zCalc.isAlive()) {
                this.zCalc = new Thread(this, "Voxelmap LiveMap Calculation Thread");
                this.zCalc.start();
                this.zCalcTicker = 0;
            }

            if (!(VoxelConstants.getMinecraft().screen instanceof DeathScreen) && !(VoxelConstants.getMinecraft().screen instanceof OutOfMemoryScreen)) {
                ++this.zCalcTicker;
                if (this.zCalcTicker > 2000) {
                    this.zCalcTicker = 0;
                    Exception ex = new Exception();
                    ex.setStackTrace(this.zCalc.getStackTrace());
                    DebugRenderState.print();
                    VoxelConstants.getLogger().error("Voxelmap LiveMap Calculation Thread is hanging?", ex);
                }
                synchronized (this.zCalc) {
                    this.zCalc.notify();
                }
            }
        } else {
            if (this.options.displayMode != 0 && this.options.minimapAllowed && this.world != null) {
                this.mapCalc(this.doFullRender);
                if (!this.doFullRender) {
                    MutableBlockPos blockPos = MutableBlockPosCache.get();
                    this.chunkCache[this.zoom].centerChunks(blockPos.withXYZ(this.lastX, 0, this.lastZ));
                    MutableBlockPosCache.release(blockPos);
                    this.chunkCache[this.zoom].checkIfChunksChanged();
                }
            }

            this.doFullRender = false;
        }

        boolean enabled = !VoxelConstants.getMinecraft().options.hideGui && (this.options.showUnderMenus || VoxelConstants.getMinecraft().screen == null) && !VoxelConstants.getMinecraft().getDebugOverlay().showDebugScreen();

        this.direction = GameVariableAccessShim.rotationYaw() + 180.0F;

        while (this.direction >= 360.0F) {
            this.direction -= 360.0F;
        }

        while (this.direction < 0.0F) {
            this.direction += 360.0F;
        }

        if (!this.message.isEmpty() && this.zTimer <= 0) {
            this.zTimer = 1500;
        }

        this.deltaTime = Math.max(1, (int)(System.currentTimeMillis() - this.deltaTimeBase));
        this.deltaTimeBase = System.currentTimeMillis();
        if (this.zTimer > 0) {
            this.zTimer -= this.deltaTime;
        }

        if (this.zTimer <= 0 && !this.message.isEmpty()) {
            this.message = "";
        }

        if (enabled && VoxelMap.mapOptions.minimapAllowed) {
            this.drawMinimap(drawContext);
        }

        this.timer = this.timer > 5000 ? 0 : this.timer + 1;
    }

    private void changeZoom(int step) {
        this.options.zoom += step;
        if (this.options.zoom < 0) {
            this.options.zoom = 4;
        } else if (this.options.zoom > 4) {
            this.options.zoom = 0;
        }
        this.options.saveAll();
        this.zoomChanged = true;
        this.zoom = this.options.zoom;
        this.setZoomScale();
        this.doFullRender = true;
        this.showZoomLevel();
    }

    private void showZoomLevel() {
        if (this.zoom == 4) {
            this.message = I18n.get("minimap.ui.zoomlevel") + " (0.25x)";
        } else if (this.zoom == 3) {
            this.message = I18n.get("minimap.ui.zoomlevel") + " (0.5x)";
        } else if (this.zoom == 2) {
            this.message = I18n.get("minimap.ui.zoomlevel") + " (1.0x)";
        } else if (this.zoom == 1) {
            this.message = I18n.get("minimap.ui.zoomlevel") + " (2.0x)";
        } else if (this.zoom == 0) {
            this.message = I18n.get("minimap.ui.zoomlevel") + " (4.0x)";
        }
        this.zTimer = 1500;
    }

    private void setZoomScale() {
        this.zoomScale = Math.pow(2.0, this.zoom) / 2.0;
        if (this.options.shape == 1 && this.options.rotates) {
            this.zoomScaleAdjusted = this.zoomScale / 1.4142F;
        } else {
            this.zoomScaleAdjusted = this.zoomScale;
        }
    }

    private LightTexture getLightmapTexture() {
        return VoxelConstants.getMinecraft().gameRenderer.lightTexture();
    }

    public void calculateCurrentLightAndSkyColor() {
        try {
            if (this.world != null) {
                if (this.needLightmapRefresh && VoxelConstants.getElapsedTicks() != this.tickWithLightChange && !VoxelConstants.getMinecraft().isPaused()) {
                    OpenGL.Utils.disp(this.lightmapTexture.target.getColorTextureId());
                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder());
                    OpenGL.glGetTexImage(OpenGL.GL11_GL_TEXTURE_2D, 0, OpenGL.GL11_GL_RGBA, OpenGL.GL11_GL_UNSIGNED_BYTE, byteBuffer);

                    for (int i = 0; i < this.lightmapColors.length; ++i) {
                        int index = i * 4;
                        this.lightmapColors[i] = (byteBuffer.get(index + 3) << 24) + (byteBuffer.get(index) << 16) + (byteBuffer.get(index + 1) << 8) + (byteBuffer.get(index + 2));
                    }

                    if (this.lightmapColors[255] != 0) {
                        this.needLightmapRefresh = false;
                    }
                }

                boolean lightChanged = false;
                if (VoxelConstants.getMinecraft().options.gamma().get() != this.lastGamma) {
                    lightChanged = true;
                    this.lastGamma = VoxelConstants.getMinecraft().options.gamma().get();
                }

                float[] providerLightBrightnessTable = new float[16];

                for (int t = 0; t < 16; ++t) {
                    providerLightBrightnessTable[t] = this.world.dimensionType().timeOfDay(t);
                }

                for (int t = 0; t < 16; ++t) {
                    if (providerLightBrightnessTable[t] != this.lastLightBrightnessTable[t]) {
                        lightChanged = true;
                        this.lastLightBrightnessTable[t] = providerLightBrightnessTable[t];
                    }
                }

                float sunBrightness = this.world.getSkyDarken(1.0F);
                if (Math.abs(this.lastSunBrightness - sunBrightness) > 0.01 || sunBrightness == 1.0 && sunBrightness != this.lastSunBrightness || sunBrightness == 0.0 && sunBrightness != this.lastSunBrightness) {
                    lightChanged = true;
                    this.needSkyColor = true;
                    this.lastSunBrightness = sunBrightness;
                }

                float potionEffect = 0.0F;
                if (VoxelConstants.getPlayer().hasEffect(MobEffects.NIGHT_VISION)) {
                    int duration = VoxelConstants.getPlayer().getEffect(MobEffects.NIGHT_VISION).getDuration();
                    potionEffect = duration > 200 ? 1.0F : 0.7F + Mth.sin((duration - 1.0F) * (float) Math.PI * 0.2F) * 0.3F;
                }

                if (this.lastPotion != potionEffect) {
                    this.lastPotion = potionEffect;
                    lightChanged = true;
                }

                int lastLightningBolt = this.world.getSkyFlashTime();
                if (this.lastLightning != lastLightningBolt) {
                    this.lastLightning = lastLightningBolt;
                    lightChanged = true;
                }

                if (this.lastPaused != VoxelConstants.getMinecraft().isPaused()) {
                    this.lastPaused = !this.lastPaused;
                    lightChanged = true;
                }

                boolean scheduledUpdate = (this.timer - 50) % (this.lastLightBrightnessTable[0] == 0.0F ? 50 : 100) == 0;
                if (lightChanged || scheduledUpdate) {
                    this.tickWithLightChange = VoxelConstants.getElapsedTicks();
                    this.needLightmapRefresh = true;
                }

                boolean aboveHorizon = VoxelConstants.getPlayer().getEyePosition(0.0F).y >= this.world.getLevelData().getHorizonHeight(this.world);
                if (this.world.dimension().location().toString().toLowerCase().contains("ether")) {
                    aboveHorizon = true;
                }

                if (aboveHorizon != this.lastAboveHorizon) {
                    this.needSkyColor = true;
                    this.lastAboveHorizon = aboveHorizon;
                }

                MutableBlockPos blockPos = MutableBlockPosCache.get();
                int biomeID = this.world.registryAccess().lookupOrThrow(Registries.BIOME).getId(this.world.getBiome(blockPos.withXYZ(GameVariableAccessShim.xCoord(), GameVariableAccessShim.yCoord(), GameVariableAccessShim.zCoord())).value());
                MutableBlockPosCache.release(blockPos);
                if (biomeID != this.lastBiome) {
                    this.needSkyColor = true;
                    this.lastBiome = biomeID;
                }

                if (this.needSkyColor || scheduledUpdate) {
                    this.colorManager.setSkyColor(this.getSkyColor());
                }
            }
        } catch (NullPointerException ignore) {

        }
    }

    private int getSkyColor() {
        this.needSkyColor = false;
        boolean aboveHorizon = this.lastAboveHorizon;
        Vector4f color = FogRenderer.computeFogColor(VoxelConstants.getMinecraft().gameRenderer.getMainCamera(), 0.0F, this.world, VoxelConstants.getMinecraft().options.renderDistance().get(), VoxelConstants.getMinecraft().gameRenderer.getDarkenWorldAmount(0.0F));
        float r = color.x;
        float g = color.y;
        float b = color.z;
        if (!aboveHorizon) {
            return 0x0A000000 + (int) (r * 255.0F) * 65536 + (int) (g * 255.0F) * 256 + (int) (b * 255.0F);
        } else {
            int backgroundColor = 0xFF000000 + (int) (r * 255.0F) * 65536 + (int) (g * 255.0F) * 256 + (int) (b * 255.0F);
            if (!this.world.effects().isSunriseOrSunset(this.world.getTimeOfDay(0.0F))) {
                return backgroundColor;
            } else {
                int sunsetColor = this.world.effects().getSunriseOrSunsetColor(this.world.getTimeOfDay(0.0F));
                return ColorUtils.colorAdder(sunsetColor, backgroundColor);
            }
        }
    }

    public int[] getLightmapArray() {
        return this.lightmapColors;
    }

    public void drawMinimap(GuiGraphics drawContext) {
        this.write(drawContext, "Hey, I am important don't delete me!", 0, -20.0F, 16777215);
        int scScaleOrig = 1;

        while (VoxelConstants.getMinecraft().getWindow().getWidth() / (scScaleOrig + 1) >= 320 && VoxelConstants.getMinecraft().getWindow().getHeight() / (scScaleOrig + 1) >= 240) {
            ++scScaleOrig;
        }


        int scScale = scScaleOrig + (this.fullscreenMap ? 0 : this.options.sizeModifier);
        double scaledWidthD = (double) VoxelConstants.getMinecraft().getWindow().getWidth() / scScale;
        double scaledHeightD = (double) VoxelConstants.getMinecraft().getWindow().getHeight() / scScale;
        this.scWidth = Mth.ceil(scaledWidthD);
        this.scHeight = Mth.ceil(scaledHeightD);
        RenderSystem.backupProjectionMatrix();
        Matrix4f matrix4f = new Matrix4f().ortho(0.0f, (float) scaledWidthD, (float) scaledHeightD, 0.0f, 1000.0f, 3000.0f);
        RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.ORTHOGRAPHIC);
        Matrix4fStack modelViewMatrixStack = RenderSystem.getModelViewStack();
        modelViewMatrixStack.pushMatrix();
        modelViewMatrixStack.identity();
        modelViewMatrixStack.translate(0.0f, 0.0f, -2000.0f);
        // Lighting.setupFor3DItems();
        int mapX;
        if (this.options.mapCorner != 0 && this.options.mapCorner != 3) {
            mapX = this.scWidth - 37;
        } else {
            mapX = 37;
        }

        int mapY;
        if (this.options.mapCorner != 0 && this.options.mapCorner != 1) {
            mapY = this.scHeight - 37;
        } else {
            mapY = 37;
        }

        float statusIconOffset = 0.0F;
        if (VoxelMap.mapOptions.moveMapDownWhileStatusEffect) {
            if (this.options.mapCorner == 1 && !VoxelConstants.getPlayer().getActiveEffects().isEmpty()) {

                for (MobEffectInstance statusEffectInstance : VoxelConstants.getPlayer().getActiveEffects()) {
                    if (statusEffectInstance.showIcon()) {
                        if (statusEffectInstance.getEffect().value().isBeneficial()) {
                            statusIconOffset = Math.max(statusIconOffset, 24.0F);
                        } else {
                            statusIconOffset = 50.0F;
                        }
                    }
                }
                int scHeight = VoxelConstants.getMinecraft().getWindow().getGuiScaledHeight();
                float resFactor = (float) this.scHeight / scHeight;
                mapY += (int) (statusIconOffset * resFactor);
            }
        }
        Map.statusIconOffset = statusIconOffset;

        OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, 0);
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.options.displayMode != 0) {

            if (this.fullscreenMap) {
                if (this.options.displayMode >= 2) {
                    this.renderMapFull(drawContext, modelViewMatrixStack, this.scWidth, this.scHeight, scScale);
                }
            } else if (this.options.displayMode == 1 || this.options.displayMode == 3) {
                this.renderMap(drawContext, modelViewMatrixStack, mapX, mapY, scScale);
                this.drawDirections(drawContext, mapX, mapY, (float) (scScale / VoxelConstants.getMinecraft().getWindow().getGuiScale()));
            }

            OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
            if (this.fullscreenMap) {
                if (this.options.displayMode >= 2) {
                    this.drawArrow(modelViewMatrixStack, this.scWidth / 2, this.scHeight / 2, 20);
                }
            } else if (this.options.displayMode == 1 || this.options.displayMode == 3) {
                this.drawArrow(modelViewMatrixStack, mapX, mapY, 16);
            }
        }

        if (this.options.infoLabelMode != 0) {
            this.showCoords(drawContext, mapX, mapY, (float) (scScale / VoxelConstants.getMinecraft().getWindow().getGuiScale()));
        }

        OpenGL.glDepthMask(true);
        OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        modelViewMatrixStack.popMatrix();
        RenderSystem.restoreProjectionMatrix();
        OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        VoxelConstants.getMinecraft().font.getClass();
        MultiBufferSource.BufferSource vertexConsumerProvider = VoxelConstants.getMinecraft().renderBuffers().bufferSource();
        VoxelConstants.getMinecraft().font.drawInBatch(Component.literal("Hey, I am important don't delete me!"), 10000.0F, 100.0F, -1, true, matrix4f, vertexConsumerProvider, Font.DisplayMode.NORMAL, 0, 15728880);
        if (this.showWelcomeScreen) {
            OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
            this.drawWelcomeScreen(drawContext, VoxelConstants.getMinecraft().getWindow().getGuiScaledWidth(), VoxelConstants.getMinecraft().getWindow().getGuiScaledHeight());
        }

        OpenGL.glDepthMask(true);
        OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        // OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_NEAREST);
        // OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_NEAREST);
        OpenGL.glDisable(OpenGL.GL11_GL_BLEND);
    }

    private void checkForChanges() {
        boolean changed = this.colorManager.checkForChanges();

        if (this.options.isChanged()) {
            if (this.options.filtering) {
                this.mapImages = this.mapImagesFiltered;
            } else {
                this.mapImages = this.mapImagesUnfiltered;
            }

            changed = true;
            this.setZoomScale();
        }

        if (changed) {
            this.doFullRender = true;
            VoxelConstants.getVoxelMapInstance().getSettingsAndLightingChangeNotifier().notifyOfChanges();
        }

    }

    private void mapCalc(boolean full) {
        int currentX = GameVariableAccessShim.xCoord();
        int currentZ = GameVariableAccessShim.zCoord();
        int currentY = GameVariableAccessShim.yCoord();
        int offsetX = currentX - this.lastX;
        int offsetZ = currentZ - this.lastZ;
        int offsetY = currentY - this.lastY;
        int zoom = this.zoom;
        int multi = (int) Math.pow(2.0, zoom);
        ClientLevel world = this.world;
        boolean needHeightAndID;
        boolean needHeightMap = false;
        boolean needLight = false;
        boolean skyColorChanged = false;
        int skyColor = this.colorManager.getAirColor();
        if (this.lastSkyColor != skyColor) {
            skyColorChanged = true;
            this.lastSkyColor = skyColor;
        }

        if (this.options.lightmap) {
            int skylightMultiplier = 16;

            for (int t = 0; t < 16; ++t) {
                if (this.lastLightmapValues[t] != this.lightmapColors[t * skylightMultiplier]) {
                    needLight = true;
                    this.lastLightmapValues[t] = this.lightmapColors[t * skylightMultiplier];
                }
            }
        }

        if (offsetY != 0) {
            ++this.heightMapFudge;
        } else if (this.heightMapFudge != 0) {
            ++this.heightMapFudge;
        }

        if (full || Math.abs(offsetY) >= this.heightMapResetHeight || this.heightMapFudge > this.heightMapResetTime) {
            if (this.lastY != currentY) {
                needHeightMap = true;
            }

            this.lastY = currentY;
            this.heightMapFudge = 0;
        }

        if (Math.abs(offsetX) > 32 * multi || Math.abs(offsetZ) > 32 * multi) {
            full = true;
        }

        boolean nether = false;
        boolean caves = false;
        boolean netherPlayerInOpen;
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        blockPos.setXYZ(this.lastX, Math.max(Math.min(GameVariableAccessShim.yCoord(), world.getMaxY() - 1), world.getMinY()), this.lastZ);
        if (VoxelConstants.getPlayer().level().dimensionType().hasCeiling()) {

            netherPlayerInOpen = world.getChunk(blockPos).getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) <= currentY;
            nether = currentY < 126;
            if (this.options.cavesAllowed && this.options.showCaves && currentY >= 126 && !netherPlayerInOpen) {
                caves = true;
            }
        } else if (VoxelConstants.getClientWorld().effects().forceBrightLightmap() && !VoxelConstants.getClientWorld().dimensionType().hasSkyLight()) {
            boolean endPlayerInOpen = world.getChunk(blockPos).getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) <= currentY;
            if (this.options.cavesAllowed && this.options.showCaves && !endPlayerInOpen) {
                caves = true;
            }
        } else if (this.options.cavesAllowed && this.options.showCaves && world.getBrightness(LightLayer.SKY, blockPos) <= 0) {
            caves = true;
        }
        MutableBlockPosCache.release(blockPos);

        boolean beneathRendering = caves || nether;
        if (this.lastBeneathRendering != beneathRendering) {
            full = true;
        }

        this.lastBeneathRendering = beneathRendering;
        needHeightAndID = needHeightMap && (nether || caves);
        int color24;
        synchronized (this.coordinateLock) {
            if (!full) {
                this.mapImages[zoom].moveY(offsetZ);
                this.mapImages[zoom].moveX(offsetX);
            }

            this.lastX = currentX;
            this.lastZ = currentZ;
        }
        int startX = currentX - 16 * multi;
        int startZ = currentZ - 16 * multi;
        if (!full) {
            this.mapData[zoom].moveZ(offsetZ);
            this.mapData[zoom].moveX(offsetX);

            for (int imageY = offsetZ > 0 ? 32 * multi - 1 : -offsetZ - 1; imageY >= (offsetZ > 0 ? 32 * multi - offsetZ : 0); --imageY) {
                for (int imageX = 0; imageX < 32 * multi; ++imageX) {
                    color24 = this.getPixelColor(true, true, true, true, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                    this.mapImages[zoom].setRGB(imageX, imageY, color24);
                }
            }

            for (int imageY = 32 * multi - 1; imageY >= 0; --imageY) {
                for (int imageX = offsetX > 0 ? 32 * multi - offsetX : 0; imageX < (offsetX > 0 ? 32 * multi : -offsetX); ++imageX) {
                    color24 = this.getPixelColor(true, true, true, true, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                    this.mapImages[zoom].setRGB(imageX, imageY, color24);
                }
            }
        }

        if (full || this.options.heightmap && needHeightMap || needHeightAndID || this.options.lightmap && needLight || skyColorChanged) {
            for (int imageY = 32 * multi - 1; imageY >= 0; --imageY) {
                for (int imageX = 0; imageX < 32 * multi; ++imageX) {
                    color24 = this.getPixelColor(full, full || needHeightAndID, full, full || needLight || needHeightAndID, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                    this.mapImages[zoom].setRGB(imageX, imageY, color24);
                }
            }
        }

        if ((full || offsetX != 0 || offsetZ != 0 || !this.lastFullscreen) && this.fullscreenMap && this.options.biomeOverlay != 0) {
            this.mapData[zoom].segmentBiomes();
            this.mapData[zoom].findCenterOfSegments(!this.options.oldNorth);
        }

        this.lastFullscreen = this.fullscreenMap;
        if (full || offsetX != 0 || offsetZ != 0 || needHeightMap || needLight || skyColorChanged) {
            this.imageChanged = true;
        }

        if (needLight || skyColorChanged) {
            VoxelConstants.getVoxelMapInstance().getSettingsAndLightingChangeNotifier().notifyOfChanges();
        }

    }

    @Override
    public void handleChangeInWorld(int chunkX, int chunkZ) {
        try {
            this.chunkCache[this.zoom].registerChangeAt(chunkX, chunkZ);
        } catch (Exception e) {
            VoxelConstants.getLogger().warn(e);
        }
    }

    @Override
    public void processChunk(LevelChunk chunk) {
        this.rectangleCalc(chunk.getPos().x * 16, chunk.getPos().z * 16, chunk.getPos().x * 16 + 15, chunk.getPos().z * 16 + 15);
    }

    private void rectangleCalc(int left, int top, int right, int bottom) {
        boolean nether = false;
        boolean caves = false;
        boolean netherPlayerInOpen;
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        blockPos.setXYZ(this.lastX, Math.max(Math.min(GameVariableAccessShim.yCoord(), world.getMaxY()), world.getMinY()), this.lastZ);
        int currentY = GameVariableAccessShim.yCoord();
        if (VoxelConstants.getPlayer().level().dimensionType().hasCeiling()) {
            netherPlayerInOpen = this.world.getChunk(blockPos).getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) <= currentY;
            nether = currentY < 126;
            if (this.options.cavesAllowed && this.options.showCaves && currentY >= 126 && !netherPlayerInOpen) {
                caves = true;
            }
        } else if (VoxelConstants.getClientWorld().effects().forceBrightLightmap() && !VoxelConstants.getClientWorld().dimensionType().hasSkyLight()) {
            boolean endPlayerInOpen = this.world.getChunk(blockPos).getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) <= currentY;
            if (this.options.cavesAllowed && this.options.showCaves && !endPlayerInOpen) {
                caves = true;
            }
        } else if (this.options.cavesAllowed && this.options.showCaves && this.world.getBrightness(LightLayer.SKY, blockPos) <= 0) {
            caves = true;
        }
        MutableBlockPosCache.release(blockPos);

        int zoom = this.zoom;
        int startX = this.lastX;
        int startZ = this.lastZ;
        ClientLevel world = this.world;
        int multi = (int) Math.pow(2.0, zoom);
        startX -= 16 * multi;
        startZ -= 16 * multi;
        left = left - startX - 1;
        right = right - startX + 1;
        top = top - startZ - 1;
        bottom = bottom - startZ + 1;
        left = Math.max(0, left);
        right = Math.min(32 * multi - 1, right);
        top = Math.max(0, top);
        bottom = Math.min(32 * multi - 1, bottom);
        int color24;

        for (int imageY = bottom; imageY >= top; --imageY) {
            for (int imageX = left; imageX <= right; ++imageX) {
                color24 = this.getPixelColor(true, true, true, true, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY);
                this.mapImages[zoom].setRGB(imageX, imageY, color24);
            }
        }

        this.imageChanged = true;
    }

    private int getPixelColor(boolean needBiome, boolean needHeightAndID, boolean needTint, boolean needLight, boolean nether, boolean caves, ClientLevel world, int zoom, int multi, int startX, int startZ, int imageX, int imageY) {
        int surfaceHeight = Short.MIN_VALUE;
        int seafloorHeight = Short.MIN_VALUE;
        int transparentHeight = Short.MIN_VALUE;
        int foliageHeight = Short.MIN_VALUE;
        int surfaceColor;
        int seafloorColor = 0;
        int transparentColor = 0;
        int foliageColor = 0;
        this.surfaceBlockState = null;
        this.transparentBlockState = BlockRepository.air.defaultBlockState();
        BlockState foliageBlockState = BlockRepository.air.defaultBlockState();
        BlockState seafloorBlockState = BlockRepository.air.defaultBlockState();
        boolean surfaceBlockChangeForcedTint = false;
        boolean transparentBlockChangeForcedTint = false;
        boolean foliageBlockChangeForcedTint = false;
        boolean seafloorBlockChangeForcedTint = false;
        int surfaceBlockStateID;
        int transparentBlockStateID;
        int foliageBlockStateID;
        int seafloorBlockStateID;
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        MutableBlockPos tempBlockPos = MutableBlockPosCache.get();
        blockPos.withXYZ(startX + imageX, 64, startZ + imageY);
        int color24;
        Biome biome;
        if (needBiome) {
            if (world.hasChunkAt(blockPos)) {
                biome = world.getBiome(blockPos).value();
            } else {
                biome = null;
            }

            this.mapData[zoom].setBiome(imageX, imageY, biome);
        } else {
            biome = this.mapData[zoom].getBiome(imageX, imageY);
        }

        if (this.options.biomeOverlay == 1) {
            if (biome != null) {
                color24 = ARGB.toABGR(BiomeRepository.getBiomeColor(biome) | 0xFF000000);
            } else {
                color24 = 0;
            }

        } else {
            boolean solid = false;
            if (needHeightAndID) {
                if (!nether && !caves) {
                    LevelChunk chunk = world.getChunkAt(blockPos);
                    transparentHeight = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) + 1;
                    this.transparentBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, transparentHeight - 1, startZ + imageY));
                    FluidState fluidState = this.transparentBlockState.getFluidState();
                    if (fluidState != Fluids.EMPTY.defaultFluidState()) {
                        this.transparentBlockState = fluidState.createLegacyBlock();
                    }

                    surfaceHeight = transparentHeight;
                    this.surfaceBlockState = this.transparentBlockState;
                    VoxelShape voxelShape;
                    boolean hasOpacity = this.surfaceBlockState.getLightBlock() > 0;
                    if (!hasOpacity && this.surfaceBlockState.canOcclude() && this.surfaceBlockState.useShapeForLightOcclusion()) {
                        voxelShape = this.surfaceBlockState.getFaceOcclusionShape(Direction.DOWN);
                        hasOpacity = Shapes.faceShapeOccludes(voxelShape, Shapes.empty());
                        voxelShape = this.surfaceBlockState.getFaceOcclusionShape(Direction.UP);
                        hasOpacity = hasOpacity || Shapes.faceShapeOccludes(Shapes.empty(), voxelShape);
                    }

                    while (!hasOpacity && surfaceHeight > world.getMinY()) {
                        foliageBlockState = this.surfaceBlockState;
                        --surfaceHeight;
                        this.surfaceBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY));
                        fluidState = this.surfaceBlockState.getFluidState();
                        if (fluidState != Fluids.EMPTY.defaultFluidState()) {
                            this.surfaceBlockState = fluidState.createLegacyBlock();
                        }

                        hasOpacity = this.surfaceBlockState.getLightBlock() > 0;
                        if (!hasOpacity && this.surfaceBlockState.canOcclude() && this.surfaceBlockState.useShapeForLightOcclusion()) {
                            voxelShape = this.surfaceBlockState.getFaceOcclusionShape(Direction.DOWN);
                            hasOpacity = Shapes.faceShapeOccludes(voxelShape, Shapes.empty());
                            voxelShape = this.surfaceBlockState.getFaceOcclusionShape(Direction.UP);
                            hasOpacity = hasOpacity || Shapes.faceShapeOccludes(Shapes.empty(), voxelShape);
                        }
                    }

                    if (surfaceHeight == transparentHeight) {
                        transparentHeight = Short.MIN_VALUE;
                        this.transparentBlockState = BlockRepository.air.defaultBlockState();
                        foliageBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight, startZ + imageY));
                    }

                    if (foliageBlockState.getBlock() == Blocks.SNOW) {
                        this.surfaceBlockState = foliageBlockState;
                        foliageBlockState = BlockRepository.air.defaultBlockState();
                    }

                    if (foliageBlockState == this.transparentBlockState) {
                        foliageBlockState = BlockRepository.air.defaultBlockState();
                    }

                    if (foliageBlockState != null && !(foliageBlockState.getBlock() instanceof AirBlock)) {
                        foliageHeight = surfaceHeight + 1;
                    } else {
                        foliageHeight = Short.MIN_VALUE;
                    }

                    Block material = this.surfaceBlockState.getBlock();
                    if (material == Blocks.WATER || material == Blocks.ICE) {
                        seafloorHeight = surfaceHeight;

                        for (seafloorBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY)); seafloorBlockState.getLightBlock() < 5 && !(seafloorBlockState.getBlock() instanceof LeavesBlock)
                                && seafloorHeight > world.getMinY() + 1; seafloorBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, seafloorHeight - 1, startZ + imageY))) {
                            material = seafloorBlockState.getBlock();
                            if (transparentHeight == Short.MIN_VALUE && material != Blocks.ICE && material != Blocks.WATER && seafloorBlockState.blocksMotion()) {
                                transparentHeight = seafloorHeight;
                                this.transparentBlockState = seafloorBlockState;
                            }

                            if (foliageHeight == Short.MIN_VALUE && seafloorHeight != transparentHeight && this.transparentBlockState != seafloorBlockState && material != Blocks.ICE && material != Blocks.WATER && !(material instanceof AirBlock) && material != Blocks.BUBBLE_COLUMN) {
                                foliageHeight = seafloorHeight;
                                foliageBlockState = seafloorBlockState;
                            }

                            --seafloorHeight;
                        }

                        if (seafloorBlockState.getBlock() == Blocks.WATER) {
                            seafloorBlockState = BlockRepository.air.defaultBlockState();
                        }
                    }
                } else {
                    surfaceHeight = this.getCaveHeight(startX + imageX, startZ + imageY);
                    this.surfaceBlockState = world.getBlockState(blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY));
                    surfaceBlockStateID = BlockRepository.getStateId(this.surfaceBlockState);
                    foliageHeight = surfaceHeight + 1;
                    blockPos.setXYZ(startX + imageX, foliageHeight - 1, startZ + imageY);
                    foliageBlockState = world.getBlockState(blockPos);
                    Block material = foliageBlockState.getBlock();
                    if (material != Blocks.SNOW && !(material instanceof AirBlock) && material != Blocks.LAVA && material != Blocks.WATER) {
                        foliageBlockStateID = BlockRepository.getStateId(foliageBlockState);
                    } else {
                        foliageHeight = Short.MIN_VALUE;
                    }
                }

                surfaceBlockStateID = BlockRepository.getStateId(this.surfaceBlockState);
                if (this.options.biomes && this.surfaceBlockState != this.mapData[zoom].getBlockstate(imageX, imageY)) {
                    surfaceBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setHeight(imageX, imageY, surfaceHeight);
                this.mapData[zoom].setBlockstateID(imageX, imageY, surfaceBlockStateID);
                if (this.options.biomes && this.transparentBlockState != this.mapData[zoom].getTransparentBlockstate(imageX, imageY)) {
                    transparentBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setTransparentHeight(imageX, imageY, transparentHeight);
                transparentBlockStateID = BlockRepository.getStateId(this.transparentBlockState);
                this.mapData[zoom].setTransparentBlockstateID(imageX, imageY, transparentBlockStateID);
                if (this.options.biomes && foliageBlockState != this.mapData[zoom].getFoliageBlockstate(imageX, imageY)) {
                    foliageBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setFoliageHeight(imageX, imageY, foliageHeight);
                foliageBlockStateID = BlockRepository.getStateId(foliageBlockState);
                this.mapData[zoom].setFoliageBlockstateID(imageX, imageY, foliageBlockStateID);
                if (this.options.biomes && seafloorBlockState != this.mapData[zoom].getOceanFloorBlockstate(imageX, imageY)) {
                    seafloorBlockChangeForcedTint = true;
                }

                this.mapData[zoom].setOceanFloorHeight(imageX, imageY, seafloorHeight);
                seafloorBlockStateID = BlockRepository.getStateId(seafloorBlockState);
                this.mapData[zoom].setOceanFloorBlockstateID(imageX, imageY, seafloorBlockStateID);
            } else {
                surfaceHeight = this.mapData[zoom].getHeight(imageX, imageY);
                surfaceBlockStateID = this.mapData[zoom].getBlockstateID(imageX, imageY);
                this.surfaceBlockState = BlockRepository.getStateById(surfaceBlockStateID);
                transparentHeight = this.mapData[zoom].getTransparentHeight(imageX, imageY);
                transparentBlockStateID = this.mapData[zoom].getTransparentBlockstateID(imageX, imageY);
                this.transparentBlockState = BlockRepository.getStateById(transparentBlockStateID);
                foliageHeight = this.mapData[zoom].getFoliageHeight(imageX, imageY);
                foliageBlockStateID = this.mapData[zoom].getFoliageBlockstateID(imageX, imageY);
                foliageBlockState = BlockRepository.getStateById(foliageBlockStateID);
                seafloorHeight = this.mapData[zoom].getOceanFloorHeight(imageX, imageY);
                seafloorBlockStateID = this.mapData[zoom].getOceanFloorBlockstateID(imageX, imageY);
                seafloorBlockState = BlockRepository.getStateById(seafloorBlockStateID);
            }

            if (surfaceHeight == Short.MIN_VALUE) {
                surfaceHeight = this.lastY + 1;
                solid = true;
            }

            if (this.surfaceBlockState.getBlock() == Blocks.LAVA) {
                solid = false;
            }

            if (this.options.biomes) {
                surfaceColor = this.colorManager.getBlockColor(blockPos, surfaceBlockStateID, biome);
                int tint;
                if (!needTint && !surfaceBlockChangeForcedTint) {
                    tint = this.mapData[zoom].getBiomeTint(imageX, imageY);
                } else {
                    tint = this.colorManager.getBiomeTint(this.mapData[zoom], world, this.surfaceBlockState, surfaceBlockStateID, blockPos.withXYZ(startX + imageX, surfaceHeight - 1, startZ + imageY), tempBlockPos, startX, startZ);
                    this.mapData[zoom].setBiomeTint(imageX, imageY, tint);
                }

                if (tint != -1) {
                    surfaceColor = ColorUtils.colorMultiplier(surfaceColor, tint);
                }
            } else {
                surfaceColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, surfaceBlockStateID);
            }

            surfaceColor = this.applyHeight(surfaceColor, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY, surfaceHeight, solid, 1);
            int light;
            if (needLight) {
                light = this.getLight(surfaceColor, this.surfaceBlockState, world, startX + imageX, startZ + imageY, surfaceHeight, solid);
                this.mapData[zoom].setLight(imageX, imageY, light);
            } else {
                light = this.mapData[zoom].getLight(imageX, imageY);
            }

            if (light == 0) {
                surfaceColor = 0;
            } else if (light != 255) {
                surfaceColor = ColorUtils.colorMultiplier(surfaceColor, light);
            }

            if (this.options.waterTransparency && seafloorHeight != Short.MIN_VALUE) {
                if (!this.options.biomes) {
                    seafloorColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, seafloorBlockStateID);
                } else {
                    seafloorColor = this.colorManager.getBlockColor(blockPos, seafloorBlockStateID, biome);
                    int tint;
                    if (!needTint && !seafloorBlockChangeForcedTint) {
                        tint = this.mapData[zoom].getOceanFloorBiomeTint(imageX, imageY);
                    } else {
                        tint = this.colorManager.getBiomeTint(this.mapData[zoom], world, seafloorBlockState, seafloorBlockStateID, blockPos.withXYZ(startX + imageX, seafloorHeight - 1, startZ + imageY), tempBlockPos, startX, startZ);
                        this.mapData[zoom].setOceanFloorBiomeTint(imageX, imageY, tint);
                    }

                    if (tint != -1) {
                        seafloorColor = ColorUtils.colorMultiplier(seafloorColor, tint);
                    }
                }

                seafloorColor = this.applyHeight(seafloorColor, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY, seafloorHeight, solid, 0);
                int seafloorLight;
                if (needLight) {
                    seafloorLight = this.getLight(seafloorColor, seafloorBlockState, world, startX + imageX, startZ + imageY, seafloorHeight, solid);
                    blockPos.setXYZ(startX + imageX, seafloorHeight, startZ + imageY);
                    BlockState blockStateAbove = world.getBlockState(blockPos);
                    Block materialAbove = blockStateAbove.getBlock();
                    if (this.options.lightmap && materialAbove == Blocks.ICE) {
                        int multiplier = VoxelConstants.getMinecraft().options.ambientOcclusion().get() ? 200 : 120;
                        seafloorLight = ColorUtils.colorMultiplier(seafloorLight, 0xFF000000 | multiplier << 16 | multiplier << 8 | multiplier);
                    }

                    this.mapData[zoom].setOceanFloorLight(imageX, imageY, seafloorLight);
                } else {
                    seafloorLight = this.mapData[zoom].getOceanFloorLight(imageX, imageY);
                }

                if (seafloorLight == 0) {
                    seafloorColor = 0;
                } else if (seafloorLight != 255) {
                    seafloorColor = ColorUtils.colorMultiplier(seafloorColor, seafloorLight);
                }
            }

            if (this.options.blockTransparency) {
                if (transparentHeight != Short.MIN_VALUE && this.transparentBlockState != null && this.transparentBlockState != BlockRepository.air.defaultBlockState()) {
                    if (this.options.biomes) {
                        transparentColor = this.colorManager.getBlockColor(blockPos, transparentBlockStateID, biome);
                        int tint;
                        if (!needTint && !transparentBlockChangeForcedTint) {
                            tint = this.mapData[zoom].getTransparentBiomeTint(imageX, imageY);
                        } else {
                            tint = this.colorManager.getBiomeTint(this.mapData[zoom], world, this.transparentBlockState, transparentBlockStateID, blockPos.withXYZ(startX + imageX, transparentHeight - 1, startZ + imageY), tempBlockPos, startX, startZ);
                            this.mapData[zoom].setTransparentBiomeTint(imageX, imageY, tint);
                        }

                        if (tint != -1) {
                            transparentColor = ColorUtils.colorMultiplier(transparentColor, tint);
                        }
                    } else {
                        transparentColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, transparentBlockStateID);
                    }

                    transparentColor = this.applyHeight(transparentColor, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY, transparentHeight, solid, 3);
                    int transparentLight;
                    if (needLight) {
                        transparentLight = this.getLight(transparentColor, this.transparentBlockState, world, startX + imageX, startZ + imageY, transparentHeight, solid);
                        this.mapData[zoom].setTransparentLight(imageX, imageY, transparentLight);
                    } else {
                        transparentLight = this.mapData[zoom].getTransparentLight(imageX, imageY);
                    }

                    if (transparentLight == 0) {
                        transparentColor = 0;
                    } else if (transparentLight != 255) {
                        transparentColor = ColorUtils.colorMultiplier(transparentColor, transparentLight);
                    }
                }

                if (foliageHeight != Short.MIN_VALUE && foliageBlockState != null && foliageBlockState != BlockRepository.air.defaultBlockState()) {
                    if (!this.options.biomes) {
                        foliageColor = this.colorManager.getBlockColorWithDefaultTint(blockPos, foliageBlockStateID);
                    } else {
                        foliageColor = this.colorManager.getBlockColor(blockPos, foliageBlockStateID, biome);
                        int tint;
                        if (!needTint && !foliageBlockChangeForcedTint) {
                            tint = this.mapData[zoom].getFoliageBiomeTint(imageX, imageY);
                        } else {
                            tint = this.colorManager.getBiomeTint(this.mapData[zoom], world, foliageBlockState, foliageBlockStateID, blockPos.withXYZ(startX + imageX, foliageHeight - 1, startZ + imageY), tempBlockPos, startX, startZ);
                            this.mapData[zoom].setFoliageBiomeTint(imageX, imageY, tint);
                        }

                        if (tint != -1) {
                            foliageColor = ColorUtils.colorMultiplier(foliageColor, tint);
                        }
                    }

                    foliageColor = this.applyHeight(foliageColor, nether, caves, world, zoom, multi, startX, startZ, imageX, imageY, foliageHeight, solid, 2);
                    int foliageLight;
                    if (needLight) {
                        foliageLight = this.getLight(foliageColor, foliageBlockState, world, startX + imageX, startZ + imageY, foliageHeight, solid);
                        this.mapData[zoom].setFoliageLight(imageX, imageY, foliageLight);
                    } else {
                        foliageLight = this.mapData[zoom].getFoliageLight(imageX, imageY);
                    }

                    if (foliageLight == 0) {
                        foliageColor = 0;
                    } else if (foliageLight != 255) {
                        foliageColor = ColorUtils.colorMultiplier(foliageColor, foliageLight);
                    }
                }
            }

            if (seafloorColor != 0 && seafloorHeight > Short.MIN_VALUE) {
                color24 = seafloorColor;
                if (foliageColor != 0 && foliageHeight <= surfaceHeight) {
                    color24 = ColorUtils.colorAdder(foliageColor, seafloorColor);
                }

                if (transparentColor != 0 && transparentHeight <= surfaceHeight) {
                    color24 = ColorUtils.colorAdder(transparentColor, color24);
                }

                color24 = ColorUtils.colorAdder(surfaceColor, color24);
            } else {
                color24 = surfaceColor;
            }

            if (foliageColor != 0 && foliageHeight > surfaceHeight) {
                color24 = ColorUtils.colorAdder(foliageColor, color24);
            }

            if (transparentColor != 0 && transparentHeight > surfaceHeight) {
                color24 = ColorUtils.colorAdder(transparentColor, color24);
            }

            if (this.options.biomeOverlay == 2) {
                int bc = 0;
                if (biome != null) {
                    bc = ARGB.toABGR(BiomeRepository.getBiomeColor(biome));
                }

                bc = 2130706432 | bc;
                color24 = ColorUtils.colorAdder(bc, color24);
            }

        }
        MutableBlockPosCache.release(blockPos);
        MutableBlockPosCache.release(tempBlockPos);
        return MapUtils.doSlimeAndGrid(color24, world, startX + imageX, startZ + imageY);
    }

    private int getBlockHeight(boolean nether, boolean caves, Level world, int x, int z) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        int playerHeight = GameVariableAccessShim.yCoord();
        blockPos.setXYZ(x, playerHeight, z);
        LevelChunk chunk = (LevelChunk) world.getChunk(blockPos);
        int height = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, blockPos.getX() & 15, blockPos.getZ() & 15) + 1;
        BlockState blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z));
        FluidState fluidState = this.transparentBlockState.getFluidState();
        if (fluidState != Fluids.EMPTY.defaultFluidState()) {
            blockState = fluidState.createLegacyBlock();
        }

        while (blockState.getLightBlock() == 0 && height > world.getMinY()) {
            --height;
            blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z));
            fluidState = this.surfaceBlockState.getFluidState();
            if (fluidState != Fluids.EMPTY.defaultFluidState()) {
                blockState = fluidState.createLegacyBlock();
            }
        }
        MutableBlockPosCache.release(blockPos);
        return (nether || caves) && height > playerHeight ? this.getCaveHeight(x, z) : height;
    }

    private int getCaveHeight(int x, int z) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        int y = this.lastY;
        blockPos.setXYZ(x, y, z);
        BlockState blockState = this.world.getBlockState(blockPos);
        if (blockState.getLightBlock() == 0 && blockState.getBlock() != Blocks.LAVA) {
            while (y > world.getMinY()) {
                --y;
                blockPos.setXYZ(x, y, z);
                blockState = this.world.getBlockState(blockPos);
                if (blockState.getLightBlock() > 0 || blockState.getBlock() == Blocks.LAVA) {
                    MutableBlockPosCache.release(blockPos);
                    return y + 1;
                }
            }
            MutableBlockPosCache.release(blockPos);
            return y;
        } else {
            while (y <= this.lastY + 10 && y < world.getMaxY()) {
                ++y;
                blockPos.setXYZ(x, y, z);
                blockState = this.world.getBlockState(blockPos);
                if (blockState.getLightBlock() == 0 && blockState.getBlock() != Blocks.LAVA) {
                    MutableBlockPosCache.release(blockPos);
                    return y;
                }
            }
            MutableBlockPosCache.release(blockPos);
            return this.world.getMinY() - 1;
        }
    }

    private int getSeafloorHeight(Level world, int x, int z, int height) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        for (BlockState blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z)); blockState.getLightBlock() < 5 && !(blockState.getBlock() instanceof LeavesBlock) && height > world.getMinY() + 1; blockState = world.getBlockState(blockPos.withXYZ(x, height - 1, z))) {
            --height;
        }
        MutableBlockPosCache.release(blockPos);
        return height;
    }

    private int getTransparentHeight(boolean nether, boolean caves, Level world, int x, int z, int height) {
        MutableBlockPos blockPos = MutableBlockPosCache.get();
        int transHeight;
        if (!caves && !nether) {
            transHeight = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos.withXYZ(x, height, z)).getY();
            if (transHeight <= height) {
                transHeight = Short.MIN_VALUE;
            }
        } else {
            transHeight = Short.MIN_VALUE;
        }

        BlockState blockState = world.getBlockState(blockPos.withXYZ(x, transHeight - 1, z));
        Block material = blockState.getBlock();
        if (transHeight == height + 1 && material == Blocks.SNOW) {
            transHeight = Short.MIN_VALUE;
        }

        if (material == Blocks.BARRIER) {
            ++transHeight;
            blockState = world.getBlockState(blockPos.withXYZ(x, transHeight - 1, z));
            material = blockState.getBlock();
            if (material instanceof AirBlock) {
                transHeight = Short.MIN_VALUE;
            }
        }
        MutableBlockPosCache.release(blockPos);
        return transHeight;
    }

    private int applyHeight(int color24, boolean nether, boolean caves, Level world, int zoom, int multi, int startX, int startZ, int imageX, int imageY, int height, boolean solid, int layer) {
        if (color24 != this.colorManager.getAirColor() && color24 != 0 && (this.options.heightmap || this.options.slopemap) && !solid) {
            int heightComp = -1;
            int diff;
            double sc = 0.0;
            if (!this.options.slopemap) {
                diff = height - this.lastY;
                sc = Math.log10(Math.abs(diff) / 8.0 + 1.0) / 1.8;
                if (diff < 0) {
                    sc = 0.0 - sc;
                }
            } else {
                if (imageX > 0 && imageY < 32 * multi - 1) {
                    if (layer == 0) {
                        heightComp = this.mapData[zoom].getOceanFloorHeight(imageX - 1, imageY + 1);
                    }

                    if (layer == 1) {
                        heightComp = this.mapData[zoom].getHeight(imageX - 1, imageY + 1);
                    }

                    if (layer == 2) {
                        heightComp = height;
                    }

                    if (layer == 3) {
                        heightComp = this.mapData[zoom].getTransparentHeight(imageX - 1, imageY + 1);
                        if (heightComp == Short.MIN_VALUE) {
                            Block block = BlockRepository.getStateById(this.mapData[zoom].getTransparentBlockstateID(imageX, imageY)).getBlock();
                            if (block == Blocks.GLASS || block instanceof StainedGlassBlock) {
                                heightComp = this.mapData[zoom].getHeight(imageX - 1, imageY + 1);
                            }
                        }
                    }
                } else {
                    if (layer == 0) {
                        int baseHeight = this.getBlockHeight(nether, caves, world, startX + imageX - 1, startZ + imageY + 1);
                        heightComp = this.getSeafloorHeight(world, startX + imageX - 1, startZ + imageY + 1, baseHeight);
                    }

                    if (layer == 1) {
                        heightComp = this.getBlockHeight(nether, caves, world, startX + imageX - 1, startZ + imageY + 1);
                    }

                    if (layer == 2) {
                        heightComp = height;
                    }

                    if (layer == 3) {
                        int baseHeight = this.getBlockHeight(nether, caves, world, startX + imageX - 1, startZ + imageY + 1);
                        heightComp = this.getTransparentHeight(nether, caves, world, startX + imageX - 1, startZ + imageY + 1, baseHeight);
                        if (heightComp == Short.MIN_VALUE) {
                            MutableBlockPos blockPos = MutableBlockPosCache.get();
                            BlockState blockState = world.getBlockState(blockPos.withXYZ(startX + imageX, height - 1, startZ + imageY));
                            MutableBlockPosCache.release(blockPos);
                            Block block = blockState.getBlock();
                            if (block == Blocks.GLASS || block instanceof StainedGlassBlock) {
                                heightComp = baseHeight;
                            }
                        }
                    }
                }

                if (heightComp == Short.MIN_VALUE) {
                    heightComp = height;
                }

                diff = heightComp - height;
                if (diff != 0) {
                    sc = diff > 0 ? 1.0 : -1.0;
                    sc /= 8.0;
                }

                if (this.options.heightmap) {
                    diff = height - this.lastY;
                    double heightsc = Math.log10(Math.abs(diff) / 8.0 + 1.0) / 3.0;
                    sc = diff > 0 ? sc + heightsc : sc - heightsc;
                }
            }

            int alpha = color24 >> 24 & 0xFF;
            int r = color24 >> 16 & 0xFF;
            int g = color24 >> 8 & 0xFF;
            int b = color24 & 0xFF;
            if (sc > 0.0) {
                r += (int) (sc * (255 - r));
                g += (int) (sc * (255 - g));
                b += (int) (sc * (255 - b));
            } else if (sc < 0.0) {
                sc = Math.abs(sc);
                r -= (int) (sc * r);
                g -= (int) (sc * g);
                b -= (int) (sc * b);
            }

            color24 = alpha * 16777216 + r * 65536 + g * 256 + b;
        }

        return color24;
    }

    private int getLight(int color24, BlockState blockState, Level world, int x, int z, int height, boolean solid) {
        int combinedLight = 0xffffffff;
        if (solid) {
            combinedLight = 0;
        } else if (color24 != this.colorManager.getAirColor() && color24 != 0 && this.options.lightmap) {
            MutableBlockPos blockPos = MutableBlockPosCache.get();
            blockPos.setXYZ(x, Math.max(Math.min(height, world.getMaxY()), world.getMinY()), z);
            int blockLight = world.getBrightness(LightLayer.BLOCK, blockPos);
            int skyLight = world.getBrightness(LightLayer.SKY, blockPos);
            if (blockState.getBlock() == Blocks.LAVA || blockState.getBlock() == Blocks.MAGMA_BLOCK) {
                blockLight = 14;
            }
            MutableBlockPosCache.release(blockPos);
            combinedLight = this.lightmapColors[blockLight + skyLight * 16];
        }

        return ARGB.toABGR(combinedLight);
    }

    private void renderMap(GuiGraphics drawContext, Matrix4fStack matrixStack, int x, int y, int scScale) {
        float scale = 1.0F;
        if (this.options.shape == 1 && this.options.rotates) {
            scale = 1.4142F;
        }

        OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, 0);
        Matrix4f minimapProjectionMatrix = RenderSystem.getProjectionMatrix();
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        Matrix4f matrix4f = new Matrix4f().ortho(0.0F, 512.0F, 512.0F, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.ORTHOGRAPHIC);
        OpenGL.Utils.bindFramebuffer();
        OpenGL.glViewport(0, 0, 512, 512);
        matrixStack.pushMatrix();
        matrixStack.identity();
        matrixStack.translate(0.0f, 0.0f, -2000.0f);
        OpenGL.glDepthMask(false);
        OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        OpenGL.glClear(16384);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, 0);
        OpenGL.Utils.img2(this.options.shape == 1 ? this.squareStencil : this.circleStencil);
        OpenGL.Utils.drawPre();
        OpenGL.Utils.ldrawthree(256.0F - 256.0F / scale, 256.0F + 256.0F / scale, 1.0, 0.0F, 0.0F);
        OpenGL.Utils.ldrawthree((256.0F + 256.0F / scale), 256.0F + 256.0F / scale, 1.0, 1.0F, 0.0F);
        OpenGL.Utils.ldrawthree(256.0F + 256.0F / scale, 256.0F - 256.0F / scale, 1.0, 1.0F, 1.0F);
        OpenGL.Utils.ldrawthree(256.0F - 256.0F / scale, 256.0F - 256.0F / scale, 1.0, 0.0F, 1.0F);
        OpenGL.Utils.drawPost();
        OpenGL.glBlendFuncSeparate(1, 0, 774, 0);
        synchronized (this.coordinateLock) {
            if (this.imageChanged) {
                this.imageChanged = false;
                this.mapImages[this.zoom].write();
                this.lastImageX = this.lastX;
                this.lastImageZ = this.lastZ;
            }
        }

        float multi = (float) (1.0 / this.zoomScale);
        float percentX = (float) (GameVariableAccessShim.xCoordDouble() - this.lastImageX) * multi;
        float percentY = (float) (GameVariableAccessShim.zCoordDouble() - this.lastImageZ) * multi;
        OpenGL.Utils.disp2(this.mapImages[this.zoom].getIndex());
        // OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR_MIPMAP_LINEAR);
        // OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
        matrixStack.pushMatrix();
        matrixStack.translate(256.0f, 256.0f, 0.0f);
        if (!this.options.rotates) {
            matrixStack.rotate(Axis.ZP.rotationDegrees((-this.northRotate)));
        } else {
            matrixStack.rotate(Axis.ZP.rotationDegrees(this.direction));
        }
        matrixStack.translate(-256.0f, -256.0f, 0.0f);
        matrixStack.translate(-percentX * 512.0F / 64.0F, percentY * 512.0F / 64.0F, 0.0f);
        OpenGL.Utils.drawPre();
        OpenGL.Utils.ldrawthree(0.0, 512.0, 1.0, 0.0F, 0.0F);
        OpenGL.Utils.ldrawthree(512.0, 512.0, 1.0, 1.0F, 0.0F);
        OpenGL.Utils.ldrawthree(512.0, 0.0, 1.0, 1.0F, 1.0F);
        OpenGL.Utils.ldrawthree(0.0, 0.0, 1.0, 0.0F, 1.0F);
        OpenGL.Utils.drawPost();
        matrixStack.popMatrix();
        OpenGL.glDepthMask(true);
        OpenGL.glEnable(GL11C.GL_DEPTH_TEST);
        OpenGL.Utils.unbindFramebuffer();
        OpenGL.glViewport(0, 0, VoxelConstants.getMinecraft().getWindow().getWidth(), VoxelConstants.getMinecraft().getWindow().getHeight());
        matrixStack.popMatrix();
        RenderSystem.setProjectionMatrix(minimapProjectionMatrix, ProjectionType.ORTHOGRAPHIC);
        matrixStack.pushMatrix();
        OpenGL.glBlendFunc(GL11C.GL_SRC_ALPHA, GL11C.GL_ZERO);
        OpenGL.Utils.disp2(OpenGL.Utils.fboTextureId);

        double guiScale = (double) VoxelConstants.getMinecraft().getWindow().getWidth() / this.scWidth;
        minTablistOffset = guiScale * 63;
        OpenGL.glEnable(GL11C.GL_SCISSOR_TEST);
        OpenGL.glScissor((int) (guiScale * (x - 32)), (int) (guiScale * ((this.scHeight - y) - 32.0)), (int) (guiScale * 64.0), (int) (guiScale * 63.0));
        OpenGL.Utils.drawPre();
        OpenGL.Utils.setMapWithScale(x, y, scale);
        OpenGL.Utils.drawPost();
        OpenGL.glDisable(GL11C.GL_SCISSOR_TEST);
        matrixStack.popMatrix();

        OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
        if (VoxelConstants.getVoxelMapInstance().getRadar() != null) {
            this.layoutVariables.updateVars(scScale, x, y, this.zoomScale, this.zoomScaleAdjusted, this.options.rotates, this.fullscreenMap);
            VoxelConstants.getVoxelMapInstance().getRadar().onTickInGame(drawContext, matrixStack, this.layoutVariables, (float) (scScale / VoxelConstants.getMinecraft().getWindow().getGuiScale()));
        }
        OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);

        OpenGL.glBlendFunc(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA);
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.drawMapFrame(x, y, this.options.shape, 128);

        double lastXDouble = GameVariableAccessShim.xCoordDouble();
        double lastZDouble = GameVariableAccessShim.zCoordDouble();
        TextureAtlas textureAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
        OpenGL.Utils.disp2(textureAtlas.getId());
        OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
        OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
        if (VoxelMap.mapOptions.waypointsAllowed) {
            Waypoint highlightedPoint = this.waypointManager.getHighlightedWaypoint();

            for (Waypoint pt : this.waypointManager.getWaypoints()) {
                if (pt.isActive() || pt == highlightedPoint) {
                    double distanceSq = pt.getDistanceSqToEntity(VoxelConstants.getMinecraft().getCameraEntity());
                    if (distanceSq < (this.options.maxWaypointDisplayDistance * this.options.maxWaypointDisplayDistance) || this.options.maxWaypointDisplayDistance < 0 || pt == highlightedPoint) {
                        this.drawWaypoint(matrixStack, pt, textureAtlas, null, null, null, null, x, y, lastXDouble, lastZDouble, this.zoomScaleAdjusted, this.options.rotates);
                    }
                }
            }

            if (highlightedPoint != null) {
                this.drawWaypoint(matrixStack, highlightedPoint, textureAtlas, textureAtlas.getAtlasSprite("voxelmap:images/waypoints/target.png"), 1.0F, 0.0F, 0.0F, x, y, lastXDouble, lastZDouble, this.zoomScaleAdjusted, this.options.rotates);
            }
        }
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawWaypoint(Matrix4fStack matrixStack, Waypoint pt, TextureAtlas textureAtlas, Sprite icon, Float r, Float g, Float b, int x, int y, double lastXDouble, double lastZDouble, double adjustedZoom, boolean rotating) {
        boolean uprightIcon = icon != null;
        if (r == null) {
            r = pt.red;
        }

        if (g == null) {
            g = pt.green;
        }

        if (b == null) {
            b = pt.blue;
        }
        float iconScale = this.fullscreenMap ? 0.4f : 1.0f;
        double wayX = lastXDouble - pt.getX() - 0.5;
        double wayY = lastZDouble - pt.getZ() - 0.5;
        float locate = (float) Math.toDegrees(Math.atan2(wayX, wayY));
        float hypot = (float) Math.sqrt(wayX * wayX + wayY * wayY);
        boolean far;
        if (rotating) {
            locate += this.direction;
        } else {
            locate -= this.northRotate;
        }

        hypot /= adjustedZoom;
        if (this.options.shape == 1 || this.fullscreenMap) {
            double radLocate = Math.toRadians(locate);
            double dispX = hypot * Math.cos(radLocate);
            double dispY = hypot * Math.sin(radLocate);
            far = Math.abs(dispX) > 28.5 || Math.abs(dispY) > 28.5;
            if (far) {
                hypot = (float) (hypot / Math.max(Math.abs(dispX), Math.abs(dispY)) * 30.0);
            }
        } else {
            far = hypot >= 31.0f;
            if (far) {
                hypot = 34.0f;
            }
        }

        boolean target = false;
        if (far) {
            try {
                if (icon == null) {
                    icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/marker" + pt.imageSuffix + ".png");

                    if (icon == textureAtlas.getMissingImage()) {
                        icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/marker.png");
                    }
                } else {
                    target = true;
                }

                matrixStack.pushMatrix();
                OpenGL.glColor4f(r, g, b, !pt.enabled && !target ? 0.3F : 1.0F);
                matrixStack.translate(x, y, 0.0f);
                matrixStack.rotate(Axis.ZP.rotationDegrees(-locate));
                if (uprightIcon) {
                    matrixStack.translate(0.0f, -hypot, 0.0f);
                    matrixStack.rotate(Axis.ZP.rotationDegrees(locate));
                    matrixStack.translate(-x, -y, 0.0f);
                } else {
                    matrixStack.translate(-x, -y, 0.0f);
                    matrixStack.translate(0.0f, -hypot, 0.0f);
                }

                // OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
                // OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
                OpenGL.Utils.drawPre();
                OpenGL.Utils.setMap(icon, x, y, 16.0F * iconScale);
                OpenGL.Utils.drawPost();
            } catch (Exception var40) {
                this.message = "Error: marker overlay not found!";
            } finally {
                matrixStack.popMatrix();
            }
        } else {
            try {
                if (icon == null) {
                    icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + pt.imageSuffix + ".png");

                    if (icon == textureAtlas.getMissingImage()) {
                        icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
                    }
                } else {
                    target = true;
                }

                matrixStack.pushMatrix();
                OpenGL.glColor4f(r, g, b, !pt.enabled && !target ? 0.3F : 1.0F);
                matrixStack.rotate(Axis.ZP.rotationDegrees(-locate));
                matrixStack.translate(0.0f, -hypot, 0.0f);
                matrixStack.rotate(Axis.ZP.rotationDegrees(-(-locate)));
                // OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
                // OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
                OpenGL.Utils.drawPre();
                OpenGL.Utils.setMap(icon, x, y, 16.0F * iconScale);
                OpenGL.Utils.drawPost();
            } catch (Exception var42) {
                this.message = "Error: waypoint overlay not found!";
            } finally {
                matrixStack.popMatrix();
            }
        }

    }

    private void drawArrow(Matrix4fStack matrixStack, int x, int y, int size) {
        try {
            RenderSystem.setShader(CoreShaders.POSITION_TEX);
            matrixStack.pushMatrix();
            OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
            // OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
            // OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
            OpenGL.Utils.img2(this.directionArrow);
            matrixStack.translate(x, y, 0.0f);
            matrixStack.rotate(Axis.ZP.rotationDegrees(this.options.rotates && !this.fullscreenMap ? 0.0F : this.direction + this.northRotate));
            matrixStack.translate(-x, -y, 0.0f);
            OpenGL.Utils.drawPre();
            OpenGL.Utils.setMap(x, y, size);
            OpenGL.Utils.drawPost();
        } catch (Exception var8) {
            this.message = "Error: minimap arrow not found!";
        } finally {
            matrixStack.popMatrix();
        }

    }

    private void renderMapFull(GuiGraphics drawContext, Matrix4fStack modelStack, int scWidth, int scHeight, int scScale) {
        PoseStack matrixStack = drawContext.pose();
        synchronized (this.coordinateLock) {
            if (this.imageChanged) {
                this.imageChanged = false;
                this.mapImages[this.zoom].write();
                this.lastImageX = this.lastX;
                this.lastImageZ = this.lastZ;
            }
        }

        int mapScale = Math.min(scWidth, scHeight) - 64;
        int left = scWidth / 2 - mapScale / 2;
        int top = scHeight / 2 - mapScale / 2;
        float multi = (float) (0.015625f / this.zoomScale);
        float percentX = (float) (GameVariableAccessShim.xCoordDouble() - this.lastImageX) * multi;
        float percentY = (float) (GameVariableAccessShim.zCoordDouble() - this.lastImageZ) * multi;
        float uOne = 1.0f + percentX - 0.03125f; float vOne = 1.0f + percentY - 0.03125f;
        float uZero = percentX + 0.03125f; float vZero = percentY + 0.03125f;
        matrixStack.pushPose();
        matrixStack.translate(scWidth / 2.0F, scHeight / 2.0F, 0.0);
        matrixStack.mulPose(Axis.ZP.rotationDegrees(this.northRotate));
        matrixStack.translate(-(scWidth / 2.0F), -(scHeight / 2.0F), 0.0);
        RenderSystem.setShader(CoreShaders.POSITION_TEX);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        OpenGL.Utils.disp2(this.mapImages[this.zoom].getIndex());
        RenderSystem.bindTextureForSetup(this.mapImages[this.zoom].getIndex());
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR_MIPMAP_LINEAR);
        OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
        OpenGL.Utils.drawPre();
        OpenGL.Utils.ldrawone(left, top + mapScale, 160.0, uZero, vOne);
        OpenGL.Utils.ldrawone(left + mapScale, top + mapScale, 160.0, uOne, vOne);
        OpenGL.Utils.ldrawone(left + mapScale, top, 160.0, uOne, vZero);
        OpenGL.Utils.ldrawone(left, top, 160.0, uZero, vZero);
        OpenGL.Utils.drawPost();
        matrixStack.popPose();

        float markerScale = mapScale / 64f + mapScale / 1024f;
        int markerX = Math.round(scWidth / markerScale / 2f);
        int markerY = Math.round(scHeight / markerScale / 2f);
        modelStack.pushMatrix();
        modelStack.scale(markerScale, markerScale, 1f);
        if (VoxelConstants.getVoxelMapInstance().getRadar() != null) {
            this.layoutVariables.updateVars(scScale, markerX, markerY, this.zoomScale, this.zoomScale, false, true);
            VoxelConstants.getVoxelMapInstance().getRadar().onTickInGame(drawContext, modelStack, this.layoutVariables, markerScale * (float) (scScale / VoxelConstants.getMinecraft().getWindow().getGuiScale()));
        }
        modelStack.popMatrix();

        float fixedScale = scScale / (float)VoxelConstants.getMinecraft().getWindow().getGuiScale();

        int frameScale = mapScale + 8;
        OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
        this.drawMapFrame(scWidth / 2, scHeight / 2, 2, frameScale * 2);
        float directionTextSize = 0.75f;
        matrixStack.pushPose();
        matrixStack.scale(fixedScale * directionTextSize, fixedScale * directionTextSize, 1.0f);
        this.write(drawContext, "N", scWidth / (directionTextSize * 2f) - 2f, scHeight / (directionTextSize * 2f) - frameScale / (directionTextSize * 2f) - 8f, 0xFFFFFF);
        this.write(drawContext, "S", scWidth / (directionTextSize * 2f) - 2f, scHeight / (directionTextSize * 2f) + frameScale / (directionTextSize * 2f), 0xFFFFFF);
        this.write(drawContext, "E", scWidth / (directionTextSize * 2f) + frameScale / (directionTextSize * 2f), scHeight / (directionTextSize * 2f) - 4f, 0xFFFFFF);
        this.write(drawContext, "W", scWidth / (directionTextSize * 2f) - frameScale / (directionTextSize * 2f) - 6f, scHeight / (directionTextSize * 2f) - 4f, 0xFFFFFF);
        matrixStack.popPose();

        double lastXDouble = GameVariableAccessShim.xCoordDouble();
        double lastZDouble = GameVariableAccessShim.zCoordDouble();
        TextureAtlas textureAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
        OpenGL.Utils.disp2(textureAtlas.getId());
        OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
        modelStack.pushMatrix();
        modelStack.scale(markerScale, markerScale, 1f);
        if (VoxelMap.mapOptions.waypointsAllowed) {
            Waypoint highlightedPoint = this.waypointManager.getHighlightedWaypoint();

            for (Waypoint pt : this.waypointManager.getWaypoints()) {
                if (pt.isActive() || pt == highlightedPoint) {
                    double distanceSq = pt.getDistanceSqToEntity(VoxelConstants.getMinecraft().getCameraEntity());
                    if (distanceSq < (this.options.maxWaypointDisplayDistance * this.options.maxWaypointDisplayDistance) || this.options.maxWaypointDisplayDistance < 0 || pt == highlightedPoint) {
                        this.drawWaypoint(modelStack, pt, textureAtlas, null, null, null, null, markerX, markerY, lastXDouble, lastZDouble, this.zoomScale, false);
                    }
                }
            }

            if (highlightedPoint != null) {
                this.drawWaypoint(modelStack, highlightedPoint, textureAtlas, textureAtlas.getAtlasSprite("voxelmap:images/waypoints/target.png"), 1.0F, 0.0F, 0.0F, markerX, markerY, lastXDouble, lastZDouble, this.zoomScale, false);
            }
        }
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        modelStack.popMatrix();

        if (this.options.biomeOverlay != 0) {
            float biomeTextSize = 0.5f;
            double factor = Math.pow(2.0, 3 - this.zoom);
            int minimumSize = (int) Math.pow(2.0, this.zoom);
            minimumSize *= minimumSize;
            ArrayList<AbstractMapData.BiomeLabel> labels = this.mapData[this.zoom].getBiomeLabels();
            matrixStack.pushPose();
            matrixStack.scale(fixedScale * biomeTextSize, fixedScale * biomeTextSize, 1.0f);
            for (AbstractMapData.BiomeLabel o : labels) {
                if (o.segmentSize > minimumSize) {
                    String name = o.name;
                    int nameWidth = this.chkLen(name);
                    float x = (float) (o.x * factor);
                    float z = (float) (o.z * factor);
                    float labelX; float labelY;
                    if (this.options.oldNorth) {
                        labelX = (left + mapScale) - z - (nameWidth / 2f);
                        labelY = top + x - 3.0F;
                    } else {
                        labelX = left + x - (nameWidth / 2f);
                        labelY = top + z - 3.0F;
                    }
                    labelX *= (1f / biomeTextSize);
                    labelY *= (1f / biomeTextSize);
                    this.write(drawContext, name, labelX, labelY, 0xFFFFFF);
                }
            }
            matrixStack.popPose();
        }

    }

    private void drawMapFrame(int x, int y, int mode, int size) {
        try {
            ResourceLocation frame = switch(mode) {
                case 0 -> this.roundMapFrame;
                case 1 -> this.squareMapFrame;
                case 2 -> this.fullscreenMapFrame;
                default -> null;
            };
            OpenGL.Utils.img2(frame);
            OpenGL.Utils.drawPre();
            OpenGL.Utils.setMap(x, y, size);
            OpenGL.Utils.drawPost();
        } catch (Exception var4) {
            this.message = "Error: minimap overlay not found!";
        }
    }

    private void drawDirections(GuiGraphics drawContext, int x, int y, float scaleProj) {
        PoseStack matrixStack = drawContext.pose();
        boolean unicode = VoxelConstants.getMinecraft().options.forceUnicodeFont().get();
        float scale = unicode ? 0.65F : 0.5F;
        float rotate;
        if (this.options.rotates) {
            rotate = -this.direction - 90.0F - this.northRotate;
        } else {
            rotate = -90.0F;
        }

        float distance;
        if (this.options.shape == 1) {
            if (this.options.rotates) {
                float tempdir = this.direction % 90.0F;
                tempdir = 45.0F - Math.abs(45.0F - tempdir);
                distance = (float) (33.5 / scale / Math.cos(Math.toRadians(tempdir)));
            } else {
                distance = 33.5F / scale;
            }
        } else {
            distance = 32.0F / scale;
        }

        matrixStack.pushPose();
        matrixStack.scale(scaleProj, scaleProj, 1.0F);

        matrixStack.pushPose();
        matrixStack.scale(scale, scale, 1.0F);
        matrixStack.translate(distance * Math.sin(Math.toRadians(-(rotate - 90.0))), distance * Math.cos(Math.toRadians(-(rotate - 90.0))), 100.0);
        this.write(drawContext, "N", x / scale - 2.0F, y / scale - 4.0F, 16777215);
        matrixStack.popPose();
        matrixStack.pushPose();
        matrixStack.scale(scale, scale, 1.0F);
        matrixStack.translate(distance * Math.sin(Math.toRadians(-rotate)), distance * Math.cos(Math.toRadians(-rotate)), 10.0);
        this.write(drawContext, "E", x / scale - 2.0F, y / scale - 4.0F, 16777215);
        matrixStack.popPose();
        matrixStack.pushPose();
        matrixStack.scale(scale, scale, 1.0F);
        matrixStack.translate(distance * Math.sin(Math.toRadians(-(rotate + 90.0))), distance * Math.cos(Math.toRadians(-(rotate + 90.0))), 10.0);
        this.write(drawContext, "S", x / scale - 2.0F, y / scale - 4.0F, 16777215);
        matrixStack.popPose();
        matrixStack.pushPose();
        matrixStack.scale(scale, scale, 1.0F);
        matrixStack.translate(distance * Math.sin(Math.toRadians(-(rotate + 180.0))), distance * Math.cos(Math.toRadians(-(rotate + 180.0))), 10.0);
        this.write(drawContext, "W", x / scale - 2.0F, y / scale - 4.0F, 16777215);
        matrixStack.popPose();

        matrixStack.popPose();
    }

    private void showCoords(GuiGraphics drawContext, int x, int y, float scaleProj) {

        PoseStack matrixStack = drawContext.pose();
        int textStart;
        if (y > this.scHeight - 37 - 32 - 4 - 15) {
            textStart = y - 32 - 4 - 9;
        } else {
            textStart = y + 32 + 4;
        }

        if (this.options.displayMode != 0 && this.options.displayMode != 2 && !this.fullscreenMap) {
            float textScale = 0.5f;
            matrixStack.pushPose();
            matrixStack.scale(scaleProj * textScale, scaleProj * textScale, 1.0F);

            if (this.options.infoLabelMode == 1 || this.options.infoLabelMode == 2) {
                String xyz = this.dCoord(GameVariableAccessShim.xCoord()) + ", " + this.dCoord(GameVariableAccessShim.zCoord());
                int xPos = this.chkLen(xyz) / 2;
                this.write(drawContext, xyz, x * (1f / textScale) - xPos, textStart * (1f / textScale), 16777215); // X, Z

                xyz = dCoord(GameVariableAccessShim.yCoord());
                xPos = this.chkLen(xyz) / 2;
                this.write(drawContext, xyz, x * (1f / textScale) - xPos, textStart * (1f / textScale) + 10.0F, 16777215); // Y
            } else {
                String xyz = this.dCoord(GameVariableAccessShim.xCoord()) + ", " + this.dCoord(GameVariableAccessShim.yCoord()) + ", " + this.dCoord(GameVariableAccessShim.zCoord());
                int xPos = this.chkLen(xyz) / 2;
                this.write(drawContext, xyz, x * (1f / textScale) - xPos, textStart * (1f / textScale), 16777215); // X, Y, Z
            }

            if (this.options.infoLabelMode == 2 || this.options.infoLabelMode == 4) {
                String biomeString = GameVariableAccessShim.getWorld().getBiome(GameVariableAccessShim.playerBlockPos()).getRegisteredName();
                biomeString = I18n.get(biomeString.replace("minecraft:", "biome.minecraft."));
                int xPos = this.chkLen(biomeString) / 2;
                int yPos = this.options.infoLabelMode == 2 ? 20 : 10;
                this.write(drawContext, biomeString, x * (1f / textScale) - xPos, textStart * (1f / textScale) + yPos, 16777215);
            }

            if (zTimer > 0) {
                int xPos = this.chkLen(this.message) / 2;
                int yPos = (this.options.infoLabelMode == 1 || this.options.infoLabelMode == 2) ? 20 : 10;
                if (this.options.infoLabelMode == 2 || this.options.infoLabelMode == 4) {
                    yPos += 10;
                }
                this.write(drawContext, this.message, x * (1f / textScale) - xPos, textStart * (1f / textScale) + yPos, 16777215); // WORLD NAME
            }

            matrixStack.popPose();
        } else {
            int heading = (int) (this.direction + this.northRotate);
            if (heading > 360) {
                heading -= 360;
            }
            String ns = "";
            String ew = "";
            if (heading > 360 - 67.5 || heading <= 67.5) {
                ns = "N";
            } else if (heading > 180 - 67.5 && heading <= 180 + 67.5) {
                ns = "S";
            }
            if (heading > 90 - 67.5 && heading <= 90 + 67.5) {
                ew = "E";
            } else if (heading > 270 - 67.5 && heading <= 270 + 67.5) {
                ew = "W";
            }

            String stats = "(" + this.dCoord(GameVariableAccessShim.xCoord()) + ", " + this.dCoord(GameVariableAccessShim.yCoord()) + ", " + this.dCoord(GameVariableAccessShim.zCoord()) + ") " + heading + "' " + ns + ew;
            int halfWidth = this.chkLen(stats) / 2;
            int scaledWidth = VoxelConstants.getMinecraft().getWindow().getGuiScaledWidth();
            this.write(drawContext, stats, scaledWidth / 2f - halfWidth, 5.0F, 16777215);
            if (this.zTimer > 0) {
                halfWidth = this.chkLen(this.message) / 2;
                this.write(drawContext, this.message, scaledWidth / 2f - halfWidth, 15.0F, 16777215);
            }
        }
    }

    private String dCoord(int paramInt1) {
        if (paramInt1 < 0) {
            return "-" + Math.abs(paramInt1);
        } else {
            return paramInt1 > 0 ? "+" + paramInt1 : "" + paramInt1;
        }
    }

    private int chkLen(String string) {
        return this.fontRenderer.width(string);
    }

    private void write(GuiGraphics drawContext, String text, float x, float y, int color) {
        write(drawContext, Component.nullToEmpty(text), x, y, color);
    }

    private int chkLen(Component text) {
        return this.fontRenderer.width(text);
    }

    private void write(GuiGraphics drawContext, Component text, float x, float y, int color) {
        drawContext.drawString(this.fontRenderer, text, (int) x, (int) y, color);
    }

    private void drawWelcomeScreen(GuiGraphics drawContext, int scWidth, int scHeight) {
        if (this.welcomeText[1] == null || this.welcomeText[1].getString().equals("minimap.ui.welcome2")) {
            this.welcomeText[0] = (Component.literal("")).append((Component.literal("VoxelMap! ")).withStyle(ChatFormatting.RED)).append(Component.translatable("minimap.ui.welcome1"));
            this.welcomeText[1] = Component.translatable("minimap.ui.welcome2");
            this.welcomeText[2] = Component.translatable("minimap.ui.welcome3");
            this.welcomeText[3] = Component.translatable("minimap.ui.welcome4");
            this.welcomeText[4] = (Component.literal("")).append((Component.keybind(this.options.keyBindZoom.getName())).withStyle(ChatFormatting.AQUA)).append(": ").append(Component.translatable("minimap.ui.welcome5aa"))
                    .append(", ").append((Component.keybind(this.options.keyBindZoomOut.getName())).withStyle(ChatFormatting.AQUA)).append(": ").append(Component.translatable("minimap.ui.welcome5ab"));
            this.welcomeText[5] = (Component.literal("")).append((Component.keybind(this.options.keyBindMenu.getName())).withStyle(ChatFormatting.AQUA)).append(": ").append(Component.translatable("minimap.ui.welcome5b"))
                    .append(", ").append((Component.keybind(this.options.keyBindFullscreen.getName())).withStyle(ChatFormatting.AQUA)).append(": ").append(Component.translatable("minimap.ui.welcome6"));
            this.welcomeText[6] = (Component.literal("")).append((Component.keybind(this.options.keyBindWaypoint.getName())).withStyle(ChatFormatting.AQUA)).append(": ").append(Component.translatable("minimap.ui.welcome7"))
                    .append(", ").append((Component.keybind(VoxelConstants.getAlternativeListKey().getName())).withStyle(ChatFormatting.AQUA)).append(": ").append(Component.translatable("minimap.ui.welcome7a"));;
            this.welcomeText[7] = this.options.keyBindFullscreen.getTranslatedKeyMessage().copy().append(": ").append((Component.translatable("minimap.ui.welcome8")).withStyle(ChatFormatting.GRAY));
        }

        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
        int maxSize = 0;
        int border = 2;
        Component head = this.welcomeText[0];

        int height;
        for (height = 1; height < this.welcomeText.length - 1; ++height) {
            if (this.chkLen(this.welcomeText[height]) > maxSize) {
                maxSize = this.chkLen(this.welcomeText[height]);
            }
        }

        int title = this.chkLen(head);
        int centerX = (int) ((scWidth + 5) / 2.0);
        int centerY = (int) ((scHeight + 5) / 2.0);
        Component hide = this.welcomeText[this.welcomeText.length - 1];
        int footer = this.chkLen(hide);
        int leftX = centerX - title / 2 - border;
        int rightX = centerX + title / 2 + border;
        int topY = centerY - (height - 1) / 2 * 10 - border - 20;
        int botY = centerY - (height - 1) / 2 * 10 + border - 10;
        this.drawBox(drawContext, leftX, rightX, topY, botY);
        leftX = centerX - maxSize / 2 - border;
        rightX = centerX + maxSize / 2 + border;
        topY = centerY - (height - 1) / 2 * 10 - border;
        botY = centerY + (height - 1) / 2 * 10 + border;
        this.drawBox(drawContext, leftX, rightX, topY, botY);
        leftX = centerX - footer / 2 - border;
        rightX = centerX + footer / 2 + border;
        topY = centerY + (height - 1) / 2 * 10 - border + 10;
        botY = centerY + (height - 1) / 2 * 10 + border + 20;
        this.drawBox(drawContext, leftX, rightX, topY, botY);
        drawContext.drawString(this.fontRenderer, head, (centerX - title / 2), (centerY - (height - 1) * 10 / 2 - 19), Color.WHITE.getRGB());
        for (int n = 1; n < height; ++n) {
            drawContext.drawString(this.fontRenderer, this.welcomeText[n], (centerX - maxSize / 2), (centerY - (height - 1) * 10 / 2 + n * 10 - 9), Color.WHITE.getRGB());
        }

        drawContext.drawString(this.fontRenderer, hide, (centerX - footer / 2), ((scHeight + 5) / 2 + (height - 1) * 10 / 2 + 11), Color.WHITE.getRGB());
    }

    private void drawBox(GuiGraphics drawContext, int leftX, int rightX, int topY, int botY) {
        double e = VoxelConstants.getMinecraft().options.textBackgroundOpacity().get();
        int v = (int) (255.0 * 1.0 * e);
        drawContext.fill(leftX, topY, rightX, botY, v << 24);
    }

    public static double getMinTablistOffset() {
        return minTablistOffset;
    }

    public static float getStatusIconOffset() {
        return statusIconOffset;
    }
}
