package com.mamiyaotaru.voxelmap.util;

public class LayoutVariables {
    public int scScale;
    public float scaleProj;
    public int mapX;
    public int mapY;
    public int mapSize;
    public int scWidth;
    public int scHeight;
    public double zoomScale;
    public double zoomScaleAdjusted;

    public void updateVars(int scScale, float scaleProj, int mapX, int mapY, int mapSize, int scWidth, int scHeight, double zoomScale, double zoomScaleAdjusted) {
        this.scScale = scScale;
        this.scaleProj = scaleProj;
        this.mapX = mapX;
        this.mapY = mapY;
        this.mapSize = mapSize;
        this.scWidth = scWidth;
        this.scHeight = scHeight;
        this.zoomScale = zoomScale;
        this.zoomScaleAdjusted = zoomScaleAdjusted;
    }
}
