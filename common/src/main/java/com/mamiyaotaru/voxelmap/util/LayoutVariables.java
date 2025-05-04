package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;

public class LayoutVariables {
    public int scScale;
    public float scaleProj;
    public int mapX;
    public int mapY;
    public int mapSize;
    public int mapMode;
    public double zoomScale;
    public double zoomScaleAdjusted;

    private final MapSettingsManager mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();

    public void updateVars(int scScale, float scaleProj, int mapX, int mapY, int mapSize, int mapMode, double zoomScale, double zoomScaleAdjusted) {
        this.scScale = scScale;
        this.scaleProj = scaleProj;
        this.mapX = mapX;
        this.mapY = mapY;
        this.mapSize = mapSize;
        this.mapMode = mapMode;
        this.zoomScale = zoomScale;
        this.zoomScaleAdjusted = zoomScaleAdjusted;
    }

    public boolean isSquareMap() {
        return mapOptions.squareMap || mapMode == 2;
    }

    public boolean getRotates() {
        return mapOptions.rotates && mapMode == 0;
    }

    public double getZoomScaleAdjusted() {
        return mapMode == 0 ? zoomScaleAdjusted : zoomScale;
    }

    public float getPositionScale() {
        return (mapSize / 64.0F) / (float) getZoomScaleAdjusted();
    }
}
