package com.mamiyaotaru.voxelmap.util;

public class LayoutVariables {
    public int scScale;
    public int mapX;
    public int mapY;
    public double zoomScale;
    public double zoomScaleAdjusted;
    public boolean rotating;

    public void updateVars(int scScale, int mapX, int mapY, double zoomScale, double zoomScaleAdjusted, boolean rotating) {
        this.scScale = scScale;
        this.mapX = mapX;
        this.mapY = mapY;
        this.zoomScale = zoomScale;
        this.zoomScaleAdjusted = zoomScaleAdjusted;
        this.rotating = rotating;
    }
}
