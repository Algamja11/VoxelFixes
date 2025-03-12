package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.AbstractMapData;
import com.mamiyaotaru.voxelmap.util.BlockModel;
import com.mamiyaotaru.voxelmap.util.BlockRepository;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.MessageUtils;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.imageio.ImageIO;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;

import java.awt.Image;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ColorManager {
    private boolean resourcePacksChanged;
    private ClientLevel world;
    private BufferedImage terrainBuff;
    private BufferedImage colorPicker;
    private int sizeOfBiomeArray;
    private int[] blockColors = new int[16384];
    private int[] blockColorsWithDefaultTint = new int[16384];
    private final HashSet<Integer> biomeTintsAvailable = new HashSet<>();
    private final HashMap<Integer, int[][]> blockTintTables = new HashMap<>();
    private final HashSet<Integer> biomeTextureAvailable = new HashSet<>();
    private final HashMap<String, Integer> blockBiomeSpecificColors = new HashMap<>();
    private float failedToLoadX;
    private float failedToLoadY;
    private final RandomSource random = RandomSource.create();
    private boolean loaded;
    private final MutableBlockPos dummyBlockPos = new MutableBlockPos(BlockPos.ZERO.getX(), BlockPos.ZERO.getY(), BlockPos.ZERO.getZ());
    private final Vector3f fullbright = new Vector3f(1.0F, 1.0F, 1.0F);
    private final ColorResolver spruceColorResolver = (blockState, biomex, blockPos) -> FoliageColor.FOLIAGE_EVERGREEN;
    private final ColorResolver birchColorResolver = (blockState, biomex, blockPos) -> FoliageColor.FOLIAGE_BIRCH;
    private final ColorResolver grassColorResolver = (blockState, biomex, blockPos) -> biomex.getGrassColor(blockPos.getX(), blockPos.getZ());
    private final ColorResolver foliageColorResolver = (blockState, biomex, blockPos) -> biomex.getFoliageColor();
    private final ColorResolver waterColorResolver = (blockState, biomex, blockPos) -> biomex.getWaterColor();
    private final ColorResolver redstoneColorResolver = (blockState, biomex, blockPos) -> RedStoneWireBlock.getColorForPower(blockState.getValue(RedStoneWireBlock.POWER));

    public ColorManager() {
        ++this.sizeOfBiomeArray;
    }

    public int getAirColor() {
        return this.blockColors[BlockRepository.airID];
    }

    public BufferedImage getColorPicker() {
        return this.colorPicker;
    }

    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.resourcePacksChanged = true;
    }

    public boolean checkForChanges() {
        boolean biomesChanged = false;

        if (VoxelConstants.getClientWorld() != this.world) {
            this.world = VoxelConstants.getClientWorld();
            int largestBiomeID = 0;

            for (Biome biome : this.world.registryAccess().lookupOrThrow(Registries.BIOME)) {
                int biomeID = this.world.registryAccess().lookupOrThrow(Registries.BIOME).getId(biome);
                if (biomeID > largestBiomeID) {
                    largestBiomeID = biomeID;
                }
            }

            if (this.sizeOfBiomeArray != largestBiomeID + 1) {
                this.sizeOfBiomeArray = largestBiomeID + 1;
                biomesChanged = true;
            }
        }

        boolean changed = this.resourcePacksChanged || biomesChanged;
        this.resourcePacksChanged = false;
        if (changed) {
            this.loadColors();
        }

        return changed;
    }

    private void loadColors() {
        VoxelConstants.getMinecraft().getSkinManager().getInsecureSkin(VoxelConstants.getPlayer().getGameProfile());
        BlockRepository.getBlocks();
        this.loadColorPicker();
        this.loadTexturePackTerrainImage();
        TextureAtlasSprite missing = VoxelConstants.getMinecraft().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.parse("missingno"));
        this.failedToLoadX = missing.getU0();
        this.failedToLoadY = missing.getV0();
        this.loaded = false;

        try {
            Arrays.fill(this.blockColors, -16842497);
            Arrays.fill(this.blockColorsWithDefaultTint, -16842497);
            this.loadSpecialColors();
            this.biomeTintsAvailable.clear();
            this.biomeTextureAvailable.clear();
            this.blockBiomeSpecificColors.clear();
            this.blockTintTables.clear();
            VoxelConstants.getVoxelMapInstance().getMap().forceFullRender(true);
        } catch (Exception var5) {
            VoxelConstants.getLogger().error("error loading pack", var5);
        }

        this.loaded = true;
    }

    public final BufferedImage getBlockImage(BlockState blockState, ItemStack stack, Level world, float iconScale, float captureDepth) {
        try {
            BakedModel model = VoxelConstants.getMinecraft().getModelManager().getBlockModelShaper().getBlockModel(blockState);
            this.drawModel(Direction.EAST, model, stack, world, iconScale, captureDepth);
            BufferedImage blockImage = ImageUtils.createBufferedImageFromGLID(OpenGL.Utils.fboTextureId);
            if (VoxelConstants.DEBUG) {
                ImageIO.write(blockImage, "png", new File(VoxelConstants.getMinecraft().gameDirectory, blockState.getBlock().getName().getString() + "-" + Block.getId(blockState) + ".png"));
            }
            return blockImage;
        } catch (Exception var8) {
            VoxelConstants.getLogger().error("error getting block armor image for " + blockState.toString() + ": " + var8.getLocalizedMessage(), var8);
            return null;
        }
    }

    private void drawModel(Direction facing, BakedModel model, ItemStack stack, Level world, float scale, float captureDepth) {
        float size = 8.0F * scale;
        ItemTransforms transforms = model.getTransforms();
        ItemTransform headTransforms = transforms.head();
        Vector3f translations = headTransforms.translation;
        float transX = -translations.x() * size + 0.5F * size;
        float transY = translations.y() * size + 0.5F * size;
        float transZ = -translations.z() * size + 0.5F * size;
        Vector3f rotations = headTransforms.rotation;
        float rotX = rotations.x();
        float rotY = rotations.y();
        float rotZ = rotations.z();
        OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.Utils.fboTextureId);
        int width = OpenGL.glGetTexLevelParameteri(OpenGL.GL11_GL_TEXTURE_2D, 0, OpenGL.GL11_GL_TRANSFORM_BIT);
        int height = OpenGL.glGetTexLevelParameteri(OpenGL.GL11_GL_TEXTURE_2D, 0, OpenGL.GL11_GL_TEXTURE_HEIGHT);
        OpenGL.glBindTexture(OpenGL.GL11_GL_TEXTURE_2D, 0);
        OpenGL.glViewport(0, 0, width, height);
        Matrix4f minimapProjectionMatrix = RenderSystem.getProjectionMatrix();
        Matrix4f matrix4f = new Matrix4f().ortho(0.0F, width, height, 0.0F, 1000.0F, 3000.0F);
        RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.ORTHOGRAPHIC);
        Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
        matrixStack.pushMatrix();
        matrixStack.identity();
        matrixStack.translate(0.0f, 0.0f, -3000.0f + (captureDepth * scale));
        OpenGL.Utils.bindFramebuffer();
        OpenGL.glDepthMask(true);
        OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        OpenGL.glDisable(OpenGL.GL11_GL_CULL_FACE);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        OpenGL.glClearColor(1.0F, 1.0F, 1.0F, 0.0F);
        OpenGL.glClearDepth(1.0);
        OpenGL.glClear(OpenGL.GL11_GL_COLOR_BUFFER_BIT | OpenGL.GL11_GL_DEPTH_BUFFER_BIT);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
        matrixStack.pushMatrix();
        matrixStack.translate((width / 2f) - size / 2.0F + transX, (height / 2f) - size / 2.0F + transY, 0.0F + transZ);
        matrixStack.scale(size, size, size);
        VoxelConstants.getMinecraft().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).setFilter(false, false);
        OpenGL.Utils.img2(TextureAtlas.LOCATION_BLOCKS);
        matrixStack.rotate(Axis.YP.rotationDegrees(180.0F));
        matrixStack.rotate(Axis.YP.rotationDegrees(rotY));
        matrixStack.rotate(Axis.XP.rotationDegrees(rotX));
        matrixStack.rotate(Axis.ZP.rotationDegrees(rotZ));
        if (facing == Direction.UP) {
            matrixStack.rotate(Axis.XP.rotationDegrees(90.0F));
        }

        Vector4f fullbright2 = new Vector4f(this.fullbright.x, fullbright.y, fullbright.z, 0);
        fullbright2.mul(matrixStack);
        Vector3f fullbright3 = new Vector3f(fullbright2.x, fullbright2.y, fullbright2.z);
        RenderSystem.setShaderLights(fullbright3, fullbright3);
        PoseStack newMatrixStack = new PoseStack();
        MultiBufferSource.BufferSource immediate = VoxelConstants.getMinecraft().renderBuffers().bufferSource();
        VoxelConstants.getMinecraft().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, 15728880, OverlayTexture.NO_OVERLAY, newMatrixStack, immediate, world, 0);
        immediate.endBatch();
        matrixStack.popMatrix();
        matrixStack.popMatrix();
        OpenGL.glEnable(OpenGL.GL11_GL_CULL_FACE);
        OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glDepthMask(false);
        OpenGL.Utils.unbindFramebuffer();
        RenderSystem.setProjectionMatrix(minimapProjectionMatrix, ProjectionType.ORTHOGRAPHIC);
        OpenGL.glViewport(0, 0, VoxelConstants.getMinecraft().getWindow().getWidth(), VoxelConstants.getMinecraft().getWindow().getHeight());
    }

    private void loadColorPicker() {
        try {
            InputStream is = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/colorpicker.png")).get().open();
            Image picker = ImageIO.read(is);
            is.close();
            this.colorPicker = new BufferedImage(picker.getWidth(null), picker.getHeight(null), 2);
            Graphics gfx = this.colorPicker.createGraphics();
            gfx.drawImage(picker, 0, 0, null);
            gfx.dispose();
        } catch (Exception var4) {
            VoxelConstants.getLogger().error("Error loading color picker: " + var4.getLocalizedMessage());
        }

    }

    public void setSkyColor(int skyColor) {
        this.blockColors[BlockRepository.airID] = skyColor;
        this.blockColors[BlockRepository.voidAirID] = skyColor;
        this.blockColors[BlockRepository.caveAirID] = skyColor;
    }

    private void loadTexturePackTerrainImage() {
        try {
            VoxelConstants.getMinecraft().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).bind();
            BufferedImage terrainStitched = ImageUtils.createBufferedImageFromCurrentGLImage();
            this.terrainBuff = new BufferedImage(terrainStitched.getWidth(null), terrainStitched.getHeight(null), 6);
            Graphics gfx = this.terrainBuff.createGraphics();
            gfx.drawImage(terrainStitched, 0, 0, null);
            gfx.dispose();
        } catch (Exception var4) {
            VoxelConstants.getLogger().error("Error processing new resource pack: " + var4.getLocalizedMessage(), var4);
        }

    }

    private void loadSpecialColors() {
        int blockStateID;
        for (Iterator<BlockState> blockStateIterator = BlockRepository.pistonTechBlock.getStateDefinition().getPossibleStates().iterator(); blockStateIterator.hasNext(); this.blockColors[blockStateID] = 0) {
            BlockState blockState = blockStateIterator.next();
            blockStateID = BlockRepository.getStateId(blockState);
        }

        for (Iterator<BlockState> var6 = BlockRepository.barrier.getStateDefinition().getPossibleStates().iterator(); var6.hasNext(); this.blockColors[blockStateID] = 0) {
            BlockState blockState = var6.next();
            blockStateID = BlockRepository.getStateId(blockState);
        }

    }

    public final int getBlockColorWithDefaultTint(MutableBlockPos blockPos, int blockStateID) {
        if (this.loaded) {
            int col = 452984832;

            try {
                col = this.blockColorsWithDefaultTint[blockStateID];
            } catch (ArrayIndexOutOfBoundsException ignored) {
            }

            return ARGB.toABGR(col != -16842497 ? col : this.getBlockColor(blockPos, blockStateID));
        } else {
            return 0;
        }
    }

    public final int getBlockColor(MutableBlockPos blockPos, int blockStateID, Biome biomeID) {
        if (this.loaded) {
            return ARGB.toABGR(this.getBlockColor(blockPos, blockStateID));
        } else {
            return 0;
        }
    }

    private int getBlockColor(int blockStateID) {
        return this.getBlockColor(this.dummyBlockPos, blockStateID);
    }

    private int getBlockColor(MutableBlockPos blockPos, int blockStateID) {
        int col = 452984832;

        try {
            col = this.blockColors[blockStateID];
        } catch (ArrayIndexOutOfBoundsException var5) {
            this.resizeColorArrays(blockStateID);
        }

        if (col == -16842497 || col == 452984832) {
            BlockState blockState = BlockRepository.getStateById(blockStateID);
            col = this.blockColors[blockStateID] = this.getColor(blockPos, blockState);
        }

        return col;
    }

    private synchronized void resizeColorArrays(int queriedID) {
        if (queriedID >= this.blockColors.length) {
            int[] newBlockColors = new int[this.blockColors.length * 2];
            int[] newBlockColorsWithDefaultTint = new int[this.blockColors.length * 2];
            System.arraycopy(this.blockColors, 0, newBlockColors, 0, this.blockColors.length);
            System.arraycopy(this.blockColorsWithDefaultTint, 0, newBlockColorsWithDefaultTint, 0, this.blockColorsWithDefaultTint.length);
            Arrays.fill(newBlockColors, this.blockColors.length, newBlockColors.length, -16842497);
            Arrays.fill(newBlockColorsWithDefaultTint, this.blockColorsWithDefaultTint.length, newBlockColorsWithDefaultTint.length, -16842497);
            this.blockColors = newBlockColors;
            this.blockColorsWithDefaultTint = newBlockColorsWithDefaultTint;
        }

    }

    private int getColor(MutableBlockPos blockPos, BlockState state) {
        try {
            int color = this.getColorForBlockPosBlockStateAndFacing(blockPos, state, Direction.UP);
            if (color == 452984832) {
                BlockRenderDispatcher blockRendererDispatcher = VoxelConstants.getMinecraft().getBlockRenderer();
                color = this.getColorForTerrainSprite(state, blockRendererDispatcher);
            }

            Block block = state.getBlock();
            if (block == BlockRepository.cobweb) {
                color |= -16777216;
            }

            if (block == BlockRepository.redstone) {
                color = ColorUtils.colorMultiplier(color, VoxelConstants.getMinecraft().getBlockColors().getColor(state, null, null, 0) | 0xFF000000);
            }

            if (BlockRepository.biomeBlocks.contains(block)) {
                this.applyDefaultBuiltInShading(state, color);
            } else {
                this.checkForBiomeTinting(blockPos, state, color);
            }

            if (BlockRepository.shapedBlocks.contains(block)) {
                color = this.applyShape(block, color);
            }

            if ((color >> 24 & 0xFF) < 27) {
                color |= 452984832;
            }

            return color;
        } catch (Exception var5) {
            VoxelConstants.getLogger().error("failed getting color: " + state.getBlock().getName().getString(), var5);
            return 452984832;
        }
    }

    private int getColorForBlockPosBlockStateAndFacing(BlockPos blockPos, BlockState blockState, Direction facing) {
        int color = 452984832;

        try {
            RenderShape blockRenderType = blockState.getRenderShape();
            BlockRenderDispatcher blockRendererDispatcher = VoxelConstants.getMinecraft().getBlockRenderer();
            if (blockRenderType == RenderShape.MODEL) {
                BakedModel iBakedModel = blockRendererDispatcher.getBlockModel(blockState);
                List<BakedQuad> quads = new ArrayList<>();
                quads.addAll(iBakedModel.getQuads(blockState, facing, this.random));
                quads.addAll(iBakedModel.getQuads(blockState, null, this.random));
                BlockModel model = new BlockModel(quads, this.failedToLoadX, this.failedToLoadY);
                if (model.numberOfFaces() > 0) {
                    BufferedImage modelImage = model.getImage(this.terrainBuff);
                    if (modelImage != null) {
                        color = this.getColorForCoordinatesAndImage(new float[]{0.0F, 1.0F, 0.0F, 1.0F}, modelImage);
                    } else {
                        VoxelConstants.getLogger().warn(String.format("Block texture for block %s is missing!", blockState.getBlockHolder().getRegisteredName()));
                    }
                }
            }
        } catch (Exception var11) {
            VoxelConstants.getLogger().error(var11.getMessage(), var11);
        }

        return color;
    }

    private int getColorForTerrainSprite(BlockState blockState, BlockRenderDispatcher blockRendererDispatcher) {
        BlockModelShaper blockModelShapes = blockRendererDispatcher.getBlockModelShaper();
        TextureAtlasSprite icon = blockModelShapes.getParticleIcon(blockState);
        if (icon == blockModelShapes.getModelManager().getMissingModel().getParticleIcon()) {
            Block block = blockState.getBlock();
            Block material = blockState.getBlock();
            if (block instanceof LiquidBlock) {
                if (material == Blocks.WATER) {
                    icon = VoxelConstants.getMinecraft().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.parse("minecraft:blocks/water_flow"));
                } else if (material == Blocks.LAVA) {
                    icon = VoxelConstants.getMinecraft().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.parse("minecraft:blocks/lava_flow"));
                }
            } else if (material == Blocks.WATER) {
                icon = VoxelConstants.getMinecraft().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.parse("minecraft:blocks/water_still"));
            } else if (material == Blocks.LAVA) {
                icon = VoxelConstants.getMinecraft().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ResourceLocation.parse("minecraft:blocks/lava_still"));
            }
        }

        return this.getColorForIcon(icon);
    }

    private int getColorForIcon(TextureAtlasSprite icon) {
        int color = 452984832;
        if (icon != null) {
            float left = icon.getU0();
            float right = icon.getU1();
            float top = icon.getV0();
            float bottom = icon.getV1();
            color = this.getColorForCoordinatesAndImage(new float[]{left, right, top, bottom}, this.terrainBuff);
        }

        return color;
    }

    private int getColorForCoordinatesAndImage(float[] uv, BufferedImage imageBuff) {
        int color = 452984832;
        if (uv[0] != this.failedToLoadX || uv[2] != this.failedToLoadY) {
            int left = (int) (uv[0] * imageBuff.getWidth());
            int right = (int) Math.ceil(uv[1] * imageBuff.getWidth());
            int top = (int) (uv[2] * imageBuff.getHeight());
            int bottom = (int) Math.ceil(uv[3] * imageBuff.getHeight());

            try {
                BufferedImage blockTexture = imageBuff.getSubimage(left, top, right - left, bottom - top);
                Image singlePixel = blockTexture.getScaledInstance(1, 1, 4);
                BufferedImage singlePixelBuff = new BufferedImage(1, 1, imageBuff.getType());
                Graphics gfx = singlePixelBuff.createGraphics();
                gfx.drawImage(singlePixel, 0, 0, null);
                gfx.dispose();
                color = singlePixelBuff.getRGB(0, 0);
            } catch (RasterFormatException var12) {
                VoxelConstants.getLogger().warn("error getting color");
                VoxelConstants.getLogger().warn(IntStream.of(left, right, top, bottom).mapToObj(String::valueOf).collect(Collectors.joining(" ")));
            }
        }

        return color;
    }

    private void applyDefaultBuiltInShading(BlockState blockState, int color) {
        Block block = blockState.getBlock();
        int blockStateID = BlockRepository.getStateId(blockState);
        if (block != BlockRepository.largeFern && block != BlockRepository.tallGrass && block != BlockRepository.reeds) {
            if (block == BlockRepository.water) {
                this.blockColorsWithDefaultTint[blockStateID] = ColorUtils.colorMultiplier(color, 0xFF3F76E4);
            } else {
                this.blockColorsWithDefaultTint[blockStateID] = ColorUtils.colorMultiplier(color, VoxelConstants.getMinecraft().getBlockColors().getColor(blockState, null, null, 0) | 0xFF000000);
            }
        } else {
            this.blockColorsWithDefaultTint[blockStateID] = ColorUtils.colorMultiplier(color, GrassColor.get(0.7, 0.8) | 0xFF000000);
        }

    }

    private void checkForBiomeTinting(MutableBlockPos blockPos, BlockState blockState, int color) {
        try {
            Block block = blockState.getBlock();
            String blockName = String.valueOf(BuiltInRegistries.BLOCK.getKey(block));
            if (BlockRepository.biomeBlocks.contains(block) || !blockName.startsWith("minecraft:")) {
                int tint;
                MutableBlockPos tempBlockPos = new MutableBlockPos(0, 0, 0);
                if (blockPos == this.dummyBlockPos) {
                    tint = this.tintFromFakePlacedBlock(blockState, tempBlockPos, null); // Biome 4?
                } else {
                    ClientLevel clientWorld = VoxelConstants.getClientWorld();

                    ChunkAccess chunk = clientWorld.getChunk(blockPos);
                    if (chunk != null && !((LevelChunk) chunk).isEmpty() && clientWorld.hasChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4)) {
                        tint = VoxelConstants.getMinecraft().getBlockColors().getColor(blockState, clientWorld, blockPos, 1) | 0xFF000000;
                    } else {
                        tint = this.tintFromFakePlacedBlock(blockState, tempBlockPos, null); // Biome 4?
                    }
                }

                if (tint != 16777215 && tint != -1) {
                    int blockStateID = BlockRepository.getStateId(blockState);
                    this.biomeTintsAvailable.add(blockStateID);
                    this.blockColorsWithDefaultTint[blockStateID] = ColorUtils.colorMultiplier(color, tint);
                } else {
                    this.blockColorsWithDefaultTint[BlockRepository.getStateId(blockState)] = 452984832;
                }
            }
        } catch (Exception ignored) {
        }

    }

    private int tintFromFakePlacedBlock(BlockState blockState, MutableBlockPos loopBlockPos, Biome biomeID) {
        return -1;
    }

    public int getBiomeTint(AbstractMapData mapData, Level world, BlockState blockState, int blockStateID, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, int startX, int startZ) {
        ChunkAccess chunk = world.getChunk(blockPos);
        boolean live = chunk != null && !((LevelChunk) chunk).isEmpty() && VoxelConstants.getPlayer().level().hasChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4);
        live = live && VoxelConstants.getPlayer().level().hasChunkAt(blockPos);
        int tint = -2;
        if (!live && this.biomeTintsAvailable.contains(blockStateID)) {
            try {
                int[][] tints = this.blockTintTables.get(blockStateID);
                if (tints != null) {
                    int r = 0;
                    int g = 0;
                    int b = 0;

                    for (int t = blockPos.getX() - 1; t <= blockPos.getX() + 1; ++t) {
                        for (int s = blockPos.getZ() - 1; s <= blockPos.getZ() + 1; ++s) {
                            Biome biome;
                            if (live) {
                                biome = world.getBiome(loopBlockPos.withXYZ(t, blockPos.getY(), s)).value();
                            } else {
                                int dataX = t - startX;
                                int dataZ = s - startZ;
                                dataX = Math.max(dataX, 0);
                                dataX = Math.min(dataX, mapData.getWidth() - 1);
                                dataZ = Math.max(dataZ, 0);
                                dataZ = Math.min(dataZ, mapData.getHeight() - 1);
                                biome = mapData.getBiome(dataX, dataZ);
                            }

                            if (biome == null) {
                                biome = world.registryAccess().lookupOrThrow(Registries.BIOME).get(Biomes.PLAINS).get().value();
                            }
                            int biomeID = world.registryAccess().lookupOrThrow(Registries.BIOME).getId(biome);
                            int biomeTint = tints[biomeID][loopBlockPos.y / 8];
                            r += (biomeTint & 0xFF0000) >> 16;
                            g += (biomeTint & 0xFF00) >> 8;
                            b += biomeTint & 0xFF;
                        }
                    }

                    tint = 0xFF000000 | (r / 9 & 0xFF) << 16 | (g / 9 & 0xFF) << 8 | b / 9 & 0xFF;
                }
            } catch (Exception ignored) {
            }
        }

        if (tint == -2) {
            tint = this.getBuiltInBiomeTint(mapData, world, blockState, blockStateID, blockPos, loopBlockPos, startX, startZ, live);
        }

        return ARGB.toABGR(tint);
    }

    private int getBuiltInBiomeTint(AbstractMapData mapData, Level world, BlockState blockState, int blockStateID, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, int startX, int startZ, boolean live) {
        int tint = -1;
        Block block = blockState.getBlock();
        if (BlockRepository.biomeBlocks.contains(block) || this.biomeTintsAvailable.contains(blockStateID)) {
            if (live) {
                try {
                    DebugRenderState.blockX = blockPos.x;
                    DebugRenderState.blockY = blockPos.y;
                    DebugRenderState.blockZ = blockPos.z;
                    tint = VoxelConstants.getMinecraft().getBlockColors().getColor(blockState, world, blockPos, 0) | 0xFF000000;
                } catch (Exception ignored) {
                }
            }

            if (tint == -1) {
                tint = this.getBuiltInBiomeTintFromUnloadedChunk(mapData, world, blockState, blockStateID, blockPos, loopBlockPos, startX, startZ) | 0xFF000000;
            }
        }

        return tint;
    }

    private int getBuiltInBiomeTintFromUnloadedChunk(AbstractMapData mapData, Level world, BlockState blockState, int blockStateID, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, int startX, int startZ) {
        int tint = -1;
        Block block = blockState.getBlock();
        ColorResolver colorResolver = null;
        if (block == BlockRepository.water) {
            colorResolver = this.waterColorResolver;
        } else if (block == BlockRepository.spruceLeaves) {
            colorResolver = this.spruceColorResolver;
        } else if (block == BlockRepository.birchLeaves) {
            colorResolver = this.birchColorResolver;
        } else if (block != BlockRepository.oakLeaves && block != BlockRepository.jungleLeaves && block != BlockRepository.acaciaLeaves && block != BlockRepository.darkOakLeaves && block != BlockRepository.mangroveLeaves && block != BlockRepository.vine) {
            if (block == BlockRepository.redstone) {
                colorResolver = this.redstoneColorResolver;
            } else if (BlockRepository.biomeBlocks.contains(block)) {
                colorResolver = this.grassColorResolver;
            }
        } else {
            colorResolver = this.foliageColorResolver;
        }

        if (colorResolver != null) {
            int r = 0;
            int g = 0;
            int b = 0;

            for (int t = blockPos.getX() - 1; t <= blockPos.getX() + 1; ++t) {
                for (int s = blockPos.getZ() - 1; s <= blockPos.getZ() + 1; ++s) {
                    int dataX = t - startX;
                    int dataZ = s - startZ;
                    dataX = Math.max(dataX, 0);
                    dataX = Math.min(dataX, 255);
                    dataZ = Math.max(dataZ, 0);
                    dataZ = Math.min(dataZ, 255);
                    Biome biome = mapData.getBiome(dataX, dataZ);
                    if (biome == null) {
                        MessageUtils.printDebug("Null biome ID! " + " at " + t + "," + s);
                        MessageUtils.printDebug("block: " + mapData.getBlockstate(dataX, dataZ) + ", height: " + mapData.getHeight(dataX, dataZ));
                        MessageUtils.printDebug("Mapdata: " + mapData);
                    }

                    int biomeTint = biome == null ? 0 : colorResolver.getColorAtPos(blockState, biome, loopBlockPos.withXYZ(t, blockPos.getY(), s));
                    r += (biomeTint & 0xFF0000) >> 16;
                    g += (biomeTint & 0xFF00) >> 8;
                    b += biomeTint & 0xFF;
                }
            }

            tint = (r / 9 & 0xFF) << 16 | (g / 9 & 0xFF) << 8 | b / 9 & 0xFF;
        } else if (this.biomeTintsAvailable.contains(blockStateID)) {
            tint = this.getCustomBlockBiomeTintFromUnloadedChunk(mapData, world, blockState, blockPos, loopBlockPos, startX, startZ);
        }

        return tint;
    }

    private int getCustomBlockBiomeTintFromUnloadedChunk(AbstractMapData mapData, Level world, BlockState blockState, MutableBlockPos blockPos, MutableBlockPos loopBlockPos, int startX, int startZ) {
        int tint;

        try {
            int dataX = blockPos.getX() - startX;
            int dataZ = blockPos.getZ() - startZ;
            dataX = Math.max(dataX, 0);
            dataX = Math.min(dataX, mapData.getWidth() - 1);
            dataZ = Math.max(dataZ, 0);
            dataZ = Math.min(dataZ, mapData.getHeight() - 1);
            Biome biome = mapData.getBiome(dataX, dataZ);
            tint = this.tintFromFakePlacedBlock(blockState, loopBlockPos, biome);
        } catch (Exception var12) {
            tint = -1;
        }

        return tint;
    }

    private int applyShape(Block block, int color) {
        int alpha = color >> 24 & 0xFF;
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        if (block instanceof SignBlock) {
            alpha = 31;
        } else if (block instanceof DoorBlock) {
            alpha = 47;
        } else if (block == BlockRepository.ladder || block == BlockRepository.vine) {
            alpha = 15;
        }

        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF;
    }

    @FunctionalInterface
    private interface ColorResolver {
        int getColorAtPos(BlockState state, Biome biome, BlockPos pos);
    }
}