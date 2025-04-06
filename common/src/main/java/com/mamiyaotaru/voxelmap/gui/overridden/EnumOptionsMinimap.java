package com.mamiyaotaru.voxelmap.gui.overridden;

public enum EnumOptionsMinimap {
    HIDE_MINIMAP("options.voxelmap.hideminimap", false, true, false),
    SHOW_COORDINATES("options.voxelmap.showcoordinates", false, true, false),
    OLD_NORTH("options.voxelmap.oldnorth", false, true, false),
    SHOW_BIOME_LABEL("options.voxelmap.showbiomelabel", false, true, false),
    SIZE("options.voxelmap.size", false, false, true),
    SHAPE("options.voxelmap.shape", false, true, false),
    ROTATES("options.voxelmap.rotation", false, true, false),
    LOCATION("options.voxelmap.location", false, false, true),
    CAVE_MODE("options.voxelmap.cavemode", false, true, false),
    INGAME_WAYPOINTS("options.voxelmap.ingamewaypoints", false, false, true),
    MOVE_SCOREBOARD_BELOW_MAP("options.voxelmap.movescoreboardbelowmap", false, true, false),
    MOVE_MAP_BELOW_STATUS_EFFECT("options.voxelmap.movemapbelowstatuseffect", false, true, false),
    DYNAMIC_LIGHTING("options.voxelmap.dynamiclighting", false, true, false),
    TERRAIN_DEPTH("options.voxelmap.terraindepth", false, false, true),
    FILTERING("options.voxelmap.filtering", false, true, false),
    WATER_TRANSPARENCY("options.voxelmap.watertransparency", false, true, false),
    BLOCK_TRANSPARENCY("options.voxelmap.blocktransparency", false, true, false),
    BIOME_TINT("options.voxelmap.biomes", false, true, false),
    BIOME_OVERLAY("options.voxelmap.biomeoverlay", false, false, true),
    CHUNK_GRID("options.voxelmap.chunkgrid", false, true, false),
    SLIME_CHUNKS("options.voxelmap.slimechunks", false, true, false),
    WORLD_BORDER("options.voxelmap.worldborder", false, true, false),
    WORLD_SEED("World Seed", false, false, false),
    TELEPORT_COMMAND("Teleport Command", false, false, false),

    SHOW_RADAR("options.voxelmap.radar.showradar", false, true, false),
    RADAR_MODE("options.voxelmap.radar.radarmode", false, false, true),
    SHOW_NEUTRALS("options.voxelmap.radar.showneutrals", false, true, false),
    SHOW_HOSTILES("options.voxelmap.radar.showhostiles", false, true, false),
    SHOW_MOB_HELMETS("options.voxelmap.radar.showmobhelmets", false, true, false),
    SHOW_MOB_NAMES("options.voxelmap.radar.showmobnames", false, true, false),
    SHOW_PLAYERS("options.voxelmap.radar.showplayers", false, true, false),
    SHOW_PLAYER_HELMETS("options.voxelmap.radar.showplayerhelmets", false, true, false),
    SHOW_PLAYER_NAMES("options.voxelmap.radar.showplayernames", false, true, false),
    ICON_OUTLINES("options.voxelmap.radar.iconoutlines", false, true, false),
    ICON_FILTERING("options.voxelmap.radar.iconfiltering", false, true, false),
    SHOW_FACING("options.voxelmap.radar.showfacing", false, true, false),

    WAYPOINT_DISTANCE("options.voxelmap.waypoints.distance", true, false, false),
    DEATHPOINTS("options.voxelmap.waypoints.deathpoints", false, false, true),
    DISTANCE_UNIT_CONVERSION("options.voxelmap.waypoints.distanceunitconversion", false, true, false),
    NAME_LABEL_BELOW_ICON("options.voxelmap.waypoints.waypointnamebelowicon", false, true, false),
    DISTANCE_LABEL_BELOW_NAME("options.voxelmap.waypoints.waypointdistancebelowname", false, true, false),

    SHOW_WAYPOINTS("options.voxelmap.worldmap.showwaypoints", false, true, false),
    SHOW_WAYPOINT_NAMES("options.voxelmap.worldmap.showwaypointnames", false, true, false),
    MIN_ZOOM("options.voxelmap.worldmap.minzoom", true, false, false),
    MAX_ZOOM("options.voxelmap.worldmap.maxzoom", true, false, false),
    CACHE_SIZE("options.voxelmap.worldmap.cachesize", true, false, false),

    WELCOME_SCREEN("Welcome Screen", false, true, false),
    ZOOM_LEVEL("Zoom Level", false, true, false);

    private final boolean isFloat;
    private final boolean isBoolean;
    private final boolean isList;
    private final String name;

    EnumOptionsMinimap(String name, boolean isFloat, boolean isBoolean, boolean isList) {
        this.name = name;
        this.isFloat = isFloat;
        this.isBoolean = isBoolean;
        this.isList = isList;
    }

    public boolean isFloat() {
        return this.isFloat;
    }

    public boolean isBoolean() {
        return this.isBoolean;
    }

    public boolean isList() {
        return this.isList;
    }

    public String getName() {
        return this.name;
    }
}
