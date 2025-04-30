package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;

public final class MessageUtils {
//    private static final boolean debug = false;

    private MessageUtils() {}

    public static void chatInfo(String s) { VoxelConstants.getVoxelMapInstance().sendPlayerMessageOnMainThread(s); }

    public static void printDebugInfo(String line) { if (VoxelConstants.DEBUG) VoxelConstants.getLogger().info(line); }

    public static void printDebugWarn(String line) { if (VoxelConstants.DEBUG) VoxelConstants.getLogger().warn(line); }
}