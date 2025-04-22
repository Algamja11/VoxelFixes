package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;

public interface ISettingsManager {
    String getKeyText(EnumOptionsMinimap option);

    boolean getBooleanValue(EnumOptionsMinimap option);

    String getListValue(EnumOptionsMinimap option);

    float getFloatValue(EnumOptionsMinimap option);

    void setValue(EnumOptionsMinimap option);

    void setFloatValue(EnumOptionsMinimap option, float value);
}
