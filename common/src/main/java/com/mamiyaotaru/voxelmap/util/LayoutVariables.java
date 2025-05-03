package com.mamiyaotaru.voxelmap.util;

public class LayoutVariables {
    public int scScale;
    public float scaleProj;
    public int mapX;
    public int mapY;
    public int mapSize;
    public boolean rotates;
    public double zoomScale;
    public double zoomScaleAdjusted;

    public void updateVars(int scScale, float scaleProj, int mapX, int mapY, int mapSize, boolean rotates, double zoomScale, double zoomScaleAdjusted) {
        this.scScale = scScale;
        this.scaleProj = scaleProj;
        this.mapX = mapX;
        this.mapY = mapY;
        this.mapSize = mapSize;
        this.rotates = rotates;
        this.zoomScale = zoomScale;
        this.zoomScaleAdjusted = zoomScaleAdjusted;
    }

    public float getPositionScale() {
        return (mapSize / 64.0F) / (float) zoomScaleAdjusted;
    }
}
