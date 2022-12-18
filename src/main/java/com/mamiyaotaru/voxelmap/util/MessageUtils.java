package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelMap;

public class MessageUtils {
    private final static boolean debug = false;

    public static void chatInfo(String s) {
        VoxelMap.getInstance().sendPlayerMessageOnMainThread(s);
    }

    public static void printDebug(String line) {
        if (debug) {
            VoxelConstants.getLogger().warn(line);
        }

    }
}
