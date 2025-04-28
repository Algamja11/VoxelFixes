package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.ISubSettingsManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import net.minecraft.client.resources.language.I18n;

public class PersistentMapSettingsManager implements ISubSettingsManager {
    protected int mapX;
    protected int mapZ;
    protected float zoom = 4.0F;
    private float minZoomPower = -1.0F;
    private float maxZoomPower = 4.0F;
    protected float minZoom = 0.5F;
    protected float maxZoom = 16.0F;
    protected int cacheSize = 500;
    protected boolean outputImages;
    public boolean showWaypoints = true;
    public boolean showWaypointNames = true;

    @Override
    public void loadAll(File settingsFile) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(settingsFile));

            String sCurrentLine;
            while ((sCurrentLine = in.readLine()) != null) {
                String[] curLine = sCurrentLine.split(":");
                switch (curLine[0]) {
                    case "Worldmap Zoom" -> this.zoom = Float.parseFloat(curLine[1]);
                    case "Worldmap Minimum Zoom" -> this.minZoom = Float.parseFloat(curLine[1]);
                    case "Worldmap Maximum Zoom" -> this.maxZoom = Float.parseFloat(curLine[1]);
                    case "Worldmap Cache Size" -> this.cacheSize = Integer.parseInt(curLine[1]);
                    case "Show Worldmap Waypoints" -> this.showWaypoints = Boolean.parseBoolean(curLine[1]);
                    case "Show Worldmap Waypoint Names" -> this.showWaypointNames = Boolean.parseBoolean(curLine[1]);
                    case "Output Images" -> this.outputImages = Boolean.parseBoolean(curLine[1]);
                }
            }

            in.close();
        } catch (IOException ignored) {}

        for (int power = -3; power <= 5; ++power) {
            if (Math.pow(2.0, power) == this.minZoom) {
                this.minZoomPower = power;
            }

            if (Math.pow(2.0, power) == this.maxZoom) {
                this.maxZoomPower = power;
            }
        }

        this.bindCacheSize();
        this.bindZoom();
    }

    @Override
    public void saveAll(PrintWriter out) {
        out.println("Worldmap Zoom:" + this.zoom);
        out.println("Worldmap Minimum Zoom:" + this.minZoom);
        out.println("Worldmap Maximum Zoom:" + this.maxZoom);
        out.println("Worldmap Cache Size:" + this.cacheSize);
        out.println("Show Worldmap Waypoints:" + this.showWaypoints);
        out.println("Show Worldmap Waypoint Names:" + this.showWaypointNames);
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
                case WORLDMAP_MIN_ZOOM, WORLDMAP_MAX_ZOOM -> name + (float) Math.pow(2.0, value) + "x";
                case WORLDMAP_CACHE_SIZE -> name + (int) value;
                default -> name + value;
            };
        } else {
            return name;
        }
    }

    @Override
    public boolean getBooleanValue(EnumOptionsMinimap option) {
        return switch (option) {
            case WORLDMAP_SHOW_WAYPOINTS -> this.showWaypoints && VoxelMap.mapOptions.waypointsAllowed;
            case WORLDMAP_SHOW_WAYPOINT_NAMES -> this.showWaypointNames && VoxelMap.mapOptions.waypointsAllowed;
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        };
    }

    @Override
    public String getListValue(EnumOptionsMinimap option) {
        switch (option) {
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        }
    }

    @Override
    public float getFloatValue(EnumOptionsMinimap option) {
        return switch (option) {
            case WORLDMAP_MIN_ZOOM -> this.minZoomPower;
            case WORLDMAP_MAX_ZOOM -> this.maxZoomPower;
            case WORLDMAP_CACHE_SIZE -> this.cacheSize;
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        };
    }

    @Override
    public void setValue(EnumOptionsMinimap option) {
        switch (option) {
            case WORLDMAP_SHOW_WAYPOINTS -> this.showWaypoints = !this.showWaypoints;
            case WORLDMAP_SHOW_WAYPOINT_NAMES -> this.showWaypointNames = !this.showWaypointNames;
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        }

    }

    @Override
    public void setFloatValue(EnumOptionsMinimap option, float value) {
        switch (option) {
            case WORLDMAP_MIN_ZOOM -> {
                this.minZoomPower = ((int) (value * 8.0F) - 3);
                this.minZoom = (float) Math.pow(2.0, this.minZoomPower);
                if (this.maxZoom < this.minZoom) {
                    this.maxZoom = this.minZoom;
                    this.maxZoomPower = this.minZoomPower;
                }
            }
            case WORLDMAP_MAX_ZOOM -> {
                this.maxZoomPower = ((int) (value * 8.0F) - 3);
                this.maxZoom = (float) Math.pow(2.0, this.maxZoomPower);
                if (this.minZoom > this.maxZoom) {
                    this.minZoom = this.maxZoom;
                    this.minZoomPower = this.maxZoomPower;
                }
            }
            case WORLDMAP_CACHE_SIZE -> {
                this.cacheSize = (int) (value * 5000.0F);
                this.cacheSize = Math.max(this.cacheSize, 30);

                for (int minCacheSize = (int) ((1600.0F / this.minZoom / 256.0F + 4.0F) * (1100.0F / this.minZoom / 256.0F + 3.0F) * 1.35F); this.cacheSize < minCacheSize; minCacheSize = (int) ((1600.0F / this.minZoom / 256.0F + 4.0F) * (1100.0F / this.minZoom / 256.0F + 3.0F) * 1.35F)) {
                    ++this.minZoomPower;
                    this.minZoom = (float) Math.pow(2.0, this.minZoomPower);
                }

                if (this.maxZoom < this.minZoom) {
                    this.maxZoom = this.minZoom;
                    this.maxZoomPower = this.minZoomPower;
                }
            }
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        }

        this.bindZoom();
        this.bindCacheSize();
    }

    private void bindCacheSize() {
        int minCacheSize = (int) ((1600.0F / this.minZoom / 256.0F + 4.0F) * (1100.0F / this.minZoom / 256.0F + 3.0F) * 1.35F);
        this.cacheSize = Math.max(this.cacheSize, minCacheSize);
    }

    private void bindZoom() {
        this.zoom = Math.max(this.zoom, this.minZoom);
        this.zoom = Math.min(this.zoom, this.maxZoom);
    }
}
