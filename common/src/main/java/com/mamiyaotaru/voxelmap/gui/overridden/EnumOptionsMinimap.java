package com.mamiyaotaru.voxelmap.gui.overridden;

public enum EnumOptionsMinimap {
    HIDE_MINIMAP("options.voxelmap.hideminimap", false, true, false),
    SHOW_COORDINATES("options.voxelmap.showcoordinates", false, false, true),
    OLD_NORTH("options.voxelmap.oldnorth", false, true, false),
    SHOW_BIOME_LABEL("options.voxelmap.showbiomelabel", false, true, false),
    SIZE("options.voxelmap.size", false, false, true),
    SQUAREMAP("options.voxelmap.squaremap", false, true, false),
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
    SHOW_MOBS("options.voxelmap.radar.showmobs", false, false, true),
    SHOW_MOB_NAMES("options.voxelmap.radar.showmobnames", false, true, false),
    SHOW_MOB_HELMETS("options.voxelmap.radar.showmobhelmets", false, true, false),
    SHOW_PLAYERS("options.voxelmap.radar.showplayers", false, true, false),
    SHOW_PLAYER_NAMES("options.voxelmap.radar.showplayernames", false, true, false),
    SHOW_PLAYER_HELMETS("options.voxelmap.radar.showplayerhelmets", false, true, false),
    RADAR_FONT_SCALE("options.voxelmap.radar.fontsize", true, false, false),
    SHOW_FACING("options.voxelmap.radar.showfacing", false, true, false),
    ICON_OUTLINES("options.voxelmap.radar.iconoutlines", false, true, false),
    ICON_FILTERING("options.voxelmap.radar.iconfiltering", false, true, false),

    WAYPOINT_DISTANCE("options.voxelmap.waypoints.distance", true, false, false),
    WAYPOINT_SIZE("options.voxelmap.waypoints.iconsize", true, false, false),
    DEATHPOINTS("options.voxelmap.waypoints.deathpoints", false, false, true),
    AUTO_UNIT_CONVERSION("options.voxelmap.waypoints.autounitconversion", false, true, false),
    SHOW_WAYPOINT_NAME("options.voxelmap.waypoints.shownamelabel", false, false, true),
    SHOW_WAYPOINT_DISTANCE("options.voxelmap.waypoints.showdistancelabel", false, false, true),

    WORLDMAP_MIN_ZOOM("options.voxelmap.worldmap.minzoom", true, false, false),
    WORLDMAP_MAX_ZOOM("options.voxelmap.worldmap.maxzoom", true, false, false),
    WORLDMAP_CACHE_SIZE("options.voxelmap.worldmap.cachesize", true, false, false),
    WORLDMAP_SHOW_WAYPOINTS("options.voxelmap.worldmap.showwaypoints", false, true, false),
    WORLDMAP_SHOW_WAYPOINT_NAMES("options.voxelmap.worldmap.showwaypointnames", false, true, false),

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
