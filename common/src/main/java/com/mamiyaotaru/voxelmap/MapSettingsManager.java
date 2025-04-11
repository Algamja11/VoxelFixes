package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.ISettingsManager;
import com.mamiyaotaru.voxelmap.interfaces.ISubSettingsManager;
import com.mamiyaotaru.voxelmap.util.MessageUtils;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class MapSettingsManager implements ISettingsManager {
    private File settingsFile;
    public boolean showUnderMenus;
    private final int availableProcessors = Runtime.getRuntime().availableProcessors();
    public final boolean multicore = this.availableProcessors > 1;
    public boolean hide = false; // "Hide Minimap"
    public int coordsMode = 1; // "Coordinates Mode"
    public boolean oldNorth = false; // "Old North"
    public boolean showBiomeLabel = true; // "Show Biome Label"
    public int sizeModifier = 1; // "Map Size"
    public boolean squareMap = true; // "Square Map"
    public boolean rotates = true; // "Rotation"
    public int mapCorner = 1; // "Map Corner"
    protected boolean showCaves = true; // "Enable Cave Mode"
    public boolean showWaypointBeacons = false; // "Waypoint Beacons"
    public boolean showWaypointSigns = true; // "Waypoint Signs"
    private boolean preToggleWaypointBeacons = false;
    private boolean preToggleWaypointSigns = true;
    public boolean moveScoreboardBelowMap = true; // "Move Scoreboard Below Map"
    public boolean moveMapBelowStatusEffect = true; // "Move Map Below Status Effect"
    public boolean lightmap = true; // "Dynamic Lighting"
    public boolean heightmap = this.multicore; // "Height Map"
    public boolean slopemap = true; // "Slope Map"
    public boolean filtering = false; // "Filtering"
    public boolean waterTransparency = this.multicore; // "Water Transparency"
    public boolean blockTransparency = this.multicore; // "Block Transparency"
    public boolean biomes = this.multicore; // "Biomes"
    public int biomeOverlay = 0; // "Biome Overlay"
    public boolean chunkGrid = false; // "Chunk Grid"
    public boolean slimeChunks = false; // "Slime Chunks"
    public boolean worldborder = true; // "World Border"
    public String teleportCommand = "tp %p %x %y %z"; // "Teleport Command"
    public String serverTeleportCommand = null;

    public int maxWaypointDisplayDistance = 1000; // "Waypoint Max Distance"
    public int deathpoints = 1; // "Deathpoints"
    public boolean distanceUnitConversion = true; // "Distance Unit Conversion"
    public boolean waypointNameBelowIcon = true; // "Waypoint Name Below Icon"
    public boolean waypointDistanceBelowName = true; // "Waypoint Distance Below Name"
    public int sort = 1; // "Waypoint Sort By"

    public final KeyMapping keyBindZoomIn = new KeyMapping("key.voxelmap.zoomin", GLFW.GLFW_KEY_UP, "controls.voxelmap.title"); // "Zoom In Key"
    public final KeyMapping keyBindZoomOut = new KeyMapping("key.voxelmap.zoomout", GLFW.GLFW_KEY_DOWN, "controls.voxelmap.title"); // "Zoom Out Key"
    public final KeyMapping keyBindFullscreen = new KeyMapping("key.voxelmap.togglefullscreen", GLFW.GLFW_KEY_Z, "controls.voxelmap.title"); // "Fullscreen Key"
    public final KeyMapping keyBindMenu = new KeyMapping("key.voxelmap.voxelmapmenu", GLFW.GLFW_KEY_M, "controls.voxelmap.title"); // "Menu Key"
    public final KeyMapping keyBindWaypointMenu = new KeyMapping("key.voxelmap.waypointmenu", GLFW.GLFW_KEY_U, "controls.voxelmap.title"); // "Waypoint Menu Key"
    public final KeyMapping keyBindWaypoint = new KeyMapping("key.voxelmap.waypointhotkey", GLFW.GLFW_KEY_N, "controls.voxelmap.title"); // "Waypoint Key"
    public final KeyMapping keyBindMobToggle = new KeyMapping("key.voxelmap.togglemobs", GLFW.GLFW_KEY_UNKNOWN, "controls.voxelmap.title"); // "Mob Key"
    public final KeyMapping keyBindWaypointToggle = new KeyMapping("key.voxelmap.toggleingamewaypoints", GLFW.GLFW_KEY_UNKNOWN, "controls.voxelmap.title"); // "In-game Waypoint Key"
    public final KeyMapping[] keyBindings;

    protected boolean welcome = true; // "Welcome Message"
    public int zoom = 2; // "Zoom Level"

    public Boolean cavesAllowed = true;
    public boolean worldmapAllowed = true;
    public boolean minimapAllowed = true;
    public boolean waypointsAllowed = true;
    public boolean deathWaypointAllowed = true;

    private boolean somethingChanged;
    public static MapSettingsManager instance;
    private final List<ISubSettingsManager> subSettingsManagers = new ArrayList<>();

    public MapSettingsManager() {
        instance = this;
        this.keyBindings = new KeyMapping[] { this.keyBindMenu, this.keyBindWaypointMenu, this.keyBindZoomIn, this.keyBindZoomOut, this.keyBindFullscreen, this.keyBindWaypoint, this.keyBindMobToggle, this.keyBindWaypointToggle };
    }

    public void addSecondaryOptionsManager(ISubSettingsManager secondarySettingsManager) {
        this.subSettingsManagers.add(secondarySettingsManager);
    }

    public void loadAll() {
        this.settingsFile = new File(VoxelConstants.getMinecraft().gameDirectory, "config/voxelmap.properties");

        try {
            if (this.settingsFile.exists()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(this.settingsFile), StandardCharsets.UTF_8.newDecoder()));
                String sCurrentLine;
                while ((sCurrentLine = in.readLine()) != null) {
                    String[] curLine = sCurrentLine.split(":");
                    switch (curLine[0]) {
                        case "Hide Minimap" -> this.hide = Boolean.parseBoolean(curLine[1]);
                        case "Coordinates Mode" -> this.coordsMode = Math.max(0, Math.min(2, Integer.parseInt(curLine[1])));
                        case "Old North" -> this.oldNorth = Boolean.parseBoolean(curLine[1]);
                        case "Show Biome Label" -> this.showBiomeLabel = Boolean.parseBoolean(curLine[1]);
                        case "Map Size" -> this.sizeModifier = Math.max(-1, Math.min(4, Integer.parseInt(curLine[1])));
                        case "Square Map" -> this.squareMap = Boolean.parseBoolean(curLine[1]);
                        case "Rotation" -> this.rotates = Boolean.parseBoolean(curLine[1]);
                        case "Map Corner" -> this.mapCorner = Math.max(0, Math.min(3, Integer.parseInt(curLine[1])));
                        case "Enable Cave Mode" -> this.showCaves = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Beacons" -> this.showWaypointBeacons = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Signs" -> this.showWaypointSigns = Boolean.parseBoolean(curLine[1]);
                        case "Move Scoreboard Below Map" -> this.moveScoreboardBelowMap = Boolean.parseBoolean(curLine[1]);
                        case "Move Map Below Status Effect" -> this.moveMapBelowStatusEffect = Boolean.parseBoolean(curLine[1]);
                        case "Dynamic Lighting" -> this.lightmap = Boolean.parseBoolean(curLine[1]);
                        case "Height Map" -> this.heightmap = Boolean.parseBoolean(curLine[1]);
                        case "Slope Map" -> this.slopemap = Boolean.parseBoolean(curLine[1]);
                        case "Filtering" -> this.filtering = Boolean.parseBoolean(curLine[1]);
                        case "Water Transparency" -> this.waterTransparency = Boolean.parseBoolean(curLine[1]);
                        case "Block Transparency" -> this.blockTransparency = Boolean.parseBoolean(curLine[1]);
                        case "Biomes" -> this.biomes = Boolean.parseBoolean(curLine[1]);
                        case "Biome Overlay" -> this.biomeOverlay = Math.max(0, Math.min(2, Integer.parseInt(curLine[1])));
                        case "Chunk Grid" -> this.chunkGrid = Boolean.parseBoolean(curLine[1]);
                        case "Slime Chunks" -> this.slimeChunks = Boolean.parseBoolean(curLine[1]);
                        case "World Border" -> this.worldborder = Boolean.parseBoolean(curLine[1]);
                        case "Teleport Command" -> this.teleportCommand = curLine[1];
                        case "Waypoint Max Distance" -> this.maxWaypointDisplayDistance = Math.max(-1, Math.min(10000, Integer.parseInt(curLine[1])));
                        case "Deathpoints" -> this.deathpoints = Math.max(0, Math.min(2, Integer.parseInt(curLine[1])));
                        case "Distance Unit Conversion" -> this.distanceUnitConversion = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Name Below Icon" -> this.waypointNameBelowIcon = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Distance Below Name" -> this.waypointDistanceBelowName = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Sort By" -> this.sort = Math.max(1, Math.min(4, Integer.parseInt(curLine[1])));
                        case "Zoom In Key" -> this.bindKey(this.keyBindZoomIn, curLine[1]);
                        case "Zoom Out Key" -> this.bindKey(this.keyBindZoomOut, curLine[1]);
                        case "Fullscreen Key" -> this.bindKey(this.keyBindFullscreen, curLine[1]);
                        case "Menu Key" -> this.bindKey(this.keyBindMenu, curLine[1]);
                        case "Waypoint Menu Key" -> this.bindKey(this.keyBindWaypointMenu, curLine[1]);
                        case "Waypoint Key" -> this.bindKey(this.keyBindWaypoint, curLine[1]);
                        case "Mob Key" -> this.bindKey(this.keyBindMobToggle, curLine[1]);
                        case "In-game Waypoint Key" -> this.bindKey(this.keyBindWaypointToggle, curLine[1]);
                        case "Welcome Message" -> this.welcome = Boolean.parseBoolean(curLine[1]);
                        case "Zoom Level" -> this.zoom = Math.max(0, Math.min(4, Integer.parseInt(curLine[1])));
                    }
                }
                KeyMapping.resetMapping();
                for (ISubSettingsManager subSettingsManager : this.subSettingsManagers) {
                    subSettingsManager.loadSettings(this.settingsFile);
                }

                in.close();
            }

            this.saveAll();
        } catch (IOException exception) {
            VoxelConstants.getLogger().error(exception);
        }

    }

    private void bindKey(KeyMapping keyBinding, String id) {
        try {
            keyBinding.setKey(InputConstants.getKey(id));
        } catch (RuntimeException var4) {
            VoxelConstants.getLogger().warn(id + " is not a valid keybinding");
        }

    }

    public void saveAll() {
        File settingsFileDir = new File(VoxelConstants.getMinecraft().gameDirectory, "/config/");
        if (!settingsFileDir.exists()) {
            settingsFileDir.mkdirs();
        }

        this.settingsFile = new File(settingsFileDir, "voxelmap.properties");

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.settingsFile), StandardCharsets.UTF_8.newEncoder())));
            out.println("Hide Minimap:" + this.hide);
            out.println("Coordinates Mode:" + this.coordsMode);
            out.println("Old North:" + this.oldNorth);
            out.println("Show Biome Label:" + this.showBiomeLabel);
            out.println("Map Size:" + this.sizeModifier);
            out.println("Square Map:" + this.squareMap);
            out.println("Rotation:" + this.rotates);
            out.println("Map Corner:" + this.mapCorner);
            out.println("Enable Cave Mode:" + this.showCaves);
            out.println("Waypoint Beacons:" + this.showWaypointBeacons);
            out.println("Waypoint Signs:" + this.showWaypointSigns);
            out.println("Move Scoreboard Below Map" + this.moveScoreboardBelowMap);
            out.println("Move Map Below Status Effect" + this.moveMapBelowStatusEffect);
            out.println("Dynamic Lighting:" + this.lightmap);
            out.println("Height Map:" + this.heightmap);
            out.println("Slope Map:" + this.slopemap);
            out.println("Filtering:" + this.filtering);
            out.println("Water Transparency:" + this.waterTransparency);
            out.println("Block Transparency:" + this.blockTransparency);
            out.println("Biomes:" + this.biomes);
            out.println("Biome Overlay:" + this.biomeOverlay);
            out.println("Chunk Grid:" + this.chunkGrid);
            out.println("Slime Chunks:" + this.slimeChunks);
            out.println("World Border:" + this.worldborder);
            out.println("Teleport Command:" + this.teleportCommand);
            out.println("Waypoint Max Distance:" + this.maxWaypointDisplayDistance);
            out.println("Deathpoints:" + this.deathpoints);
            out.println("Distance Unit Conversion:" + this.distanceUnitConversion);
            out.println("Waypoint Name Below Icon:" + this.waypointNameBelowIcon);
            out.println("Waypoint Distance Below Name:" + this.waypointDistanceBelowName);
            out.println("Waypoint Sort By:" + this.sort);
            out.println("Zoom In Key:" + this.keyBindZoomIn.saveString());
            out.println("Zoom Out Key:" + this.keyBindZoomOut.saveString());
            out.println("Fullscreen Key:" + this.keyBindFullscreen.saveString());
            out.println("Menu Key:" + this.keyBindMenu.saveString());
            out.println("Waypoint Menu Key:" + this.keyBindWaypointMenu.saveString());
            out.println("Waypoint Key:" + this.keyBindWaypoint.saveString());
            out.println("Mob Key:" + this.keyBindMobToggle.saveString());
            out.println("In-game Waypoint Key:" + this.keyBindWaypointToggle.saveString());
            out.println("Welcome Message:" + this.welcome);
            out.println("Zoom Level:" + this.zoom);

            for (ISubSettingsManager subSettingsManager : this.subSettingsManagers) {
                subSettingsManager.saveAll(out);
            }

            out.close();
        } catch (FileNotFoundException var5) {
            MessageUtils.chatInfo("§EError Saving Settings " + var5.getLocalizedMessage());
        }
    }

    @Override
    public String getKeyText(EnumOptionsMinimap options) {
        String s = I18n.get(options.getName()) + ": ";
        if (options.isFloat()) {
            float f = this.getOptionFloatValue(options);
            if (options == EnumOptionsMinimap.ZOOM_LEVEL) {
                return s + (int) f;
            } else if (options == EnumOptionsMinimap.WAYPOINT_DISTANCE) {
                return f < 0.0F ? s + I18n.get("options.voxelmap.waypoints.infinite") : s + (int) f;
            } else {
                return f == 0.0F ? s + I18n.get("options.off") : s + (int) f + "%";
            }
        } else if (options.isBoolean()) {
            boolean flag = this.getOptionBooleanValue(options);
            return flag ? s + I18n.get("options.on") : s + I18n.get("options.off");
        } else if (options.isList()) {
            String state = this.getOptionListValue(options);
            return s + state;
        } else {
            return s;
        }
    }

    @Override
    public float getOptionFloatValue(EnumOptionsMinimap options) {
        if (options == EnumOptionsMinimap.ZOOM_LEVEL) {
            return this.zoom;
        } else {
            return options == EnumOptionsMinimap.WAYPOINT_DISTANCE ? this.maxWaypointDisplayDistance : 0.0F;
        }
    }

    public boolean getOptionBooleanValue(EnumOptionsMinimap par1EnumOptions) {
        return switch (par1EnumOptions) {
            case HIDE_MINIMAP -> this.hide || !this.minimapAllowed;
            case OLD_NORTH -> this.oldNorth;
            case SHOW_BIOME_LABEL -> this.showBiomeLabel;
            case SQUAREMAP -> this.squareMap;
            case ROTATES -> this.rotates;
            case CAVE_MODE -> this.cavesAllowed && this.showCaves;
            case MOVE_SCOREBOARD_BELOW_MAP -> this.moveScoreboardBelowMap;
            case MOVE_MAP_BELOW_STATUS_EFFECT -> this.moveMapBelowStatusEffect;
            case DYNAMIC_LIGHTING -> this.lightmap;
            case FILTERING -> this.filtering;
            case WATER_TRANSPARENCY -> this.waterTransparency;
            case BLOCK_TRANSPARENCY -> this.blockTransparency;
            case BIOME_TINT -> this.biomes;
            case CHUNK_GRID -> this.chunkGrid;
            case SLIME_CHUNKS -> this.slimeChunks;
            case WORLD_BORDER -> this.worldborder;
            case DISTANCE_UNIT_CONVERSION -> this.distanceUnitConversion;
            case NAME_LABEL_BELOW_ICON -> this.waypointNameBelowIcon;
            case DISTANCE_LABEL_BELOW_NAME -> this.waypointDistanceBelowName;
            case WELCOME_SCREEN -> this.welcome;
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName() + ". (possibly not a boolean applicable to minimap)");
        };
    }

    public String getOptionListValue(EnumOptionsMinimap par1EnumOptions) {
        switch (par1EnumOptions) {
            case SHOW_COORDINATES -> {
                if (this.coordsMode == 0) {
                    return I18n.get("options.off");
                } else if (this.coordsMode == 1) {
                    return I18n.get("options.voxelmap.showcoordinates.mode1");
                } else {
                    if (this.coordsMode == 2) {
                        return I18n.get("options.voxelmap.showcoordinates.mode2");
                    }

                    return "error";
                }
            }
            case SIZE -> {
                if (this.sizeModifier == -1) {
                    return I18n.get("options.voxelmap.size.small");
                } else if (this.sizeModifier == 0) {
                    return I18n.get("options.voxelmap.size.medium");
                } else if (this.sizeModifier == 1) {
                    return I18n.get("options.voxelmap.size.large");
                } else if (this.sizeModifier == 2) {
                    return I18n.get("options.voxelmap.size.xl");
                } else if (this.sizeModifier == 3) {
                    return I18n.get("options.voxelmap.size.xxl");
                } else {
                    if (this.sizeModifier == 4) {
                        return I18n.get("options.voxelmap.size.xxxl");
                    }

                    return "error";
                }
            }
            case LOCATION -> {
                if (this.mapCorner == 0) {
                    return I18n.get("options.voxelmap.location.topleft");
                } else if (this.mapCorner == 1) {
                    return I18n.get("options.voxelmap.location.topright");
                } else if (this.mapCorner == 2) {
                    return I18n.get("options.voxelmap.location.bottomright");
                } else {
                    if (this.mapCorner == 3) {
                        return I18n.get("options.voxelmap.location.bottomleft");
                    }

                    return "Error";
                }
            }
            case INGAME_WAYPOINTS -> {
                if (this.waypointsAllowed && this.showWaypointBeacons && this.showWaypointSigns) {
                    return I18n.get("options.voxelmap.ingamewaypoints.both");
                } else if (this.waypointsAllowed && this.showWaypointBeacons) {
                    return I18n.get("options.voxelmap.ingamewaypoints.beacons");
                } else if (this.waypointsAllowed && this.showWaypointSigns) {
                    return I18n.get("options.voxelmap.ingamewaypoints.signs");
                }
                return I18n.get("options.off");
            }
            case TERRAIN_DEPTH -> {
                if (this.slopemap && this.heightmap) {
                    return I18n.get("options.voxelmap.terrain.both");
                } else if (this.heightmap) {
                    return I18n.get("options.voxelmap.terrain.height");
                } else if (this.slopemap) {
                    return I18n.get("options.voxelmap.terrain.slope");
                }
                return I18n.get("options.off");
            }
            case BIOME_OVERLAY -> {
                if (this.biomeOverlay == 0) {
                    return I18n.get("options.off");
                } else if (this.biomeOverlay == 1) {
                    return I18n.get("options.voxelmap.biomeoverlay.solid");
                } else {
                    if (this.biomeOverlay == 2) {
                        return I18n.get("options.voxelmap.biomeoverlay.transparent");
                    }

                    return "error";
                }
            }
            case DEATHPOINTS -> {
                if (this.deathpoints == 0) {
                    return I18n.get("options.off");
                } else if (this.deathpoints == 1) {
                    return I18n.get("options.voxelmap.waypoints.deathpoints.mostrecent");
                } else {
                    if (this.deathpoints == 2) {
                        return I18n.get("options.voxelmap.waypoints.deathpoints.all");
                    }

                    return "error";
                }
            }
            default ->
                    throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName() + ". (possibly not a list value applicable to minimap)");
        }
    }

    @Override
    public void setOptionFloatValue(EnumOptionsMinimap options, float value) {
        if (options == EnumOptionsMinimap.WAYPOINT_DISTANCE) {
            float distance = value * 9951.0F + 50.0F;
            if (distance > 10000.0F) {
                distance = -1.0F;
            }

            this.maxWaypointDisplayDistance = (int) distance;
        }

        this.somethingChanged = true;
    }

    public void setOptionValue(EnumOptionsMinimap par1EnumOptions) {
        switch (par1EnumOptions) {
            case HIDE_MINIMAP -> this.hide = !this.hide;
            case OLD_NORTH -> this.oldNorth = !this.oldNorth;
            case SHOW_BIOME_LABEL -> this.showBiomeLabel = !this.showBiomeLabel;
            case SQUAREMAP -> this.squareMap = !this.squareMap;
            case ROTATES -> this.rotates = !this.rotates;
            case CAVE_MODE -> this.showCaves = !this.showCaves;
            case MOVE_SCOREBOARD_BELOW_MAP -> this.moveScoreboardBelowMap = !this.moveScoreboardBelowMap;
            case MOVE_MAP_BELOW_STATUS_EFFECT -> this.moveMapBelowStatusEffect = !this.moveMapBelowStatusEffect;
            case DYNAMIC_LIGHTING -> this.lightmap = !this.lightmap;
            case FILTERING -> this.filtering = !this.filtering;
            case WATER_TRANSPARENCY -> this.waterTransparency = !this.waterTransparency;
            case BLOCK_TRANSPARENCY -> this.blockTransparency = !this.blockTransparency;
            case BIOME_TINT -> this.biomes = !this.biomes;
            case CHUNK_GRID -> this.chunkGrid = !this.chunkGrid;
            case SLIME_CHUNKS -> this.slimeChunks = !this.slimeChunks;
            case WORLD_BORDER -> this.worldborder = !this.worldborder;
            case DISTANCE_UNIT_CONVERSION -> this.distanceUnitConversion = !this.distanceUnitConversion;
            case NAME_LABEL_BELOW_ICON -> this.waypointNameBelowIcon = !this.waypointNameBelowIcon;
            case DISTANCE_LABEL_BELOW_NAME -> this.waypointDistanceBelowName = !this.waypointDistanceBelowName;
            case WELCOME_SCREEN -> this.welcome = !this.welcome;
            case SHOW_COORDINATES -> {
                ++this.coordsMode;
                if (this.coordsMode > 2) {
                    this.coordsMode = 0;
                }
            }
            case SIZE -> {
                ++this.sizeModifier;
                if (this.sizeModifier > 4) {
                    this.sizeModifier = -1;
                }
            }
            case LOCATION -> {
                ++this.mapCorner;
                if (this.mapCorner > 3) {
                    this.mapCorner = 0;
                }
            }
            case INGAME_WAYPOINTS -> {
                if (this.showWaypointBeacons && this.showWaypointSigns) {
                    this.showWaypointBeacons = false;
                    this.showWaypointSigns = false;
                } else if (this.showWaypointBeacons) {
                    this.showWaypointBeacons = false;
                    this.showWaypointSigns = true;
                } else if (this.showWaypointSigns) {
                    this.showWaypointBeacons = true;
                } else {
                    this.showWaypointBeacons = true;
                }
            }
            case TERRAIN_DEPTH -> {
                if (this.slopemap && this.heightmap) {
                    this.slopemap = false;
                    this.heightmap = false;
                } else if (this.slopemap) {
                    this.slopemap = false;
                    this.heightmap = true;
                } else if (this.heightmap) {
                    this.slopemap = true;
                } else {
                    this.slopemap = true;
                }
            }
            case BIOME_OVERLAY -> {
                ++this.biomeOverlay;
                if (this.biomeOverlay > 2) {
                    this.biomeOverlay = 0;
                }
            }
            case DEATHPOINTS -> {
                ++this.deathpoints;
                if (this.deathpoints > 2) {
                    this.deathpoints = 0;
                }
            }
            default ->
                    throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName());
        }

        this.somethingChanged = true;
    }

    public void toggleIngameWaypoints() {
        if (!this.showWaypointBeacons && !this.showWaypointSigns) {
            this.showWaypointBeacons = this.preToggleWaypointBeacons;
            this.showWaypointSigns = this.preToggleWaypointSigns;
        } else {
            this.preToggleWaypointBeacons = this.showWaypointBeacons;
            this.preToggleWaypointSigns = this.showWaypointSigns;
            this.showWaypointBeacons = false;
            this.showWaypointSigns = false;
        }
    }

    public String getKeyBindingDescription(int keybindIndex) {
        return this.keyBindings[keybindIndex].getName().equals("key.voxelmap.voxelmapmenu") ? I18n.get("key.voxelmap.menu") : I18n.get(this.keyBindings[keybindIndex].getName());
    }

    public Component getKeybindDisplayString(int keybindIndex) {
        KeyMapping keyBinding = this.keyBindings[keybindIndex];
        return this.getKeybindDisplayString(keyBinding);
    }

    public Component getKeybindDisplayString(KeyMapping keyBinding) {
        return keyBinding.getTranslatedKeyMessage();
    }

    public void setKeyBinding(KeyMapping keyBinding, InputConstants.Key input) {
        keyBinding.setKey(input);
        this.saveAll();
    }

    public void setSort(int sort) {
        if (sort != this.sort && sort != -this.sort) {
            this.sort = sort;
        } else {
            this.sort = -this.sort;
        }

    }

    public boolean isChanged() {
        if (this.somethingChanged) {
            this.somethingChanged = false;
            return true;
        } else {
            return false;
        }
    }
}
