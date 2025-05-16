package com.mamiyaotaru.voxelmap.gui.overridden;

public enum EnumOptionsMinimap {
    HIDE_MINIMAP("options.voxelmap.hide_minimap", false, true, false),
    SHOW_COORDINATES("options.voxelmap.show_coordinates", false, false, true),
    OLD_NORTH("options.voxelmap.old_north", false, true, false),
    SHOW_BIOME_LABEL("options.voxelmap.display_biome_name", false, true, false),
    SQUAREMAP("options.voxelmap.square_map", false, true, false),
    SIZE("options.voxelmap.size", false, false, true),
    ROTATES("options.voxelmap.rotating", false, true, false),
    LOCATION("options.voxelmap.location", false, false, true),
    CAVE_MODE("options.voxelmap.cave_mode", false, true, false),
    INGAME_WAYPOINTS("options.voxelmap.ingame_waypoints", false, false, true),
    MOVE_SCOREBOARD_BELOW_MAP("options.voxelmap.move_scoreboard_below_map", false, true, false),
    MOVE_MAP_BELOW_STATUS_EFFECT_ICONS("options.voxelmap.move_map_below_status_effect_icons", false, true, false),
    DYNAMIC_LIGHTING("options.voxelmap.dynamic_lighting", false, true, false),
    TERRAIN_DEPTH("options.voxelmap.terrain_depth", false, false, true),
    FILTERING("options.voxelmap.filtering", false, true, false),
    WATER_TRANSPARENCY("options.voxelmap.water_transparency", false, true, false),
    BLOCK_TRANSPARENCY("options.voxelmap.block_transparency", false, true, false),
    BIOME_TINT("options.voxelmap.biomes", false, true, false),
    BIOME_OVERLAY("options.voxelmap.biome_overlay", false, false, true),
    CHUNK_GRID("options.voxelmap.chunk_grid", false, true, false),
    SLIME_CHUNKS("options.voxelmap.slime_chunks", false, true, false),
    WORLD_BORDER("options.voxelmap.world_border", false, true, false),
    WORLD_SEED("World Seed", false, false, false),
    TELEPORT_COMMAND("Teleport Command", false, false, false),

    SHOW_RADAR("options.voxelmap.radar.show_radar", false, true, false),
    RADAR_MODE("options.voxelmap.radar.radar_mode", false, false, true),
    SHOW_MOBS("options.voxelmap.radar.show_mobs", false, false, true),
    SHOW_MOB_NAMES("options.voxelmap.radar.show_mob_names", false, true, false),
    SHOW_MOB_HELMETS("options.voxelmap.radar.show_mob_helmets", false, true, false),
    SHOW_PLAYERS("options.voxelmap.radar.show_players", false, true, false),
    SHOW_PLAYER_NAMES("options.voxelmap.radar.show_player_names", false, true, false),
    SHOW_PLAYER_HELMETS("options.voxelmap.radar.show_player_helmets", false, true, false),
    RADAR_FONT_SIZE("options.voxelmap.radar.font_size", true, false, false),
    SHOW_FACING("options.voxelmap.radar.show_facing", false, true, false),
    ICON_OUTLINES("options.voxelmap.radar.icon_outlines", false, true, false),
    ICON_FILTERING("options.voxelmap.radar.icon_filtering", false, true, false),

    WAYPOINT_DISTANCE("options.voxelmap.waypoints.distance", true, false, false),
    WAYPOINT_ICON_SIZE("options.voxelmap.waypoints.icon_size", true, false, false),
    WAYPOINT_FONT_SIZE("options.voxelmap.waypoints.font_size", true, false, false),
    SHOW_WAYPOINT_NAMES_ON_MAP("options.voxelmap.waypoints.show_waypoint_names_on_map", false, true, false),
    DEATHPOINTS("options.voxelmap.waypoints.deathpoints", false, false, true),
    AUTO_UNIT_CONVERSION("options.voxelmap.waypoints.auto_unit_conversion", false, true, false),
    SHOW_WAYPOINT_NAMES("options.voxelmap.waypoints.show_names", false, false, true),
    SHOW_WAYPOINT_DISTANCES("options.voxelmap.waypoints.show_distances", false, false, true),

    WORLDMAP_MIN_ZOOM("options.voxelmap.worldmap.min_zoom", true, false, false),
    WORLDMAP_MAX_ZOOM("options.voxelmap.worldmap.max_zoom", true, false, false),
    WORLDMAP_CACHE_SIZE("options.voxelmap.worldmap.cache_size", true, false, false),
    WORLDMAP_SHOW_WAYPOINTS("options.voxelmap.worldmap.show_waypoints", false, true, false),
    WORLDMAP_SHOW_WAYPOINT_NAMES("options.voxelmap.worldmap.show_waypoint_names", false, true, false),

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
