package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;

public class LayoutVariables {
    public enum MapMode { MINIMAP, ENLARGED_MAP, FULLSCREEN_MAP }

    public int scScale;
    public float scaleProj;
    public int mapX;
    public int mapY;
    public double zoomScale;
    public double zoomScaleAdjusted;

    public int mapSize;
    public MapMode mapMode;
    public boolean rotates;
    public boolean squareMap;
    public float positionScale;

    private final MapSettingsManager mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();

    public void updateVars(int scScale, float scaleProj, int mapX, int mapY, double zoomScale, double zoomScaleAdjusted, int mapSize, MapMode mapMode) {
        this.scScale = scScale;
        this.scaleProj = scaleProj;
        this.mapX = mapX;
        this.mapY = mapY;
        this.zoomScale = zoomScale;
        this.zoomScaleAdjusted = mapMode == MapMode.MINIMAP ? zoomScaleAdjusted : zoomScale;

        this.mapSize = mapSize;
        this.mapMode = mapMode;
        this.rotates = mapMode == MapMode.MINIMAP && mapOptions.rotates;
        this.squareMap = mapMode == MapMode.FULLSCREEN_MAP || mapOptions.squareMap;
        this.positionScale = (mapSize / 64.0F) / (float) this.zoomScaleAdjusted;
    }
}
