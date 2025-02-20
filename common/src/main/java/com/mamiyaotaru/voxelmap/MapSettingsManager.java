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

public class MapSettingsManager implements ISettingsManager {
    private File settingsFile;
    public boolean showUnderMenus;
    private final int availableProcessors = Runtime.getRuntime().availableProcessors();
    public final boolean multicore = this.availableProcessors > 1;

    public int displayMode = 3;
    public int coordsMode = 1;
    public int sizeModifier = 1;
    public int shape = 1;
    public boolean rotates = true;
    public int mapCorner = 1;
    protected boolean showCaves = true;
    public boolean showBeacons;
    public boolean showWaypoints = true;
    private boolean preToggleSigns = true;
    private boolean preToggleBeacons;
    public boolean moveScoreBoardDown = true;
    public boolean moveMapDownWhileStatusEffect = true;

    public boolean lightmap = true;
    public boolean slopemap = true;
    public boolean heightmap = this.multicore;
    public boolean filtering = false;
    public boolean waterTransparency = this.multicore;
    public boolean blockTransparency = this.multicore;
    public boolean biomes = this.multicore;
    public int biomeOverlay = 0;
    public boolean chunkGrid = false;
    public boolean slimeChunks = false;
    public boolean worldborder = true;
    public String teleportCommand = "tp %p %x %y %z";
    public String serverTeleportCommand;

    public int waypointSort = 1;
    public int maxWaypointDisplayDistance = -1;
    public float waypointSize = 1f;
    public int deathpoints = 1;
    public boolean distanceUnitConversion = true;
    public boolean waypointNameBelowIcon = true;
    public boolean waypointDistanceBelowName = true;

    public int zoom = 2;
    protected boolean welcome = true;
    public boolean oldNorth = false;

    public Boolean cavesAllowed = true;
    public boolean worldmapAllowed = true;
    public boolean minimapAllowed = true;
    public boolean waypointsAllowed = true;
    public boolean deathWaypointAllowed = true;

    public final KeyMapping keyBindMenu = new KeyMapping("key.minimap.voxelmapmenu", InputConstants.getKey("key.keyboard.m").getValue(), "controls.minimap.title");
    public final KeyMapping keyBindWaypointMenu = new KeyMapping("key.minimap.waypointmenu", InputConstants.getKey("key.keyboard.u").getValue(), "controls.minimap.title");
    public final KeyMapping keyBindZoom = new KeyMapping("key.minimap.zoomin", InputConstants.getKey("key.keyboard.up").getValue(), "controls.minimap.title");
    public final KeyMapping keyBindZoomOut = new KeyMapping("key.minimap.zoomout", InputConstants.getKey("key.keyboard.down").getValue(), "controls.minimap.title");
    public final KeyMapping keyBindFullscreen = new KeyMapping("key.minimap.togglefullscreen", InputConstants.getKey("key.keyboard.z").getValue(), "controls.minimap.title");
    public final KeyMapping keyBindWaypoint = new KeyMapping("key.minimap.waypointhotkey", InputConstants.getKey("key.keyboard.n").getValue(), "controls.minimap.title");
    public final KeyMapping keyBindWaypointToggle = new KeyMapping("key.minimap.toggleingamewaypoints", -1, "controls.minimap.title");
    public final KeyMapping keyBindRadarToggle = new KeyMapping("key.minimap.togglemobs", -1, "controls.minimap.title");
    public final KeyMapping keyBindListAlternative = new KeyMapping("key.minimap.listalternative", -1, "controls.minimap.title");
    public final KeyMapping[] keyBindings;

    private boolean somethingChanged;
    public static MapSettingsManager instance;
    private final List<ISubSettingsManager> subSettingsManagers = new ArrayList<>();

    public MapSettingsManager() {
        instance = this;
        this.keyBindings = new KeyMapping[]{ this.keyBindMenu, this.keyBindWaypointMenu, this.keyBindZoom, this.keyBindZoomOut, this.keyBindFullscreen, this.keyBindWaypoint, this.keyBindWaypointToggle, this.keyBindRadarToggle, this.keyBindListAlternative };
    }

    public void addSecondaryOptionsManager(ISubSettingsManager secondarySettingsManager) {
        this.subSettingsManagers.add(secondarySettingsManager);
    }

    public void loadAll() {
        this.settingsFile = new File(VoxelConstants.getMinecraft().gameDirectory, "config/voxelmap.properties");

        try {
            if (this.settingsFile.exists()) {
                BufferedReader in;
                String sCurrentLine;
                for (in = new BufferedReader(new InputStreamReader(new FileInputStream(this.settingsFile), StandardCharsets.UTF_8.newDecoder())); (sCurrentLine = in.readLine()) != null; KeyMapping.resetMapping()) {
                    String[] curLine = sCurrentLine.split(":");
                    switch (curLine[0]) {
                        case "Display Mode" -> this.displayMode = Math.max(0, Math.min(3, Integer.parseInt(curLine[1])));
                        case "Coordinates Mode" -> this.coordsMode = Math.max(0, Math.min(2, Integer.parseInt(curLine[1])));
                        case "Map Size" -> this.sizeModifier = Math.max(-1, Math.min(4, Integer.parseInt(curLine[1])));
                        case "Map Shape" -> this.shape = Math.max(0, Math.min(1, Integer.parseInt(curLine[1])));
                        case "Map Rotating" -> this.rotates = Boolean.parseBoolean(curLine[1]);
                        case "Map Corner" -> this.mapCorner = Math.max(0, Math.min(3, Integer.parseInt(curLine[1])));
                        case "Enable Cave Mode" -> this.showCaves = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Signs" -> this.showWaypoints = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Beacons" -> this.showBeacons = Boolean.parseBoolean(curLine[1]);
                        case "Move ScoreBoard Down" -> this.moveScoreBoardDown = Boolean.parseBoolean(curLine[1]);
                        case "Move Map Down While Status Effect" -> this.moveMapDownWhileStatusEffect = Boolean.parseBoolean(curLine[1]);

                        case "Dynamic Lighting" -> this.lightmap = Boolean.parseBoolean(curLine[1]);
                        case "Slope Map" -> this.slopemap = Boolean.parseBoolean(curLine[1]);
                        case "Height Map" -> this.heightmap = Boolean.parseBoolean(curLine[1]);
                        case "Filter Map" -> this.filtering = Boolean.parseBoolean(curLine[1]);
                        case "Water Transparency" -> this.waterTransparency = Boolean.parseBoolean(curLine[1]);
                        case "Block Transparency" -> this.blockTransparency = Boolean.parseBoolean(curLine[1]);
                        case "Biome Tint" -> this.biomes = Boolean.parseBoolean(curLine[1]);
                        case "Biome Overlay" -> this.biomeOverlay = Math.max(0, Math.min(2, Integer.parseInt(curLine[1])));
                        case "Chunk Grid" -> this.chunkGrid = Boolean.parseBoolean(curLine[1]);
                        case "Slime Chunks" -> this.slimeChunks = Boolean.parseBoolean(curLine[1]);
                        case "World Border" -> this.worldborder = Boolean.parseBoolean(curLine[1]);
                        case "Teleport Command" -> this.teleportCommand = curLine[1];

                        case "Waypoint Sort By" -> this.waypointSort = Math.max(1, Math.min(4, Integer.parseInt(curLine[1])));
                        case "Waypoint Max Distance" -> this.maxWaypointDisplayDistance = Math.max(-1, Math.min(10000, Integer.parseInt(curLine[1])));
                        case "Waypoint Size" -> this.waypointSize = Math.max(0.5f, Math.min(1.5f, Float.parseFloat(curLine[1])));
                        case "Deathpoints" -> this.deathpoints = Math.max(0, Math.min(2, Integer.parseInt(curLine[1])));
                        case "Distance Unit Conversion" -> this.distanceUnitConversion = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Name Below Icon" -> this.waypointNameBelowIcon = Boolean.parseBoolean(curLine[1]);
                        case "Waypoint Distance Below Name" -> this.waypointDistanceBelowName  = Boolean.parseBoolean(curLine[1]);

                        case "Menu Key" -> this.bindKey(this.keyBindMenu, curLine[1]);
                        case "Waypoint Menu Key" -> this.bindKey(this.keyBindWaypointMenu, curLine[1]);
                        case "Zoom Key" -> this.bindKey(this.keyBindZoom, curLine[1]);
                        case "Fullscreen Key" -> this.bindKey(this.keyBindFullscreen, curLine[1]);
                        case "Waypoint Key" -> this.bindKey(this.keyBindWaypoint, curLine[1]);
                        case "In-game Waypoint Key" -> this.bindKey(this.keyBindWaypointToggle, curLine[1]);
                        case "Radar Key" -> this.bindKey(this.keyBindRadarToggle, curLine[1]);
                        case "Dynamic Rader Key" -> this.bindKey(this.keyBindListAlternative, curLine[1]);

                        case "Zoom Level" -> this.zoom = Math.max(0, Math.min(4, Integer.parseInt(curLine[1])));
                        case "Welcome Message" -> this.welcome = Boolean.parseBoolean(curLine[1]);
                        case "Old North" -> this.oldNorth = Boolean.parseBoolean(curLine[1]);

                        //case "Real Time Torch Flicker" -> this.realTimeTorches = Boolean.parseBoolean(curLine[1]);
                    }
                }

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

            out.println("Display Mode:" + this.displayMode);
            out.println("Coordinates Mode:" + this.coordsMode);
            out.println("Map Size:" + this.sizeModifier);
            out.println("Map Shape:" + this.shape);
            out.println("Map Rotating:" + this.rotates);
            out.println("Map Corner:" + this.mapCorner);
            out.println("Enable Cave Mode:" + this.showCaves);
            out.println("Waypoint Signs:" + this.showWaypoints);
            out.println("Waypoint Beacons:" + this.showBeacons);
            out.println("Move ScoreBoard Down:" + this.moveScoreBoardDown);
            out.println("Move Map Down While Status Effect:" + this.moveMapDownWhileStatusEffect);

            out.println("Dynamic Lighting:" + this.lightmap);
            out.println("Slope Map:" + this.slopemap);
            out.println("Height Map:" + this.heightmap);
            out.println("Filter Map:" + this.filtering);
            out.println("Water Transparency:" + this.waterTransparency);
            out.println("Block Transparency:" + this.blockTransparency);
            out.println("Biome Tint" + this.biomes);
            out.println("Biome Overlay:" + this.biomeOverlay);
            out.println("Chunk Grid:" + this.chunkGrid);
            out.println("Slime Chunks:" + this.slimeChunks);
            out.println("World Border:" + this.worldborder);
            out.println("Teleport Command:" + this.teleportCommand);

            out.println("Waypoint Sort By:" + this.waypointSort);
            out.println("Waypoint Max Distance:" + this.maxWaypointDisplayDistance);
            out.println("Waypoint Size: " + this.waypointSize);
            out.println("Deathpoints:" + this.deathpoints);
            out.println("Distance Unit Conversion:" + this.distanceUnitConversion);
            out.println("Waypoint Name Below Icon:" + this.waypointNameBelowIcon);
            out.println("Waypoint Distance Below Name:" + this.waypointDistanceBelowName);

            out.println("Menu Key:" + this.keyBindMenu.saveString());
            out.println("Waypoint Menu Key:" + this.keyBindWaypointMenu.saveString());
            out.println("Zoom Key:" + this.keyBindZoom.saveString());
            out.println("Fullscreen Key:" + this.keyBindFullscreen.saveString());
            out.println("Waypoint Key:" + this.keyBindWaypoint.saveString());
            out.println("In-game Waypoint Key:" + this.keyBindWaypointToggle.saveString());
            out.println("Radar Key:" + this.keyBindRadarToggle.saveString());
            out.println("Dynamic Radar Key" + this.keyBindListAlternative.saveString());

            out.println("Zoom Level:" + this.zoom);
            out.println("Welcome Message:" + this.welcome);
            out.println("Old North:" + this.oldNorth);

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
            switch (options){
                case ZOOMLEVEL ->{
                    return s + (int) f;
                }
                case WAYPOINTDISTANCE -> {
                    return f < 0.0F ? s + I18n.get("options.minimap.waypoints.infinite") : s + (int) f;
                }
                case WAYPOINTSIZE -> {
                    return s + (int) (f * 100) + "%";
                }
                default -> {
                    return f == 0.0F ? s + I18n.get("options.off") : s + (int) f + "%";
                }
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
        switch (options){
            case ZOOMLEVEL -> {
                return this.zoom;
            }
            case WAYPOINTDISTANCE -> {
                return this.maxWaypointDisplayDistance;
            }
            case WAYPOINTSIZE -> {
                return this.waypointSize;
            }
            default -> {
                return 0.0f;
            }
        }
    }

    public boolean getOptionBooleanValue(EnumOptionsMinimap par1EnumOptions) {
        return switch (par1EnumOptions) {
            case ROTATES -> this.rotates;
            case CAVEMODE -> this.cavesAllowed && this.showCaves;
            case MOVESCOREBOARDDOWN -> this.moveScoreBoardDown;
            case MOVEMAPDOWNWHILESTATSUEFFECT -> this.moveMapDownWhileStatusEffect;

            case LIGHTING -> this.lightmap;
            case FILTERING -> this.filtering;
            case WATERTRANSPARENCY -> this.waterTransparency;
            case BLOCKTRANSPARENCY -> this.blockTransparency;
            case BIOMETINT -> this.biomes;
            case CHUNKGRID -> this.chunkGrid;
            case SLIMECHUNKS -> this.slimeChunks;
            case WORLDBORDER -> this.worldborder;

            case DISTANCEUNITCONVERSION -> this.distanceUnitConversion;
            case WAYPOINTNAMEBELOWICON -> this.waypointNameBelowIcon;
            case WAYPOINTDISTANCEBELOWNAME -> this.waypointDistanceBelowName;

            case WELCOME -> this.welcome;
            case OLDNORTH -> this.oldNorth;

            default ->
                    throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName() + ". (possibly not a boolean applicable to minimap)");
        };
    }

    public String getOptionListValue(EnumOptionsMinimap par1EnumOptions) {
        switch (par1EnumOptions) {
            case DISPLAY -> {
                if (this.displayMode == 0 || !this.minimapAllowed){
                    return I18n.get("options.off");
                } else if (this.displayMode == 1){
                    return I18n.get("options.minimap.display.minimap");
                } else if (this.displayMode == 2){
                    return I18n.get("options.minimap.display.fullmap");
                } else {
                    if (this.displayMode == 3){
                        return I18n.get("options.minimap.display.both");
                    }
                    return "error";
                }
            }
            case COORDS -> {
                if (this.coordsMode == 0){
                    return I18n.get("options.off");
                } else if (this.coordsMode == 1){
                    return I18n.get("options.minimap.showcoordinates.mode1");
                } else {
                    if (this.coordsMode == 2){
                        return I18n.get("options.minimap.showcoordinates.mode2");
                    }
                    return "error";
                }
            }
            case SIZE -> {
                if (this.sizeModifier == -1) {
                    return I18n.get("options.minimap.size.small");
                } else if (this.sizeModifier == 0) {
                    return I18n.get("options.minimap.size.medium");
                } else if (this.sizeModifier == 1) {
                    return I18n.get("options.minimap.size.large");
                } else if (this.sizeModifier == 2) {
                    return I18n.get("options.minimap.size.xl");
                } else if (this.sizeModifier == 3) {
                    return I18n.get("options.minimap.size.xxl");
                } else {
                    if (this.sizeModifier == 4) {
                        return I18n.get("options.minimap.size.xxxl");
                    }
                    return "error";
                }
            }
            case SHAPE -> {
                if (this.shape == 0) {
                    return I18n.get("options.minimap.shape.round");
                } else {
                    if (this.shape == 1){
                        return I18n.get("options.minimap.shape.squre");
                    }
                    return "error";
                }
            }
            case LOCATION -> {
                if (this.mapCorner == 0) {
                    return I18n.get("options.minimap.location.topleft");
                } else if (this.mapCorner == 1) {
                    return I18n.get("options.minimap.location.topright");
                } else if (this.mapCorner == 2) {
                    return I18n.get("options.minimap.location.bottomright");
                } else {
                    if (this.mapCorner == 3) {
                        return I18n.get("options.minimap.location.bottomleft");
                    }
                    return "Error";
                }
            }
            case INGAMEWAYPOINTS -> {
                if (this.waypointsAllowed && this.showBeacons && this.showWaypoints) {
                    return I18n.get("options.minimap.ingamewaypoints.both");
                } else if (this.waypointsAllowed && this.showBeacons) {
                    return I18n.get("options.minimap.ingamewaypoints.beacons");
                } else if (this.waypointsAllowed && this.showWaypoints) {
                    return I18n.get("options.minimap.ingamewaypoints.signs");
                }
                return I18n.get("options.off");
            }
            case TERRAINDEPTH -> {
                if (this.slopemap && this.heightmap) {
                    return I18n.get("options.minimap.terrain.both");
                } else if (this.heightmap) {
                    return I18n.get("options.minimap.terrain.height");
                } else if (this.slopemap) {
                    return I18n.get("options.minimap.terrain.slope");
                }
                return I18n.get("options.off");
            }
            case BIOMEOVERLAY -> {
                if (this.biomeOverlay == 0) {
                    return I18n.get("options.off");
                } else if (this.biomeOverlay == 1) {
                    return I18n.get("options.minimap.biomeoverlay.solid");
                } else {
                    if (this.biomeOverlay == 2) {
                        return I18n.get("options.minimap.biomeoverlay.transparent");
                    }
                    return "error";
                }
            }
            case DEATHPOINTS -> {
                if (this.deathpoints == 0) {
                    return I18n.get("options.off");
                } else if (this.deathpoints == 1) {
                    return I18n.get("options.minimap.waypoints.deathpoints.mostrecent");
                } else {
                    if (this.deathpoints == 2) {
                        return I18n.get("options.minimap.waypoints.deathpoints.all");
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
        switch (options){
            case WAYPOINTDISTANCE -> {
                float distance = value * 9951.0F + 50.0F;
                if (distance > 10000.0F) {
                    distance = -1.0F;
                }
                this.maxWaypointDisplayDistance = (int) distance;
            }
            case WAYPOINTSIZE -> {
                this.waypointSize = Math.round((0.5f + value) * 20.0f) / 20.0f;
            }
        }

        this.somethingChanged = true;
    }

    public void setOptionValue(EnumOptionsMinimap par1EnumOptions) {
        switch (par1EnumOptions) {
            case ROTATES -> this.rotates = !this.rotates;
            case CAVEMODE -> this.showCaves = !this.showCaves;
            case MOVESCOREBOARDDOWN -> this.moveScoreBoardDown = !this.moveScoreBoardDown;
            case MOVEMAPDOWNWHILESTATSUEFFECT -> this.moveMapDownWhileStatusEffect = !this.moveMapDownWhileStatusEffect;

            case LIGHTING -> this.lightmap = !this.lightmap;
            case FILTERING -> this.filtering = !this.filtering;
            case WATERTRANSPARENCY -> this.waterTransparency = !this.waterTransparency;
            case BLOCKTRANSPARENCY -> this.blockTransparency = !this.blockTransparency;
            case BIOMETINT -> this.biomes = !this.biomes;
            case CHUNKGRID -> this.chunkGrid = !this.chunkGrid;
            case SLIMECHUNKS -> this.slimeChunks = !this.slimeChunks;
            case WORLDBORDER -> this.worldborder = !this.worldborder;

            case DISTANCEUNITCONVERSION -> this.distanceUnitConversion = !this.distanceUnitConversion;
            case WAYPOINTNAMEBELOWICON -> this.waypointNameBelowIcon = !this.waypointNameBelowIcon;
            case WAYPOINTDISTANCEBELOWNAME -> this.waypointDistanceBelowName = !this.waypointDistanceBelowName;

            case WELCOME -> this.welcome = !this.welcome;
            case OLDNORTH -> this.oldNorth = !this.oldNorth;

            case DISPLAY -> {
                if (this.minimapAllowed){
                    this.displayMode++;
                    if (this.displayMode > 3){
                        this.displayMode = 0;
                    }
                } else {
                    this.displayMode = 0;
                }
            }

            case COORDS -> {
                this.coordsMode++;
                if (this.coordsMode > 2){
                    this.coordsMode = 0;
                }
            }

            case SIZE -> {
                this.sizeModifier++;
                if (this.sizeModifier > 4){
                    this.sizeModifier = -1;
                }
            }
            case SHAPE -> {
                this.shape++;
                if (this.shape > 1){
                    this.shape = 0;
                }
            }
            case LOCATION -> {
                this.mapCorner++;
                if (this.mapCorner > 3){
                    this.mapCorner = 0;
                }
            }
            case INGAMEWAYPOINTS -> {
                if (this.showBeacons && this.showWaypoints) {
                    this.showBeacons = false;
                    this.showWaypoints = false;
                } else if (this.showBeacons) {
                    this.showBeacons = false;
                    this.showWaypoints = true;
                } else if (this.showWaypoints) {
                    this.showBeacons = true;
                } else {
                    this.showBeacons = true;
                }
            }
            case BIOMEOVERLAY -> {
                ++this.biomeOverlay;
                if (this.biomeOverlay > 2) {
                    this.biomeOverlay = 0;
                }
            }
            case TERRAINDEPTH -> {
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
        if (!this.showBeacons && !this.showWaypoints) {
            this.showBeacons = this.preToggleBeacons;
            this.showWaypoints = this.preToggleSigns;
        } else {
            this.preToggleBeacons = this.showBeacons;
            this.preToggleSigns = this.showWaypoints;
            this.showBeacons = false;
            this.showWaypoints = false;
        }
    }

    public String getKeyBindingDescription(int keybindIndex) {
        return this.keyBindings[keybindIndex].getName().equals("key.minimap.voxelmapmenu") ? I18n.get("key.minimap.menu") : I18n.get(this.keyBindings[keybindIndex].getName());
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

    public void setWaypointSort(int waypointSort) {
        if (waypointSort != this.waypointSort && waypointSort != -this.waypointSort) {
            this.waypointSort = waypointSort;
        } else {
            this.waypointSort = -this.waypointSort;
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
