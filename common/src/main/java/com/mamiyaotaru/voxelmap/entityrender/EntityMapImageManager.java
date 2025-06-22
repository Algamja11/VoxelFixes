package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.entityrender.variants.DefaultEntityVariantData;
import com.mamiyaotaru.voxelmap.entityrender.variants.DefaultEntityVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.HorseVariantDataFactory;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.AllocatedTexture;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapCachedOrthoProjectionMatrixBuffer;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.AbstractEquineModel;
import net.minecraft.client.model.CamelModel;
import net.minecraft.client.model.CodModel;
import net.minecraft.client.model.DolphinModel;
import net.minecraft.client.model.EndermiteModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.LavaSlimeModel;
import net.minecraft.client.model.LlamaModel;
import net.minecraft.client.model.RavagerModel;
import net.minecraft.client.model.SalmonModel;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.TropicalFishModelA;
import net.minecraft.client.model.TropicalFishModelB;
import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.CodRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.GoatRenderer;
import net.minecraft.client.renderer.entity.HoglinRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.SalmonRenderer;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.TropicalFishRenderer;
import net.minecraft.client.renderer.entity.ZoglinRenderer;
import net.minecraft.client.renderer.entity.layers.SlimeOuterLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector4f;

public class EntityMapImageManager {
    private final Minecraft minecraft = Minecraft.getInstance();

    private final Class<?>[] rootRenderModels = { CodModel.class, DolphinModel.class, LavaSlimeModel.class, SalmonModel.class, SlimeModel.class, TropicalFishModelA.class, TropicalFishModelB.class };
    private final TextureAtlas textureAtlas;
    public static final ResourceLocation resourceTextureAtlasMarker = ResourceLocation.fromNamespaceAndPath("voxelmap", "atlas/mobs");

    private int imageCreationRequests;
    private int fulfilledImageCreationRequests;
    private ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

    private final HashMap<String, Properties> entityProperties = new HashMap<>();
    private final HashMap<EntityType<?>, EntityVariantDataFactory> variantDataFactories = new HashMap<>();

    private Tesselator fboTessellator = new Tesselator(4096);
    private GpuTexture fboTexture;
    private GpuTextureView fboTextureView;
    private final ResourceLocation resourceFboTexture = ResourceLocation.fromNamespaceAndPath("voxelmap", "entityimagemanager/fbo");
    private GpuTexture fboDepthTexture;
    private GpuTextureView fboDepthTextureView;
    private VoxelMapCachedOrthoProjectionMatrixBuffer projection;

    public EntityMapImageManager() {
        this.textureAtlas = new TextureAtlas("mobsmap", resourceTextureAtlasMarker);
        this.textureAtlas.setFilter(true, false);

        final int fboTextureSize = 512;
        this.fboTexture = RenderSystem.getDevice().createTexture("voxelmap-radarfbotexture", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, fboTextureSize, fboTextureSize, 1, 1);
        this.fboDepthTexture = RenderSystem.getDevice().createTexture("voxelmap-radarfbodepth", GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.DEPTH32, fboTextureSize, fboTextureSize, 1, 1);
        Minecraft.getInstance().getTextureManager().register(resourceFboTexture, new AllocatedTexture(fboTexture));

        // this.fboTexture = fboTexture.getTexture();
        fboTextureView = RenderSystem.getDevice().createTextureView(this.fboTexture);
        fboDepthTextureView = RenderSystem.getDevice().createTextureView(this.fboDepthTexture);

        projection = new VoxelMapCachedOrthoProjectionMatrixBuffer("VoxelMap Entity Map Image Proj", 256.0F, -256.0F, -256.0F, 256.0F, 1000.0F, 21000.0F);

        reset();

    }

    public void reset() {
        if (VoxelConstants.DEBUG) {
            VoxelConstants.getLogger().info("EntityMapImageManager: Resetting");
        }

        textureAtlas.reset();
        textureAtlas.registerIconForBufferedImage("hostile", ImageUtils.loadImage(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/hostile.png"), 0, 0, 16, 16, 16, 16));
        textureAtlas.registerIconForBufferedImage("neutral", ImageUtils.loadImage(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/neutral.png"), 0, 0, 16, 16, 16, 16));
        textureAtlas.registerIconForBufferedImage("tame", ImageUtils.loadImage(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/tame.png"), 0, 0, 16, 16, 16, 16));
        textureAtlas.stitch();

        entityProperties.clear();
        variantDataFactories.clear();

        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.BOGGED, ResourceLocation.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png")));
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.DROWNED, ResourceLocation.withDefaultNamespace("textures/entity/zombie/drowned_outer_layer.png")));
        addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.ENDERMAN, ResourceLocation.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png")));
        // addVariantDataFactory(new DefaultEntityVariantDataFactory(EntityType.TROPICAL_FISH, ResourceLocation.withDefaultNamespace("textures/entity/enderman/enderman_eyes.png")));
        addVariantDataFactory(new HorseVariantDataFactory(EntityType.HORSE));
        if (VoxelConstants.DEBUG) {
            BuiltInRegistries.ENTITY_TYPE.forEach(t -> {
                requestImageForMobType(t, 32, true);
            });
        }
    }

    private void addVariantDataFactory(EntityVariantDataFactory factory) {
        variantDataFactories.put(factory.getType(), factory);
    }

    public Sprite requestImageForMobType(EntityType<?> type) {
        return requestImageForMobType(type, -1, true);
    }

    public Sprite requestImageForMobType(EntityType<?> type, int size, boolean addBorder) {
        if (minecraft.level != null && type.create(minecraft.level, EntitySpawnReason.LOAD) instanceof LivingEntity le) {
            return requestImageForMob(le, size, addBorder);
        }
        return null;
    }

    public Sprite requestImageForMob(LivingEntity e) {
        return requestImageForMob(e, -1, true);
    }

    private EntityVariantData getVariantData(Entity entity, EntityRenderer renderer, EntityRenderState state, int size, boolean addBorder) {
        EntityVariantDataFactory factory = variantDataFactories.get(entity.getType());
        if (factory != null) {
            EntityVariantData data = factory.createVariantData(entity, renderer, state, size, addBorder);
            if (data != null) {
                return data;
            }
        }
        return DefaultEntityVariantDataFactory.createSimpleVariantData(entity, renderer, state, size, addBorder);
    }

    public Sprite requestImageForMob(Entity entity, int size, boolean addBorder) {
        EntityRenderer<?, ?> baseRenderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
        EntityVariantData variant = null;
        EntityRenderState renderState = null;
        if (entity instanceof AbstractClientPlayer player) {
            variant = new DefaultEntityVariantData(entity.getType(), player.getSkin().texture(), null, size, addBorder);
        } else if (entity instanceof LivingEntity && baseRenderer instanceof LivingEntityRenderer renderer) {
            renderState = renderer.createRenderState(entity, 0.5f);
            variant = getVariantData(entity, renderer, renderState, size, addBorder);
        }
        if (variant == null) {
            return null;
        }
        Sprite existing = textureAtlas.getAtlasSpriteIncludingYetToBeStitched(variant);
        if (existing != null && existing != textureAtlas.getMissingImage()) {
            // VoxelConstants.getLogger().info("EntityMapImageManager: Existing type " + entity.getType().getDescriptionId());
            return existing;
        }
        if (VoxelConstants.DEBUG) {
            VoxelConstants.getLogger().info("EntityMapImageManager: Rendering Mob of type " + entity.getType().getDescriptionId());
        }

        Sprite sprite = textureAtlas.registerEmptyIcon(variant);

        ResourceLocation resourceLocation = variant.getPrimaryTexture();
        ResourceLocation resourceLocation2 = variant.getSecondaryTexture();

        RenderPipeline renderPipeline = GLUtils.ENTITY_ICON;
        BufferBuilder bufferBuilder = fboTessellator.begin(Mode.QUADS, renderPipeline.getVertexFormat());

        float scale = 64;

        PoseStack pose = new PoseStack();
        pose.pushPose();
        pose.translate(0.0f, 0.0f, -3000.0f);
        pose.scale(scale, scale, -scale);

        rotatePart(pose, baseRenderer);

        EntityModel model = ((LivingEntityRenderer) baseRenderer).getModel();
        model.resetPose();

        for (ModelPart part : getPartToRender(model)) {
            part.xRot = 0;
            part.yRot = 0;
            part.zRot = 0;
            part.render(pose, bufferBuilder, 15, 0, 0xffffffff); // light, overlay, color //TODO set model tint
        }

        if (baseRenderer instanceof SlimeRenderer slimeRenderer) {
            SlimeOuterLayer slimeOuter = (SlimeOuterLayer) slimeRenderer.layers.get(0);
            slimeOuter.model.setupAnim(renderState);
            slimeOuter.model.root().render(pose, bufferBuilder, 15, 0, 0xffffffff); // light, overlay, color
        }

        AbstractTexture texture = minecraft.getTextureManager().getTexture(resourceLocation);
        AbstractTexture texture2 = resourceLocation2 == null ? null : minecraft.getTextureManager().getTexture(resourceLocation2);

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
                .writeTransform(
                        RenderSystem.getModelViewMatrix(),
                        new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                        RenderSystem.getModelOffset(),
                        RenderSystem.getTextureMatrix(),
                        RenderSystem.getShaderLineWidth());

        try (MeshData meshData = bufferBuilder.build()) {
            GpuBuffer vertexBuffer = renderPipeline.getVertexFormat().uploadImmediateVertexBuffer(meshData.vertexBuffer());
            GpuBuffer indexBuffer;
            VertexFormat.IndexType indexType;
            if (meshData.indexBuffer() == null) {
                RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().mode());
                indexBuffer = autoStorageIndexBuffer.getBuffer(meshData.drawState().indexCount());
                indexType = autoStorageIndexBuffer.type();
            } else {
                indexBuffer = renderPipeline.getVertexFormat().uploadImmediateIndexBuffer(meshData.indexBuffer());
                indexType = meshData.drawState().indexType();
            }

            // float size = 64.0F * scale;
            int width = fboTexture.getWidth(0);
            int height = fboTexture.getHeight(0);
            ProjectionType originalProjectionType = RenderSystem.getProjectionType();
            GpuBufferSlice originalProjectionMatrix = RenderSystem.getProjectionMatrixBuffer();
            RenderSystem.setProjectionMatrix(projection.getBuffer(), ProjectionType.ORTHOGRAPHIC);

            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "VoxelMap entity image renderer", fboTextureView, OptionalInt.of(0x00000000), fboDepthTextureView, OptionalDouble.of(1.0))) {
                renderPass.setPipeline(renderPipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                renderPass.bindSampler("Sampler0", texture.getTextureView());
                // renderPass.bindSampler("Sampler1", texture.getTexture()); // overlay
                // minecraft.gameRenderer.overlayTexture().setupOverlayColor();
                // renderPass.bindSampler("Sampler2", texture.getTexture()); // lightmap
                // minecraft.gameRenderer.lightTexture().turnOnLightLayer();
                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.setIndexBuffer(indexBuffer, indexType);
                renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);

                if (texture2 != null) {
                    renderPass.bindSampler("Sampler0", texture2.getTextureView());
                    renderPass.drawIndexed(0, 0, meshData.drawState().indexCount(), 1);
                }
            }
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.setProjectionMatrix(originalProjectionMatrix, originalProjectionType);

        }
        imageCreationRequests++;
        GLUtils.readTextureContentsToBufferedImage(fboTexture, image2 -> {
            postProcessRenderedMobImage(entity, sprite, model, image2);
        });

        return sprite;
    }

    private void postProcessRenderedMobImage(Entity entity, Sprite sprite, EntityModel model, BufferedImage image2) {
        Util.backgroundExecutor().execute(() -> {
            BufferedImage image = image2;
            image = ImageUtils.flipHorizontal(image);
            if (model instanceof CamelModel) {
                Graphics2D g = image.createGraphics();
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0, 192, image.getWidth(), image.getHeight());
            }
            if (model instanceof LlamaModel) {
                Graphics2D g = image.createGraphics();
                g.setComposite(AlphaComposite.Clear);
                g.fillRect(0, 248, image.getWidth(), image.getHeight());
            }
            image = ImageUtils.trim(image);

            Properties properties = getMobProperties(entity);
            float scale = Float.parseFloat(properties.getProperty("scale", "1.0"));
            int targetSize = Integer.parseInt(properties.getProperty("targetSize", "0"));

            int maxSize = Math.max(image.getHeight(), image.getWidth());
            float scaleBy = ((targetSize > 0 && maxSize > 0) ? ((float) targetSize / maxSize) : 1.0F) * scale;

            if (scaleBy != 1.0F) {
                image = ImageUtils.scaleImage(image, scaleBy);
            }
            image = ImageUtils.fillOutline(ImageUtils.pad(image), true, 2);

            BufferedImage image3 = image;
            taskQueue.add(() -> {
                fulfilledImageCreationRequests++;

                sprite.setTextureData(ImageUtils.nativeImageFromBufferedImage(image3));
                if (VoxelConstants.DEBUG) {
                    VoxelConstants.getLogger().info("EntityMapImageManager: Buffered Image (" + fulfilledImageCreationRequests + "/" + imageCreationRequests + ") added to texture atlas " + entity.getType().getDescriptionId() + " (" + image3.getWidth() + " * " + image3.getHeight() + ")");
                }
                if (fulfilledImageCreationRequests == imageCreationRequests) {
                    textureAtlas.stitchNew();
                    if (VoxelConstants.DEBUG) {
                        VoxelConstants.getLogger().info("EntityMapImageManager: Stiching!");
                        textureAtlas.saveDebugImage();
                    }
                }
            });
        });
    }

    private Properties getMobProperties(Entity entity) {
        String filePath = ("textures/icons/" + entity.getType().getDescriptionId() + ".properties");
        if (entityProperties.containsKey(filePath)) {
            return entityProperties.get(filePath);
        } else {
            Properties properties = new Properties();
            Optional<Resource> resource = minecraft.getResourceManager().getResource(ResourceLocation.parse(filePath));
            if (resource.isPresent()) {
                try (InputStream inputStream = resource.get().open()) {
                    properties.load(inputStream);
                } catch (Exception e){
                }
            }

            entityProperties.put(filePath, properties);

            return properties;
        }
    }

    private void rotatePart(PoseStack pose, EntityRenderer renderer) {
        if (renderer instanceof AbstractHorseRenderer<?, ?, ?>) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.XP.rotationDegrees(35.0F));
        } else if (renderer instanceof CodRenderer) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        } else if (renderer instanceof GoatRenderer) {
            pose.mulPose(Axis.XP.rotationDegrees(20.0F));
        } else if (renderer instanceof HoglinRenderer) {
            pose.mulPose(Axis.XP.rotationDegrees(60.0F));
        } else if (renderer instanceof ParrotRenderer) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        } else if (renderer instanceof SalmonRenderer) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        } else if (renderer instanceof TropicalFishRenderer) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        } else if (renderer instanceof ZoglinRenderer) {
            pose.mulPose(Axis.XP.rotationDegrees(60.0F));
        }
    }

    private ModelPart[] getPartToRender(EntityModel<?> model) {
        for (Class<?> clazz : rootRenderModels) {
            if (clazz.isInstance(model)) {
                return new ModelPart[] { model.root() };
            }
        }

        if (model instanceof AbstractEquineModel<?> equineModel) {
            return new ModelPart[] { equineModel.headParts };
        } else if (model instanceof EndermiteModel endermiteModel) {
            return new ModelPart[] { endermiteModel.root().getChild("segment0"), endermiteModel.root().getChild("segment1") };
        } else if (model instanceof RavagerModel ravagerModel) {
            return new ModelPart[] { ravagerModel.root().getChild("neck").getChild("head") };
        } else if (model instanceof SilverfishModel silverfishModel) {
            return new ModelPart[] { silverfishModel.root().getChild("segment0"), silverfishModel.root().getChild("segment1") };
        } else if (model instanceof WitherBossModel witherModel) {
            return new ModelPart[]{witherModel.root().getChild("left_head"), witherModel.root().getChild("center_head"), witherModel.root().getChild("right_head")};
        }

        for (ModelPart modelPart : model.allParts()) {
            if (modelPart.hasChild("head")) {
                return new ModelPart[] { modelPart.getChild("head") };
            }
            if (modelPart.hasChild("body")) {
                ModelPart bodyPart = modelPart.getChild("body");
                if (bodyPart.hasChild("head")) {
                    return new ModelPart[] { bodyPart.getChild("head") };
                }

                return new ModelPart[] { bodyPart };
            }
        }

        return new ModelPart[] { model.root() };
    }

    public void onRenderTick(GuiGraphics drawContext) {
        Runnable task;
        while ((task = taskQueue.poll()) != null) {
            task.run();
        }
    }
}
