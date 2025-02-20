package com.mamiyaotaru.voxelmap.gui.overridden;

public enum EnumOptionsMinimap {
    // Map Options
    DISPLAY("options.minimap.display", false, false, true),
    COORDS("options.minimap.showcoordinates", false, false, true),
    SIZE("options.minimap.size", false, false, true),
    SHAPE("options.minimap.shape", false, false, true),
    ROTATES("options.minimap.rotation", false, true, false),
    LOCATION("options.minimap.location", false, false, true),
    CAVEMODE("options.minimap.cavemode", false, true, false),
    INGAMEWAYPOINTS("options.minimap.ingamewaypoints", false, false, true),
    MOVESCOREBOARDDOWN("options.minimap.movescoreboarddown", false, true, false),
    MOVEMAPDOWNWHILESTATSUEFFECT("options.minimap.movemapdownwhilestatuseffect", false, true, false),

    // Map Detail Options
    LIGHTING("options.minimap.dynamiclighting", false, true, false),
    TERRAINDEPTH("options.minimap.terraindepth", false, false, true),
    FILTERING("options.minimap.filtering", false, true, false),
    WATERTRANSPARENCY("options.minimap.watertransparency", false, true, false),
    BLOCKTRANSPARENCY("options.minimap.blocktransparency", false, true, false),
    BIOMETINT("options.minimap.biomes", false, true, false),
    BIOMEOVERLAY("options.minimap.biomeoverlay", false, false, true),
    CHUNKGRID("options.minimap.chunkgrid", false, true, false),
    SLIMECHUNKS("options.minimap.slimechunks", false, true, false),
    WORLDBORDER("options.minimap.worldborder", false, true, false),

    // Radar Options
    SHOWRADAR("options.minimap.radar.showradar", false, true, false),
    RADARMODE("options.minimap.radar.radarmode", false, false, true),
    SHOWMOBS("options.minimap.radar.showmobs", false, false, true),
    SHOWPLAYERS("options.minimap.radar.showplayers", false, true, false),
    SHOWMOBHELMETS("options.minimap.radar.showmobhelmets", false, true, false),
    SHOWPLAYERHELMETS("options.minimap.radar.showplayerhelmets", false, true, false),
    SHOWMOBNAMES("options.minimap.radar.showmobnames", false, true, false),
    SHOWPLAYERNAMES("options.minimap.radar.showplayernames", false, true, false),
    RADAROUTLINE("options.minimap.radar.iconoutlines", false, true, false),
    RADARFILTERING("options.minimap.radar.iconfiltering", false, true, false),
    SHOWFACING("options.minimap.radar.showfacing", false, true, false),

    // Radar Detail Options
    RADARFONTSIZE("options.minimap.radar.fontsize", true, false, false),
    SHOWONLYTAGGEDMOBNAMES("options.minimap.radar.showonlytaggedmobnames", false, true, false),

    // Waypoint Options
    WAYPOINTDISTANCE("options.minimap.waypoints.distance", true, false, false),
    WAYPOINTSIZE("options.minimap.waypoints.pointsize", true, false, false),
    DEATHPOINTS("options.minimap.waypoints.deathpoints", false, false, true),
    DISTANCEUNITCONVERSION("options.minimap.waypoints.distanceunitconversion", false, true, false),
    WAYPOINTNAMEBELOWICON("options.minimap.waypoints.waypointnamebelowicon", false, true, false),
    WAYPOINTDISTANCEBELOWNAME("options.minimap.waypoints.waypointdistancebelowname", false, true, false),

    // World Map Options
    SHOWWAYPOINTS("options.worldmap.showwaypoints", false, true, false),
    SHOWWAYPOINTNAMES("options.worldmap.showwaypointnames", false, true, false),
    CACHESIZE("options.worldmap.cachesize", true, false, false),
    MINZOOM("options.worldmap.minzoom", true, false, false),
    MAXZOOM("options.worldmap.maxzoom", true, false, false),

    // Internal
    WELCOME("Welcome Screen", false, true, false),
    OLDNORTH("options.minimap.oldnorth", false, true, false),
    ZOOMLEVEL("option.minimapZoom", false, true, false);

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
