package com.mamiyaotaru.voxelmap.util;

public class LayoutVariables {
    public int scScale;
    public float scaleProj;
    public int mapX;
    public int mapY;
    public int scWidth;
    public int scHeight;
    public double zoomScale;
    public double zoomScaleAdjusted;

    public int mapSize = 64;

    public void updateVars(int scScale, float scaleProj, int mapX, int mapY, int scWidth, int scHeight, double zoomScale, double zoomScaleAdjusted) {
        this.scScale = scScale;
        this.scaleProj = scaleProj;
        this.mapX = mapX;
        this.mapY = mapY;
        this.scWidth = scWidth;
        this.scHeight = scHeight;
        this.zoomScale = zoomScale;
        this.zoomScaleAdjusted = zoomScaleAdjusted;
    }

    public float getPositionScale() {
        return (mapSize / 64.0F) / (float) zoomScaleAdjusted;
    }
}
