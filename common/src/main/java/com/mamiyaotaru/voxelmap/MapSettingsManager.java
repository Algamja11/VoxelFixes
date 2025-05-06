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

    public boolean hide;
    public int coordsMode = 1;
    public boolean oldNorth;
    public boolean showBiomeLabel = true;
    public int sizeModifier = 1;
    public boolean squareMap = true;
    public boolean rotates = true;
    public int mapCorner = 1;
    protected boolean showCaves = true;
    public boolean showWaypointBeacons;
    public boolean showWaypointSigns = true;
    private boolean preToggleWaypointBeacons;
    private boolean preToggleWaypointSigns = true;
    public boolean moveScoreboardBelowMap = true;
    public boolean moveMapBelowStatusEffectIcons = true;
    public boolean lightmap = true;
    public boolean heightmap = this.multicore;
    public boolean slopemap = true;
    public boolean filtering;
    public boolean waterTransparency = this.multicore;
    public boolean blockTransparency = this.multicore;
    public boolean biomes = this.multicore;
    public int biomeOverlay;
    public boolean chunkGrid;
    public boolean slimeChunks;
    public boolean worldborder = true;
    public String teleportCommand = "tp %p %x %y %z";
    public String serverTeleportCommand;

    public int maxWaypointDisplayDistance = 1000;
    public float waypointIconSize = 1.0F;
    public int deathpoints = 1;
    public boolean autoUnitConversion = true;
    public int showWaypointNames = 2;
    public int showWaypointDistances = 2;
    public float waypointFontSize = 1.0F;
    public boolean showWaypointNamesOnMap = true;
    public int sort = 1;

    public final KeyMapping keyBindZoomIn = new KeyMapping("key.voxelmap.zoom_in", GLFW.GLFW_KEY_UP, "controls.voxelmap.title");
    public final KeyMapping keyBindZoomOut = new KeyMapping("key.voxelmap.zoom_out", GLFW.GLFW_KEY_DOWN, "controls.voxelmap.title");
    public final KeyMapping keyBindEnlargedMap = new KeyMapping("key.voxelmap.toggle_enlarged_map", GLFW.GLFW_KEY_Z, "controls.voxelmap.title");
    public final KeyMapping keyBindFullscreenMap = new KeyMapping("key.voxelmap.toggle_fullscreen_map", GLFW.GLFW_KEY_X, "controls.voxelmap.title");
    public final KeyMapping keyBindMenu = new KeyMapping("key.voxelmap.voxelmap_menu", GLFW.GLFW_KEY_M, "controls.voxelmap.title");
    public final KeyMapping keyBindWaypointMenu = new KeyMapping("key.voxelmap.waypoint_menu", GLFW.GLFW_KEY_U, "controls.voxelmap.title");
    public final KeyMapping keyBindWaypoint = new KeyMapping("key.voxelmap.waypoint_hotkey", GLFW.GLFW_KEY_N, "controls.voxelmap.title");
    public final KeyMapping keyBindMobToggle = new KeyMapping("key.voxelmap.toggle_mobs", GLFW.GLFW_KEY_UNKNOWN, "controls.voxelmap.title");
    public final KeyMapping keyBindWaypointToggle = new KeyMapping("key.voxelmap.toggle_waypoints", GLFW.GLFW_KEY_UNKNOWN, "controls.voxelmap.title");
    public final KeyMapping[] keyBindings;

    protected boolean welcome = true;
    public int zoom = 2;

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
        this.keyBindings = new KeyMapping[] { this.keyBindMenu, this.keyBindWaypointMenu, this.keyBindZoomIn, this.keyBindZoomOut, this.keyBindEnlargedMap, this.keyBindFullscreenMap, this.keyBindWaypoint, this.keyBindMobToggle, this.keyBindWaypointToggle };
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
                        case "Square Map" -> this.squareMap = Boolean.parseBoolean(curLine[1]);
                        case "Map Size" -> this.sizeModifier = Math.max(-1, Math.min(4, Integer.parseInt(curLine[1])));
                        case "Rotation" -> this.rotates = Boolean.parseBoolean(curLine[1]);
                        case "Map Corner" -> this.mapCorner = Math.max(0, Math.min(3, Integer.parseInt(curLine[1])));
                        case "Enable Cave Mode" -> this.showCaves = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Beacons" -> this.showWaypointBeacons = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Signs" -> this.showWaypointSigns = Boolean.parseBoolean(curLine[1]);
                        case "Move Scoreboard Below Map" -> this.moveScoreboardBelowMap = Boolean.parseBoolean(curLine[1]);
                        case "Move Map Below Status Effect Icons" -> this.moveMapBelowStatusEffectIcons = Boolean.parseBoolean(curLine[1]);
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
                        case "Waypoint Icon Size" -> this.waypointIconSize = Math.max(0.5F, Math.min(2.0F, Float.parseFloat(curLine[1])));
                        case "Deathpoints" -> this.deathpoints = Math.max(0, Math.min(2, Integer.parseInt(curLine[1])));
                        case "Auto Unit Conversion" -> this.autoUnitConversion = Boolean.parseBoolean(curLine[1]);
                        case "Show Waypoint Names" -> this.showWaypointNames = Math.max(0, Math.min(2, Integer.parseInt(curLine[1])));
                        case "Show Waypoint Distances" -> this.showWaypointDistances = Math.max(0, Math.min(2, Integer.parseInt(curLine[1])));
                        case "Waypoint Font Size" -> this.waypointFontSize = Math.max(0.75F, Math.min(2.0F, Float.parseFloat(curLine[1])));
                        case "Show Waypoint Names On Map" -> this.showWaypointNamesOnMap = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Sort By" -> this.sort = Math.max(1, Math.min(4, Integer.parseInt(curLine[1])));
                        case "Zoom In Key" -> this.bindKey(this.keyBindZoomIn, curLine[1]);
                        case "Zoom Out Key" -> this.bindKey(this.keyBindZoomOut, curLine[1]);
                        case "Enlarged Map Key" -> this.bindKey(this.keyBindEnlargedMap, curLine[1]);
                        case "Fullscreen Key" -> this.bindKey(this.keyBindFullscreenMap, curLine[1]);
                        case "Menu Key" -> this.bindKey(this.keyBindMenu, curLine[1]);
                        case "Waypoint Menu Key" -> this.bindKey(this.keyBindWaypointMenu, curLine[1]);
                        case "Waypoint Key" -> this.bindKey(this.keyBindWaypoint, curLine[1]);
                        case "Mob Toggle Key" -> this.bindKey(this.keyBindMobToggle, curLine[1]);
                        case "Waypoint Toggle Key" -> this.bindKey(this.keyBindWaypointToggle, curLine[1]);
                        case "Welcome Message" -> this.welcome = Boolean.parseBoolean(curLine[1]);
                        case "Zoom Level" -> this.zoom = Math.max(0, Math.min(4, Integer.parseInt(curLine[1])));
                    }
                }
                KeyMapping.resetMapping();
                for (ISubSettingsManager subSettingsManager : this.subSettingsManagers) {
                    subSettingsManager.loadAll(this.settingsFile);
                }

                in.close();
            }

            this.saveAll();
        } catch (IOException exception) {
            VoxelConstants.getLogger().error(exception);
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
            out.println("Square Map:" + this.squareMap);
            out.println("Map Size:" + this.sizeModifier);
            out.println("Rotation:" + this.rotates);
            out.println("Map Corner:" + this.mapCorner);
            out.println("Enable Cave Mode:" + this.showCaves);
            out.println("Waypoint Beacons:" + this.showWaypointBeacons);
            out.println("Waypoint Signs:" + this.showWaypointSigns);
            out.println("Move Scoreboard Below Map" + this.moveScoreboardBelowMap);
            out.println("Move Map Below Status Effect Icons" + this.moveMapBelowStatusEffectIcons);
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
            out.println("Waypoint Icon Size:" + this.waypointIconSize);
            out.println("Deathpoints:" + this.deathpoints);
            out.println("Auto Unit Conversion:" + this.autoUnitConversion);
            out.println("Show Waypoint Names:" + this.showWaypointNames);
            out.println("Show Waypoint Distances:" + this.showWaypointDistances);
            out.println("Waypoint Font Size:" + this.waypointFontSize);
            out.println("Show Waypoint Names On Map" + this.showWaypointNamesOnMap);
            out.println("Waypoint Sort By:" + this.sort);
            out.println("Zoom In Key:" + this.keyBindZoomIn.saveString());
            out.println("Zoom Out Key:" + this.keyBindZoomOut.saveString());
            out.println("Enlarged Map Key:" + this.keyBindEnlargedMap.saveString());
            out.println("Fullscreen Key:" + this.keyBindFullscreenMap.saveString());
            out.println("Menu Key:" + this.keyBindMenu.saveString());
            out.println("Waypoint Menu Key:" + this.keyBindWaypointMenu.saveString());
            out.println("Waypoint Key:" + this.keyBindWaypoint.saveString());
            out.println("Mob Toggle Key:" + this.keyBindMobToggle.saveString());
            out.println("Waypoint Toggle Key:" + this.keyBindWaypointToggle.saveString());
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
    public String getKeyText(EnumOptionsMinimap option) {
        String name = I18n.get(option.getName()) + ": ";

        if (option.isBoolean()) {
            boolean flag = this.getBooleanValue(option);
            return flag ? name + I18n.get("options.on") : name + I18n.get("options.off");
        } else if (option.isList()) {
            String state = this.getListValue(option);
            return name + state;
        } else if (option.isFloat()) {
            float value = this.getFloatValue(option);
            return switch (option) {
                case WAYPOINT_DISTANCE -> value < 0.0F ? name + I18n.get("options.voxelmap.waypoints.infinite") : name + (int) value;
                case WAYPOINT_ICON_SIZE, WAYPOINT_FONT_SIZE -> name + value + "x";
                case ZOOM_LEVEL -> name + (int) value;
                default -> name + value;

                //value == 0.0F ? name + I18n.get("options.off") : name + (int) value + "%";
            };
        } else {
            return name;
        }
    }

    @Override
    public boolean getBooleanValue(EnumOptionsMinimap option) {
        return switch (option) {
            case HIDE_MINIMAP -> this.hide || !this.minimapAllowed;
            case OLD_NORTH -> this.oldNorth;
            case SHOW_BIOME_LABEL -> this.showBiomeLabel;
            case SQUAREMAP -> this.squareMap;
            case ROTATES -> this.rotates;
            case CAVE_MODE -> this.cavesAllowed && this.showCaves;
            case MOVE_SCOREBOARD_BELOW_MAP -> this.moveScoreboardBelowMap;
            case MOVE_MAP_BELOW_STATUS_EFFECT_ICONS -> this.moveMapBelowStatusEffectIcons;
            case DYNAMIC_LIGHTING -> this.lightmap;
            case FILTERING -> this.filtering;
            case WATER_TRANSPARENCY -> this.waterTransparency;
            case BLOCK_TRANSPARENCY -> this.blockTransparency;
            case BIOME_TINT -> this.biomes;
            case CHUNK_GRID -> this.chunkGrid;
            case SLIME_CHUNKS -> this.slimeChunks;
            case WORLD_BORDER -> this.worldborder;
            case AUTO_UNIT_CONVERSION -> this.autoUnitConversion;
            case SHOW_WAYPOINT_NAMES_ON_MAP -> this.showWaypointNamesOnMap;
            case WELCOME_SCREEN -> this.welcome;
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        };
    }

    @Override
    public String getListValue(EnumOptionsMinimap option) {
        switch (option) {
            case SHOW_COORDINATES -> {
                if (this.coordsMode == 0) {
                    return I18n.get("options.off");
                } else if (this.coordsMode == 1) {
                    return I18n.get("options.voxelmap.show_coordinates.mode1");
                } else if (this.coordsMode == 2) {
                    return I18n.get("options.voxelmap.show_coordinates.mode2");
                }

                return I18n.get("voxelmap.ui.error");
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
                } else if (this.sizeModifier == 4) {
                    return I18n.get("options.voxelmap.size.xxxl");
                }

                return I18n.get("voxelmap.ui.error");
            }
            case LOCATION -> {
                if (this.mapCorner == 0) {
                    return I18n.get("options.voxelmap.location.top_left");
                } else if (this.mapCorner == 1) {
                    return I18n.get("options.voxelmap.location.top_right");
                } else if (this.mapCorner == 2) {
                    return I18n.get("options.voxelmap.location.bottom_right");
                } else  if (this.mapCorner == 3) {
                    return I18n.get("options.voxelmap.location.bottom_left");
                }

                return I18n.get("voxelmap.ui.error");
            }
            case INGAME_WAYPOINTS -> {
                if (this.waypointsAllowed && this.showWaypointBeacons && this.showWaypointSigns) {
                    return I18n.get("options.voxelmap.ingame_waypoints.both");
                } else if (this.waypointsAllowed && this.showWaypointBeacons) {
                    return I18n.get("options.voxelmap.ingame_waypoints.beacons");
                } else if (this.waypointsAllowed && this.showWaypointSigns) {
                    return I18n.get("options.voxelmap.ingame_waypoints.signs");
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
                    return I18n.get("options.voxelmap.biome_overlay.solid");
                } else if (this.biomeOverlay == 2) {
                    return I18n.get("options.voxelmap.biome_overlay.transparent");
                }

                return I18n.get("voxelmap.ui.error");
            }
            case SHOW_WAYPOINT_NAMES -> {
                if (this.showWaypointNames == 0) {
                    return I18n.get("options.off");
                } else if (this.showWaypointNames == 1) {
                    return I18n.get("options.voxelmap.waypoints.show_waypoint_names.aboveicon");
                } else if (this.showWaypointNames == 2) {
                    return I18n.get("options.voxelmap.waypoints.show_waypoint_names.below_icon");
                }

                return I18n.get("voxelmap.ui.error");
            }
            case SHOW_WAYPOINT_DISTANCES -> {
                if (this.showWaypointDistances == 0) {
                    return I18n.get("options.off");
                } else if (this.showWaypointDistances == 1) {
                    return this.showWaypointNames == 0 ? I18n.get("options.voxelmap.waypoints.show_waypoint_distances.above_icon") : I18n.get("options.voxelmap.waypoints.show_waypoint_distances.beside_name");
                } else if (this.showWaypointDistances == 2) {
                    return this.showWaypointNames == 0 ? I18n.get("options.voxelmap.waypoints.show_waypoint_distances.below_icon") : I18n.get("options.voxelmap.waypoints.show_waypoint_distances.below_name");
                }

                return I18n.get("voxelmap.ui.error");
            }
            case DEATHPOINTS -> {
                if (this.deathpoints == 0) {
                    return I18n.get("options.off");
                } else if (this.deathpoints == 1) {
                    return I18n.get("options.voxelmap.waypoints.deathpoints.most_recent");
                } else if (this.deathpoints == 2) {
                    return I18n.get("options.voxelmap.waypoints.deathpoints.all");
                }

                return I18n.get("voxelmap.ui.error");
            }
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        }
    }

    @Override
    public float getFloatValue(EnumOptionsMinimap option) {
        return switch (option) {
            case WAYPOINT_DISTANCE -> this.maxWaypointDisplayDistance;
            case WAYPOINT_ICON_SIZE -> this.waypointIconSize;
            case WAYPOINT_FONT_SIZE -> this.waypointFontSize;
            case ZOOM_LEVEL -> this.zoom;
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        };
    }

    @Override
    public void setValue(EnumOptionsMinimap option) {
        switch (option) {
            case HIDE_MINIMAP -> this.hide = !this.hide;
            case OLD_NORTH -> this.oldNorth = !this.oldNorth;
            case SHOW_BIOME_LABEL -> this.showBiomeLabel = !this.showBiomeLabel;
            case SQUAREMAP -> this.squareMap = !this.squareMap;
            case ROTATES -> this.rotates = !this.rotates;
            case CAVE_MODE -> this.showCaves = !this.showCaves;
            case MOVE_SCOREBOARD_BELOW_MAP -> this.moveScoreboardBelowMap = !this.moveScoreboardBelowMap;
            case MOVE_MAP_BELOW_STATUS_EFFECT_ICONS -> this.moveMapBelowStatusEffectIcons = !this.moveMapBelowStatusEffectIcons;
            case DYNAMIC_LIGHTING -> this.lightmap = !this.lightmap;
            case FILTERING -> this.filtering = !this.filtering;
            case WATER_TRANSPARENCY -> this.waterTransparency = !this.waterTransparency;
            case BLOCK_TRANSPARENCY -> this.blockTransparency = !this.blockTransparency;
            case BIOME_TINT -> this.biomes = !this.biomes;
            case CHUNK_GRID -> this.chunkGrid = !this.chunkGrid;
            case SLIME_CHUNKS -> this.slimeChunks = !this.slimeChunks;
            case WORLD_BORDER -> this.worldborder = !this.worldborder;
            case AUTO_UNIT_CONVERSION -> this.autoUnitConversion = !this.autoUnitConversion;
            case SHOW_WAYPOINT_NAMES_ON_MAP -> this.showWaypointNamesOnMap = !this.showWaypointNamesOnMap;
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
            case SHOW_WAYPOINT_NAMES -> {
                ++this.showWaypointNames;
                if (this.showWaypointNames > 2) {
                    this.showWaypointNames = 0;
                }
            }
            case SHOW_WAYPOINT_DISTANCES -> {
                ++this.showWaypointDistances;
                if (this.showWaypointDistances > 2) {
                    this.showWaypointDistances = 0;
                }
            }
            case DEATHPOINTS -> {
                ++this.deathpoints;
                if (this.deathpoints > 2) {
                    this.deathpoints = 0;
                }
            }
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        }

        this.somethingChanged = true;
    }

    @Override
    public void setFloatValue(EnumOptionsMinimap option, float value) {
        switch (option) {
            case WAYPOINT_DISTANCE -> {
                float distance = value * 9951.0F + 50.0F;
                if (distance > 10000.0F) {
                    distance = -1.0F;
                }

                this.maxWaypointDisplayDistance = (int) distance;
            }
            case WAYPOINT_ICON_SIZE -> {
                value = Math.round(value * 12.0F) / 12.0F;
                this.waypointIconSize = (value * 1.5F) + 0.5F;
            }
            case WAYPOINT_FONT_SIZE -> {
                value = Math.round(value * 10.0F) / 10.0F;
                this.waypointFontSize = (value * 1.25F) + 0.75F;
            }
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        }

        this.somethingChanged = true;
    }

    private void bindKey(KeyMapping keyBinding, String id) {
        try {
            keyBinding.setKey(InputConstants.getKey(id));
        } catch (RuntimeException var4) {
            VoxelConstants.getLogger().warn(id + " is not a valid keybinding");
        }

    }

    public void setKeyBinding(KeyMapping keyBinding, InputConstants.Key input) {
        keyBinding.setKey(input);
        this.saveAll();
    }

    public Component getKeybindDisplayString(KeyMapping keyBinding) {
        return keyBinding.getTranslatedKeyMessage();
    }

    public Component getKeybindDisplayString(int keybindIndex) {
        KeyMapping keyBinding = this.keyBindings[keybindIndex];
        return this.getKeybindDisplayString(keyBinding);
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

    public void setWaypointSort(int sort) {
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
