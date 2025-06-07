package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.entityrender.variants.DefaultEntityVariantData;
import com.mamiyaotaru.voxelmap.entityrender.variants.DefaultEntityVariantDataFactory;
import com.mamiyaotaru.voxelmap.entityrender.variants.HorseVariantDataFactory;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.MessageUtils;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
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
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;

public class EntityMapImageManager {
    private final Minecraft minecraft = Minecraft.getInstance();

    private final Class<?>[] rootRenderModels = { CodModel.class, DolphinModel.class, LavaSlimeModel.class, SalmonModel.class, SlimeModel.class, TropicalFishModelA.class, TropicalFishModelB.class };
    private final TextureAtlas textureAtlas;
    public static final ResourceLocation resourceTextureAtlasMarker = ResourceLocation.fromNamespaceAndPath("voxelmap", "atlas/mobs");

    private GpuTexture fboTexture;
    private GpuTexture fboDepthTexture;
    private Tesselator fboTessellator = new Tesselator(4096);
    private final ResourceLocation resourceFboTexture = ResourceLocation.fromNamespaceAndPath("voxelmap", "entityimagemanager/fbo");

    private int imageCreationRequests;
    private int fulfilledImageCreationRequests;
    private final HashMap<String, Properties> entityProperties = new HashMap<>();
    private final HashMap<EntityType<?>, EntityVariantDataFactory> variantDataFactories = new HashMap<>();
    private ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private final RadarSettingsManager options = VoxelConstants.getVoxelMapInstance().getRadarOptions();

    public EntityMapImageManager() {
        this.textureAtlas = new TextureAtlas("mobsmap", resourceTextureAtlasMarker);

        final int fboTextureSize = 512;
        DynamicTexture fboTexture = new DynamicTexture("voxelmap-radarfbotexture", fboTextureSize, fboTextureSize, true);
        this.fboTexture = fboTexture.getTexture();
        this.fboDepthTexture = RenderSystem.getDevice().createTexture("voxelmap-radarfbodepth", TextureFormat.DEPTH32, fboTextureSize, fboTextureSize, 1);
        Minecraft.getInstance().getTextureManager().register(resourceFboTexture, fboTexture);

        reset();
    }

    public void reset() {
        MessageUtils.printDebugInfo("EntityMapImageManager: Resetting");

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
        addVariantDataFactory(new HorseVariantDataFactory(EntityType.HORSE));
//        addVariantDataFactory(new TropicalFishVariantDataFactory(EntityType.TROPICAL_FISH, null));

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
            MessageUtils.printDebugInfo("EntityMapImageManager: Existing type " + entity.getType().getDescriptionId());
            return existing;
        }
        MessageUtils.printDebugInfo("EntityMapImageManager: Rendering Mob of type " + entity.getType().getDescriptionId());

        Properties properties;
        String filePath = ("textures/icons/" + entity.getType().getDescriptionId() + ".properties").toLowerCase();
        if (entityProperties.containsKey(filePath)) {
            properties = entityProperties.get(filePath);
        } else {
            properties = new Properties();
            Optional<Resource> resource = minecraft.getResourceManager().getResource(ResourceLocation.parse(filePath));
            if (resource.isPresent()) {
                try (InputStream inputStream = resource.get().open()) {
                    properties.load(inputStream);
                } catch (Exception ignored) {
                }
            }

            entityProperties.put(filePath, properties);
        }

        Sprite sprite = textureAtlas.registerEmptyIcon(variant);
        RenderPipeline renderPipeline = GLUtils.ENTITY_ICON;
        BufferBuilder bufferBuilder = fboTessellator.begin(Mode.QUADS, renderPipeline.getVertexFormat());

        float scale = 64.0F;

        PoseStack pose = new PoseStack();
        pose.pushPose();
        pose.translate(0.0f, 0.0f, -3000.0f);
        pose.scale(scale, scale, -scale);

        rotateModel(pose, baseRenderer);

        EntityModel model = ((LivingEntityRenderer) baseRenderer).getModel();
        model.resetPose();

        for (ModelPart part : getPartToRender(model)) {
            part.xRot = 0.0F;
            part.yRot = 0.0F;
            part.zRot = 0.0F;
            part.render(pose, bufferBuilder, 15, 0, 0xFFFFFFFF); // light, overlay, color //TODO set model tint
        }

        if (baseRenderer instanceof SlimeRenderer slimeRenderer) {
            SlimeOuterLayer slimeOuter = (SlimeOuterLayer) slimeRenderer.layers.getFirst();
            slimeOuter.model.resetPose();
            slimeOuter.model.root().render(pose, bufferBuilder, 15, 0, 0xFFFFFFFF); // light, overlay, color
        }

        ResourceLocation resourceLocation = variant.getPrimaryTexture();
        ResourceLocation resourceLocation2 = variant.getSecondaryTexture();

        AbstractTexture texture = minecraft.getTextureManager().getTexture(resourceLocation);
        AbstractTexture texture2 = resourceLocation2 == null ? null : minecraft.getTextureManager().getTexture(resourceLocation2);

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

            ProjectionType originalProjectionType = RenderSystem.getProjectionType();
            Matrix4f originalProjectionMatrix = RenderSystem.getProjectionMatrix();
            RenderSystem.setProjectionMatrix(new Matrix4f().ortho(256.0F, -256.0F, -256.0F, 256.0F, 1000.0F, 21000.0F), ProjectionType.ORTHOGRAPHIC);

            try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(fboTexture, OptionalInt.of(0x00000000), fboDepthTexture, OptionalDouble.of(1.0))) {
                renderPass.setPipeline(renderPipeline);
                renderPass.bindSampler("Sampler0", texture.getTexture());
                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.setIndexBuffer(indexBuffer, indexType);
                renderPass.drawIndexed(0, meshData.drawState().indexCount());

                if (texture2 != null) {
                    renderPass.bindSampler("Sampler0", texture2.getTexture());
                    renderPass.drawIndexed(0, meshData.drawState().indexCount());
                }
            }

            RenderSystem.setProjectionMatrix(originalProjectionMatrix, originalProjectionType);

        }
        imageCreationRequests++;
        GLUtils.readTextureContentsToBufferedImage(fboTexture, image2 -> {
            postProcessRenderedMobImage(entity, sprite, model, image2, properties);
        });

        return sprite;
    }

    private void postProcessRenderedMobImage(Entity entity, Sprite sprite, EntityModel model, BufferedImage image2, Properties properties) {
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
                g.fillRect(0, 252, image.getWidth(), image.getHeight());
            }
            image = ImageUtils.trim(image);

            float scale = Float.parseFloat(properties.getProperty("scale", "1.0"));
            float targetSize = Float.parseFloat(properties.getProperty("targetSize", "0.0"));

            if (targetSize > 0.0F) {
                int maxSize = Math.max(image.getWidth(), image.getHeight());
                if (maxSize > 0) {
                    image = ImageUtils.scaleImage(image, targetSize / maxSize);
                }
            }
            image = ImageUtils.scaleImage(image, scale);
            image = ImageUtils.fillOutline(ImageUtils.pad(image), options.outlines, 2);

            BufferedImage image3 = image;
            taskQueue.add(() -> {
                fulfilledImageCreationRequests++;

                sprite.setTextureData(ImageUtils.nativeImageFromBufferedImage(image3));
                MessageUtils.printDebugInfo("EntityMapImageManager: Buffered Image (" + fulfilledImageCreationRequests + "/" + imageCreationRequests + ") added to texture atlas " + entity.getType().getDescriptionId() + " (" + image3.getWidth() + " * " + image3.getHeight() + ")");
                if (fulfilledImageCreationRequests == imageCreationRequests) {
                    textureAtlas.stitchNew();
                    MessageUtils.printDebugInfo("EntityMapImageManager: Stiching!");
                    if (VoxelConstants.DEBUG) {
                        textureAtlas.saveDebugImage();
                    }
                }
            });
        });
    }

    private void rotateModel(PoseStack pose, EntityRenderer baseRenderer) {
        if (baseRenderer instanceof AbstractHorseRenderer<?, ?, ?>) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.XP.rotationDegrees(35.0F));
        } else if (baseRenderer instanceof CodRenderer) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        } else if (baseRenderer instanceof GoatRenderer) {
            pose.mulPose(Axis.XP.rotationDegrees(20.0F));
        } else if (baseRenderer instanceof HoglinRenderer) {
            pose.mulPose(Axis.XP.rotationDegrees(60.0F));
        } else if (baseRenderer instanceof ParrotRenderer) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        } else if (baseRenderer instanceof SalmonRenderer) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        } else if (baseRenderer instanceof TropicalFishRenderer) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        } else if (baseRenderer instanceof ZoglinRenderer) {
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
            if (modelPart.hasChild("head")) return new ModelPart[] { modelPart.getChild("head") };
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
