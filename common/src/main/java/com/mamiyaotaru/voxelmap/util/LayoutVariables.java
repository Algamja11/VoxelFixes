package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;

public class LayoutVariables {
    public int scScale;
    public float scaleProj;
    public int mapX;
    public int mapY;
    public double zoomScale;
    public double zoomScaleAdjusted;

    public int mapSize;
    // 0: Minimap, 1: Enlarged Map, 2: Fullscreen Map
    public int mapMode;
    public boolean rotates;
    public boolean squareMap;
    public float positionScale;

    private final MapSettingsManager mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();

    public void updateVars(int scScale, float scaleProj, int mapX, int mapY, double zoomScale, double zoomScaleAdjusted, int mapSize, int mapMode) {
        this.scScale = scScale;
        this.scaleProj = scaleProj;
        this.mapX = mapX;
        this.mapY = mapY;
        this.zoomScale = zoomScale;
        this.zoomScaleAdjusted = mapMode == 0 ? zoomScaleAdjusted : zoomScale;

        this.mapSize = mapSize;
        this.mapMode = mapMode;
        this.rotates = mapMode == 0 && mapOptions.rotates;
        this.squareMap = mapMode == 2 || mapOptions.squareMap;
        this.positionScale = (mapSize / 64.0F) / (float) this.zoomScaleAdjusted;
    }
}
