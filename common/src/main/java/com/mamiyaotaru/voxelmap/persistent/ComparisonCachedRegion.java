package com.mamiyaotaru.voxelmap.persistent;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.BiomeParser;
import com.mamiyaotaru.voxelmap.util.BlockStateParser;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import com.mamiyaotaru.voxelmap.util.MessageUtils;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public class ComparisonCachedRegion {
    private final PersistentMap persistentMap;
    private final String key;
    private final ClientLevel world;
    private final String subworldName;
    private final String worldNamePathPart;
    private String subworldNamePathPart;
    private final String dimensionNamePathPart;
    private final boolean underground;
    private final int x;
    private final int z;
    private final CompressibleMapData data;
    final MutableBlockPos blockPos = new MutableBlockPos(0, 0, 0);
    private int loadedChunks;
    private boolean loaded;
    private boolean empty = true;

    public ComparisonCachedRegion(PersistentMap persistentMap, String key, ClientLevel world, String worldName, String subworldName, int x, int z) {
        this.data = new CompressibleMapData(world);
        this.persistentMap = persistentMap;
        this.key = key;
        this.world = world;
        this.subworldName = subworldName;
        this.worldNamePathPart = TextUtils.scrubNameFile(worldName);
        if (!Objects.equals(subworldName, "")) {
            this.subworldNamePathPart = TextUtils.scrubNameFile(subworldName) + "/";
        }

        String dimensionName = VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(world).getStorageName();
        this.dimensionNamePathPart = TextUtils.scrubNameFile(dimensionName);
        this.underground = !world.effects().forceBrightLightmap() && !world.dimensionType().hasSkyLight() || world.dimensionType().hasCeiling();
        this.x = x;
        this.z = z;
    }

    public void loadCurrent() {
        this.loadedChunks = 0;

        for (int chunkX = 0; chunkX < 16; ++chunkX) {
            for (int chunkZ = 0; chunkZ < 16; ++chunkZ) {
                LevelChunk chunk = this.world.getChunk(this.x * 16 + chunkX, this.z * 16 + chunkZ);
                if (chunk != null && !chunk.isEmpty() && this.world.hasChunk(this.x * 16 + chunkX, this.z * 16 + chunkZ) && !this.isChunkEmpty(this.world, chunk)) {
                    this.loadChunkData(chunk, chunkX, chunkZ);
                    ++this.loadedChunks;
                }
            }
        }

    }

    private boolean isChunkEmpty(ClientLevel world, LevelChunk chunk) {

        return IntStream.range(0, 16).noneMatch(t -> IntStream.range(0, 16).anyMatch(s -> chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, t, s) != 0));
    }

    private void loadChunkData(LevelChunk chunk, int chunkX, int chunkZ) {
        for (int t = 0; t < 16; ++t) {
            for (int s = 0; s < 16; ++s) {
                this.persistentMap.getAndStoreData(this.data, this.world, chunk, this.blockPos, this.underground, this.x * 256, this.z * 256, chunkX * 16 + t, chunkZ * 16 + s);
            }
        }

    }

    public void loadStored() {
        try {
            File cachedRegionFileDir = new File(VoxelConstants.getMinecraft().gameDirectory, "/voxelmap/cache/" + this.worldNamePathPart + "/" + this.subworldNamePathPart + this.dimensionNamePathPart);
            cachedRegionFileDir.mkdirs();
            File cachedRegionFile = new File(cachedRegionFileDir, "/" + this.key + ".zip");
            if (cachedRegionFile.exists()) {
                FileInputStream fis = new FileInputStream(cachedRegionFile);
                ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
                Scanner sc = new Scanner(zis);
                BiMap<BlockState, Integer> stateToInt = null;
                BiMap<Biome, Integer> biomeMap = null;
                int version = 1;

                ZipEntry ze;
                byte[] decompressedByteData = null;
                for (; (ze = zis.getNextEntry()) != null; zis.closeEntry()) {
                    if (ze.getName().equals("data")) {
                        decompressedByteData = zis.readAllBytes();
                    }

                    if (ze.getName().equals("key")) {
                        stateToInt = HashBiMap.create();

                        while (sc.hasNextLine()) {
                            BlockStateParser.parseLine(sc.nextLine(), stateToInt);
                        }
                    }

                    if (ze.getName().equals("biomes")) {
                        biomeMap = HashBiMap.create();
                        while (sc.hasNextLine()) {
                            BiomeParser.parseLine(world, sc.nextLine(), biomeMap);
                        }
                    }

                    if (ze.getName().equals("control")) {
                        Properties properties = new Properties();
                        properties.load(zis);
                        String versionString = properties.getProperty("version", "1");

                        try {
                            version = Integer.parseInt(versionString);
                        } catch (NumberFormatException var14) {
                            version = 1;
                        }
                    }
                }

                if (decompressedByteData != null && decompressedByteData.length == this.data.getExpectedDataLength(version) && stateToInt != null) {
                    if (biomeMap == null) {
                        biomeMap = HashBiMap.create();
                        BiomeParser.populateLegacyBiomeMap(world, biomeMap);
                    }
                    this.data.setData(decompressedByteData, stateToInt, biomeMap, version);
                    this.empty = false;
                    this.loaded = true;
                } else {
                    VoxelConstants.getLogger().warn("failed to load data from " + cachedRegionFile.getPath());
                }

                sc.close();
                zis.close();
                fis.close();
            }
        } catch (IOException var15) {
            VoxelConstants.getLogger().error("Failed to load region file for " + this.x + "," + this.z + " in " + this.worldNamePathPart + "/" + this.subworldNamePathPart + this.dimensionNamePathPart, var15);
        }

    }

    public String getSubworldName() {
        return this.subworldName;
    }

    public String getKey() {
        return this.key;
    }

    public CompressibleMapData getMapData() {
        return this.data;
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    public boolean isEmpty() {
        return this.empty;
    }

    public int getLoadedChunks() {
        return this.loadedChunks;
    }

    public boolean isGroundAt(int blockX, int blockZ) {
        return this.isLoaded() && this.getHeightAt(blockX, blockZ) > 0;
    }

    public int getHeightAt(int blockX, int blockZ) {
        int x = blockX - this.x * 256;
        int z = blockZ - this.z * 256;
        int y = this.data.getHeight(x, z);
        if (this.underground && y == 255) {
            y = CommandUtils.getSafeHeight(blockX, 64, blockZ, this.world);
        }

        return y;
    }

    public int getSimilarityTo(ComparisonCachedRegion candidate) {
        int compared = 0;
        int matched = 0;
        CompressibleMapData candidateData = candidate.getMapData();

        for (int t = 0; t < 16; ++t) {
            for (int s = 0; s < 16; ++s) {
                int nonZeroHeights = 0;
                int nonZeroHeightsInCandidate = 0;
                int matchesInChunk = 0;

                for (int i = 0; i < 16; ++i) {
                    for (int j = 0; j < 16; ++j) {
                        int x = t * 16 + i;
                        int z = s * 16 + j;
                        if (this.data.getHeight(x, z) == candidateData.getHeight(x, z) && this.data.getBlockstate(x, z) == candidateData.getBlockstate(x, z) && (this.data.getOceanFloorHeight(x, z) == 0 || this.data.getOceanFloorHeight(x, z) == candidateData.getOceanFloorHeight(x, z) && this.data.getOceanFloorBlockstate(x, z) == candidateData.getOceanFloorBlockstate(x, z))) {
                            ++matchesInChunk;
                        }

                        if (this.data.getHeight(x, z) != Short.MIN_VALUE) {
                            ++nonZeroHeights;
                        }

                        if (candidateData.getHeight(x, z) != Short.MIN_VALUE) {
                            ++nonZeroHeightsInCandidate;
                        }
                    }
                }

                if (nonZeroHeights != 0 && nonZeroHeightsInCandidate != 0) {
                    compared += 256;
                    matched += matchesInChunk;
                }

                MessageUtils.printDebugWarn("at " + t + "," + s + " there were local non zero: " + nonZeroHeights + " and comparison non zero: " + nonZeroHeightsInCandidate);
            }
        }

        MessageUtils.printDebugWarn("compared: " + compared + ", matched: " + matched);
        return compared >= 256 ? matched * 100 / compared : 0;
    }
}
