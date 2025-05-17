package com.mamiyaotaru.voxelmap.util;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BlockRepository {
    public static Block air = Blocks.AIR;
    public static Block voidAir;
    public static Block caveAir;
    public static int airID;
    public static int voidAirID;
    public static int caveAirID;

    public static Block oakLeaves;
    public static Block spruceLeaves;
    public static Block birchLeaves;
    public static Block jungleLeaves;
    public static Block acaciaLeaves;
    public static Block darkOakLeaves;
    public static Block mangroveLeaves;

    public static Block grassBlock;
    public static Block grass;
    public static Block tallGrass;
    public static Block fern;
    public static Block largeFern;
    public static Block leafLitter;
    public static Block lilypad;
    public static Block redstone;
    public static Block reeds;
    public static Block vine;
    public static Block water;

    public static Block barrier;
    public static Block cobweb;
    public static Block chorusPlant;
    public static Block chorusFlower;
    public static Block ice;
    public static Block ladder;
    public static Block lava;
    public static Block piston;
    public static Block stickyPiston;
    public static MovingPistonBlock movingPiston;

    public static HashSet<Block> biomeBlocks;
    public static HashSet<Block> shapedBlocks;
    public static HashSet<Block> leafBlocks;

    private static final ConcurrentHashMap<BlockState, Integer> stateToInt = new ConcurrentHashMap<>(1024);
    private static final ReferenceArrayList<BlockState> blockStates = new ReferenceArrayList<>(16384);
    private static int count = 1;
    private static final ReadWriteLock incrementLock = new ReentrantReadWriteLock();

    public static void getBlocks() {
        air = Blocks.AIR;
        voidAir = Blocks.VOID_AIR;
        caveAir = Blocks.CAVE_AIR;
        airID = getStateId(air.defaultBlockState());
        voidAirID = getStateId(voidAir.defaultBlockState());
        caveAirID = getStateId(caveAir.defaultBlockState());

        oakLeaves = Blocks.OAK_LEAVES;
        spruceLeaves = Blocks.SPRUCE_LEAVES;
        birchLeaves = Blocks.BIRCH_LEAVES;
        jungleLeaves = Blocks.JUNGLE_LEAVES;
        acaciaLeaves = Blocks.ACACIA_LEAVES;
        darkOakLeaves = Blocks.DARK_OAK_LEAVES;
        mangroveLeaves = Blocks.MANGROVE_LEAVES;

        grassBlock = Blocks.GRASS_BLOCK;
        grass = Blocks.SHORT_GRASS;
        tallGrass = Blocks.TALL_GRASS;
        fern = Blocks.FERN;
        largeFern = Blocks.LARGE_FERN;
        leafLitter = Blocks.LEAF_LITTER;
        lilypad = Blocks.LILY_PAD;
        redstone = Blocks.REDSTONE_WIRE;
        reeds = Blocks.SUGAR_CANE;
        vine = Blocks.VINE;
        water = Blocks.WATER;

        barrier = Blocks.BARRIER;
        cobweb = Blocks.COBWEB;
        chorusPlant = Blocks.CHORUS_PLANT;
        chorusFlower = Blocks.CHORUS_FLOWER;
        ice = Blocks.ICE;
        ladder = Blocks.LADDER;
        lava = Blocks.LAVA;
        piston = Blocks.PISTON;
        stickyPiston = Blocks.STICKY_PISTON;
        movingPiston = (MovingPistonBlock) Blocks.MOVING_PISTON;

        Block[] biomeBlocksArray = new Block[]{grassBlock, oakLeaves, spruceLeaves, birchLeaves, jungleLeaves, acaciaLeaves, darkOakLeaves, mangroveLeaves, grass, fern, tallGrass, largeFern, reeds, vine, lilypad, leafLitter, water};
        Block[] shapedBlocksArray = new Block[]{ladder, vine, chorusPlant, chorusFlower};
        Block[] leafBlocksArray = new Block[]{oakLeaves, spruceLeaves, birchLeaves, jungleLeaves, acaciaLeaves, darkOakLeaves, mangroveLeaves};

        biomeBlocks = new HashSet<>(Arrays.asList(biomeBlocksArray));
        shapedBlocks = new HashSet<>(Arrays.asList(shapedBlocksArray));
        leafBlocks = new HashSet<>(Arrays.asList(leafBlocksArray));

        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof DoorBlock || block instanceof SignBlock) {
                shapedBlocks.add(block);
            }
        }

    }

    public static int getStateId(BlockState blockState) {
        Integer id = stateToInt.get(blockState);
        if (id == null) {
            synchronized (incrementLock) {
                id = stateToInt.get(blockState);
                if (id == null) {
                    id = count;
                    blockStates.add(blockState);
                    stateToInt.put(blockState, id);
                    ++count;
                }
            }
        }

        return id;
    }

    public static BlockState getStateById(int id) {
        return blockStates.get(id);
    }

    static {
        BlockState airBlockState = Blocks.AIR.defaultBlockState();
        stateToInt.put(airBlockState, 0);
        blockStates.add(airBlockState);
    }
}
