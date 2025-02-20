package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.StitcherException;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.CustomMob;
import com.mamiyaotaru.voxelmap.util.CustomMobsManager;
import com.mamiyaotaru.voxelmap.util.EnumMobs;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mamiyaotaru.voxelmap.util.ReflectionUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.AxolotlModel;
import net.minecraft.client.model.BeeModel;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.CodModel;
import net.minecraft.client.model.CreakingModel;
import net.minecraft.client.model.DolphinModel;
import net.minecraft.client.model.DonkeyModel;
import net.minecraft.client.model.DrownedModel;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.EndermiteModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.GoatModel;
import net.minecraft.client.model.HoglinModel;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.LavaSlimeModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.RavagerModel;
import net.minecraft.client.model.SalmonModel;
import net.minecraft.client.model.ShulkerModel;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.SnifferModel;
import net.minecraft.client.model.SnowGolemModel;
import net.minecraft.client.model.TropicalFishModelA;
import net.minecraft.client.model.TropicalFishModelB;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.WardenModel;
import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.metadata.animation.VillagerMetadataSection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ResolvableProfile;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Radar implements IRadar {
    public final RadarSettingsManager options;
    public final MapSettingsManager minimapOptions;
    private LayoutVariables layoutVariables;

    private final TextureAtlas textureAtlas;
    private int timer = 500;
    private float direction;
    private boolean newMobs;
    private boolean completedLoading;
    private boolean lastOutlines = true;

    private final ArrayList<Contact> contacts = new ArrayList<>(40);
    public final HashMap<String, Integer> contactsSkinGetTries = new HashMap<>();
    public final HashMap<String, Integer> mpContactsSkinGetTries = new HashMap<>();
    private static final HashMap<String, BufferedImage> entityIconMap = new HashMap<>();

    private Sprite leatherArmorIcon;
    private SkullModel playerSkullModel;
    private HumanoidModel<HumanoidRenderState> bipedArmorModel;
    private HumanoidModel<HumanoidRenderState> piglinArmorModel;
    private final String[] headIDs = {"SKELETON_HEAD", "WITHER_SKELETON_HEAD", "ZOMBIE_HEAD", "CREEPER_HEAD", "ENDER_DRAGON_HEAD", "PIGLIN_HEAD", "SHEEP_FUR", "SLIME_OVERLAY"};
    private final String[] armorNames = {"leather", "leatherOverlay", "leatherOuter", "leatherOverlayOuter", "chain", "iron", "gold", "diamond", "netherite", "turtle"};

    private DynamicTexture nativeBackedTexture = new DynamicTexture(2, 2, false);
    private final ResourceLocation nativeBackedTextureLocation = ResourceLocation.fromNamespaceAndPath("voxelmap", "tempimage");
    private static final Int2ObjectMap<ResourceLocation> villagerLevelID = Util.make(new Int2ObjectOpenHashMap<>(), int2ObjectOpenHashMap -> {
        int2ObjectOpenHashMap.put(1, ResourceLocation.parse("stone"));
        int2ObjectOpenHashMap.put(2, ResourceLocation.parse("iron"));
        int2ObjectOpenHashMap.put(3, ResourceLocation.parse("gold"));
        int2ObjectOpenHashMap.put(4, ResourceLocation.parse("emerald"));
        int2ObjectOpenHashMap.put(5, ResourceLocation.parse("diamond"));
    });

    public Radar() {
        this.minimapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
        this.textureAtlas = new TextureAtlas("mobs");
        this.textureAtlas.setFilter(false, false);
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.loadTexturePackIcons();
    }

    private void loadTexturePackIcons() {
        this.completedLoading = false;

        try {
            entityIconMap.clear();
            this.mpContactsSkinGetTries.clear();
            this.contactsSkinGetTries.clear();
            this.textureAtlas.reset();

            ModelPart skullModelPart = SkullModel.createHumanoidHeadLayer().bakeRoot();
            this.playerSkullModel = new SkullModel(skullModelPart);

            LayerDefinition bipedArmorLayer = LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.0F), 0.0F), 64, 32);
            this.bipedArmorModel = new HumanoidModel<>(bipedArmorLayer.bakeRoot());

            LayerDefinition piglinArmorLayer = LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(2.0F), 0.0F), 64, 32);
            this.piglinArmorModel = new HumanoidModel<>(piglinArmorLayer.bakeRoot());

            for (int t = 0; t < EnumMobs.values().length - 1; ++t) {
                String identifier = "minecraft." + EnumMobs.values()[t].id;
                String identifierSimple = EnumMobs.values()[t].id;
                String spriteName = identifier + EnumMobs.values()[t].resourceLocation.toString();
                spriteName = spriteName + (EnumMobs.values()[t].secondaryResourceLocation != null ? EnumMobs.values()[t].secondaryResourceLocation.toString() : "");
                BufferedImage mobImage = this.getCustomMobImage(identifier, identifierSimple);
                if (mobImage != null) {
                    Sprite sprite = this.textureAtlas.registerIconForBufferedImage(identifier + "custom", mobImage);
                    this.textureAtlas.registerMaskedIcon(spriteName, sprite);
                } else {
                    this.textureAtlas.registerFailedIcon(identifier + "custom");
                    if (EnumMobs.values()[t].expectedWidth > 0.5) {
                        mobImage = this.createPreMappedIcon(EnumMobs.values()[t], EnumMobs.values()[t].resourceLocation);
                        if (mobImage != null) {
                            float scale = mobImage.getWidth() / EnumMobs.values()[t].expectedWidth;
                            mobImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(mobImage, 4.0F / scale)), this.options.outlines, 2);
                            this.textureAtlas.registerIconForBufferedImage(spriteName, mobImage);
                        }
                    }
                }
            }

            BufferedImage[] leatherImages = {
                    ImageUtils.loadImage(ResourceLocation.parse("textures/entity/equipment/humanoid/leather.png"), 8, 8, 8, 8),
                    ImageUtils.loadImage(ResourceLocation.parse("textures/entity/equipment/humanoid/leather.png"), 40, 8, 8, 8),
                    ImageUtils.loadImage(ResourceLocation.parse("textures/entity/equipment/humanoid/leather_overlay.png"), 8, 8, 8, 8),
                    ImageUtils.loadImage(ResourceLocation.parse("textures/entity/equipment/humanoid/leather_overlay.png"), 40, 8, 8, 8)
            };

            for (int i = 0; i < leatherImages.length; i++) {
                float scale = leatherImages[i].getWidth() / 8.0F;
                leatherImages[i] = ImageUtils.fillOutline(
                        ImageUtils.pad(ImageUtils.scaleImage(leatherImages[i], 4.0F / scale * 47.0F / 38.0F)), this.options.outlines && i < 2, true, 37.6F, 37.6F, 2
                );

                Sprite icon = this.textureAtlas.registerIconForBufferedImage("armor " + this.armorNames[i], leatherImages[i]);
                if (i == 0) { this.leatherArmorIcon = icon; }
            }

            BufferedImage skeleton = ImageUtils.loadImage(EnumMobs.SKELETON.resourceLocation, 8, 8, 8, 8, 64, 32);
            skeleton = ImageUtils.scaleImage(skeleton, 5f);
            skeleton = ImageUtils.fillOutline(ImageUtils.pad(skeleton), this.options.outlines, true, 40.0f, 40.0f, 2);
            this.textureAtlas.registerIconForBufferedImage(headIDs[0], skeleton);

            BufferedImage witherSkeleton = ImageUtils.loadImage(EnumMobs.WITHER_SKELETON.resourceLocation, 8, 8, 8, 8, 64, 32);
            witherSkeleton = ImageUtils.scaleImage(witherSkeleton, 5f);
            witherSkeleton = ImageUtils.fillOutline(ImageUtils.pad(witherSkeleton), this.options.outlines, true, 40.0f, 40.0f, 2);
            this.textureAtlas.registerIconForBufferedImage(headIDs[1], witherSkeleton);

            BufferedImage zombie = ImageUtils.loadImage(EnumMobs.ZOMBIE.resourceLocation, 8, 8, 8, 8, 64, 64);
            zombie = ImageUtils.scaleImage(zombie, 5f);
            zombie = ImageUtils.fillOutline(ImageUtils.pad(zombie), this.options.outlines, true, 40.0f, 40.0f, 2);
            this.textureAtlas.registerIconForBufferedImage(headIDs[2], zombie);

            BufferedImage creeper = ImageUtils.loadImage(EnumMobs.CREEPER.resourceLocation, 8, 8, 8, 8, 64, 64);
            creeper = ImageUtils.scaleImage(creeper, 5f);
            creeper = ImageUtils.fillOutline(ImageUtils.pad(creeper), this.options.outlines, true, 40.0f, 40.0f, 2);
            this.textureAtlas.registerIconForBufferedImage(headIDs[3], creeper);

            BufferedImage dragon = createPreMappedIcon(EnumMobs.ENDER_DRAGON, EnumMobs.ENDER_DRAGON.resourceLocation);
            dragon = ImageUtils.scaleImage(dragon, 2.5f);
            dragon = ImageUtils.fillOutline(ImageUtils.pad(dragon), this.options.outlines, true, 20.0f, 25.0f, 2);
            this.textureAtlas.registerIconForBufferedImage(headIDs[4], dragon);

            BufferedImage piglin = createPreMappedIcon(EnumMobs.PIGLIN, EnumMobs.PIGLIN.resourceLocation);
            piglin = ImageUtils.scaleImage(piglin, 5f);
            piglin = ImageUtils.fillOutline(ImageUtils.pad(piglin), this.options.outlines, true, 50.0f, 40.0f, 2);
            this.textureAtlas.registerIconForBufferedImage(headIDs[5], piglin);

            BufferedImage sheepFur = ImageUtils.loadImage(ResourceLocation.parse("textures/entity/sheep/sheep_fur.png"), 6, 6, 6, 6);
            float scale = sheepFur.getWidth() / 6.0F;
            sheepFur = ImageUtils.scaleImage(sheepFur, 4.0F / scale * 1.0625F);
            int chop = 2; //(int) Math.max(1.0F, 2.0F); ??????
            ImageUtils.eraseArea(sheepFur, chop, chop, sheepFur.getWidth() - chop * 2, sheepFur.getHeight() - chop * 2, sheepFur.getWidth(), sheepFur.getHeight());
            sheepFur = ImageUtils.fillOutline(ImageUtils.pad(sheepFur), this.options.outlines, true, 27.5F, 27.5F, (int) Math.max(1.0F, 2.0F));
            this.textureAtlas.registerIconForBufferedImage(headIDs[6], sheepFur);

            BufferedImage slimeOverlay = ImageUtils.loadImage(EnumMobs.SLIME.resourceLocation, 8, 8, 8, 8, 64, 32);
            slimeOverlay = ImageUtils.scaleImage(slimeOverlay, 4.5f);
            slimeOverlay = ImageUtils.fillOutline(ImageUtils.pad(slimeOverlay), this.options.outlines, true, 36.0f, 36.0f, 2);
            this.textureAtlas.registerIconForBufferedImage(headIDs[7], slimeOverlay);

            this.textureAtlas.stitch();
            applyFilteringParameters();
            this.completedLoading = true;
        } catch (Exception var30) {
            VoxelConstants.getLogger().error("Failed getting mobs" + var30.getLocalizedMessage(), var30);
        }

    }

    private BufferedImage createPreMappedIcon(EnumMobs type, ResourceLocation resourceLocation) {
        BufferedImage mobImage = ImageUtils.createBufferedImageFromResourceLocation(resourceLocation);
        try {
            return this.createPreMappedIconFromImage(type, mobImage);
        } catch (Exception var8) {
            return null;
        }
    }

    private BufferedImage createPreMappedIconFromImage(EnumMobs type, BufferedImage mobImage) {
        BufferedImage image = switch (type) {
            case GENERIC_HOSTILE ->
                    ImageUtils.loadImage(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/hostile.png"), 0, 0, 16, 16, 16, 16);
            case GENERIC_NEUTRAL ->
                    ImageUtils.loadImage(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/neutral.png"), 0, 0, 16, 16, 16, 16);
            case GENERIC_TAME ->
                    ImageUtils.loadImage(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/tame.png"), 0, 0, 16, 16, 16, 16);
            case CHICKEN ->
                    ImageUtils.addImages(ImageUtils.addImages(
                            ImageUtils.loadImage(mobImage, 2, 3, 6, 6),
                            ImageUtils.loadImage(mobImage, 16, 2, 4, 2), 1.0F, 2.0F, 6, 6),
                            ImageUtils.loadImage(mobImage, 16, 6, 2, 2), 2.0F, 4.0F, 6, 6
                    );
            case ENDER_DRAGON ->
                    ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(
                            ImageUtils.blankImage(mobImage, 16, 20, 256, 256),
                            ImageUtils.loadImage(mobImage, 128, 46, 16, 16, 256, 256), 0, 4, 16, 20),
                            ImageUtils.flipHorizontal(ImageUtils.loadImage(mobImage, 6, 6, 2, 4, 256, 256)), 3, 0, 16, 20),
                            ImageUtils.loadImage(mobImage, 6, 6, 2, 4, 256, 256), 11, 0, 16, 20),
                            ImageUtils.loadImage(mobImage, 192, 60, 12, 4, 256, 256), 2, 11, 16, 20),
                            ImageUtils.loadImage(mobImage, 192, 81, 12, 4, 256, 256), 2, 16, 16, 20
                    );
            case PIGLIN ->
                    ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(ImageUtils.addImages(
                            ImageUtils.blankImage(mobImage, 10, 8, 64, 64),
                            ImageUtils.loadImage(mobImage, 8, 8, 10, 8, 64, 64), 0, 0, 10, 8),
                            ImageUtils.loadImage(mobImage, 32, 2, 4, 4, 64, 64), 3, 4, 10, 8),
                            ImageUtils.loadImage(mobImage, 3, 1, 1, 2, 64, 64), 2, 6, 10, 8),
                            ImageUtils.loadImage(mobImage, 3, 5, 1, 2, 64, 64), 7, 6, 10, 8
                    );
            default -> null;
        };

        mobImage.flush();

        return image;
    }

    @Override
    public void onTickInGame(GuiGraphics drawContext, Matrix4fStack matrixStack, LayoutVariables layoutVariables, float scaleProj) {
        if (this.options.radarAllowed || this.options.radarMobsAllowed || this.options.radarPlayersAllowed) {
            this.layoutVariables = layoutVariables;
            if (this.options.isChanged()) {
                this.timer = 500;
                if (this.options.outlines != this.lastOutlines) {
                    this.lastOutlines = this.options.outlines;
                    this.loadTexturePackIcons();
                }
            }

            this.direction = GameVariableAccessShim.rotationYaw() + 180.0F;
            while (this.direction < 0.0F) {
                this.direction += 360.0F;
            }
            while (this.direction >= 360.0F) {
                this.direction -= 360.0F;
            }

            if (this.completedLoading && this.timer > 95) {
                this.calculateMobs();
                this.timer = 0;
            }
            ++this.timer;
            if (this.completedLoading) {
                this.renderMapMobs(drawContext, matrixStack, this.layoutVariables.mapX, this.layoutVariables.mapY, scaleProj);
            }

            OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        }
    }

    private boolean isEntityShown(Entity entity) {
        return entity != null && !entity.isInvisibleTo(VoxelConstants.getPlayer()) && (this.options.showHostiles && (this.options.radarAllowed || this.options.radarMobsAllowed) && this.isHostile(entity)
                || this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed) && this.isPlayer(entity) || this.options.showNeutrals && this.options.radarMobsAllowed && this.isNeutral(entity));
    }

    public void calculateMobs() {
        this.contacts.clear();

        Iterable<Entity> entities = VoxelConstants.getClientWorld().entitiesForRendering();

        for (Entity entity : entities) {
            try {
                if (this.isEntityShown(entity)) {
                    int wayX = GameVariableAccessShim.xCoord() - (int) entity.position().x();
                    int wayZ = GameVariableAccessShim.zCoord() - (int) entity.position().z();
                    int wayY = GameVariableAccessShim.yCoord() - (int) entity.position().y();
                    double hypot = wayX * wayX + wayZ * wayZ + wayY * wayY;
                    hypot /= this.layoutVariables.zoomScaleAdjusted * this.layoutVariables.zoomScaleAdjusted;
                    if (hypot < 961.0) {
                        Contact contact = new Contact((LivingEntity) entity, EnumMobs.getMobTypeByEntity(entity));
                        String unscrubbedName = TextUtils.asFormattedString(contact.entity.getDisplayName());
                        contact.setName(unscrubbedName);
                        if (contact.entity.getVehicle() != null && this.isEntityShown(contact.entity.getVehicle())) {
                            contact.yFudge = 1;
                        }

                        contact.updateLocation();
                        boolean enabled = false;
                        if (!contact.vanillaType) {
                            String type = entity.getType().getDescriptionId();
                            CustomMob customMob = CustomMobsManager.getCustomMobByType(type);
                            if (customMob == null || customMob.enabled) {
                                enabled = true;
                            }
                        } else if (contact.type.enabled) {
                            enabled = true;
                        }

                        if (enabled) {
                            if (contact.type == EnumMobs.PLAYER) this.handleMPplayer(contact);
                            if (contact.icon == null) this.tryCustomIcon(contact);
                            if (contact.icon == null) this.tryAutoIcon(contact);
                            if (contact.icon == null) this.getGenericIcon(contact);
                            if (contact.type == EnumMobs.HORSE) contact.setRotationFactor(-45);

                            String scrubbedName = TextUtils.scrubCodes(contact.entity.getName().getString());
                            if ((scrubbedName.equals("Dinnerbone") || scrubbedName.equals("Grumm")) && (!(contact.entity instanceof Player) || ((Player) contact.entity).isModelPartShown(PlayerModelPart.CAPE))) {
                                contact.setRotationFactor(contact.rotationFactor + 180);
                            }
                            if (this.options.showHelmetsPlayers && contact.type == EnumMobs.PLAYER || this.options.showHelmetsMobs && contact.type != EnumMobs.PLAYER || contact.type == EnumMobs.SHEEP) {
                                this.getArmor(contact, entity);
                            }

                            this.contacts.add(contact);
                        }
                    }
                }
            } catch (Exception var16) {
                VoxelConstants.getLogger().error(var16.getLocalizedMessage(), var16);
            }
        }

        if (this.newMobs) {
            try {
                this.textureAtlas.stitchNew();
                applyFilteringParameters();
            } catch (StitcherException var14) {
                VoxelConstants.getLogger().warn("Stitcher exception!  Resetting mobs texture atlas.");
                this.loadTexturePackIcons();
            }
        }

        this.newMobs = false;
        this.contacts.sort(Comparator.comparingInt(contact -> contact.y));
    }

    private void tryCustomIcon(Contact contact) {
        String identifier = contact.vanillaType ? "minecraft." + contact.type.id : contact.entity.getClass().getName();
        String identifierSimple = contact.vanillaType ? contact.type.id : contact.entity.getClass().getSimpleName();
        Sprite icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(identifier + "custom");
        if (icon == this.textureAtlas.getMissingImage()) {
            boolean isHostile = this.isHostile(contact.entity);
            CustomMobsManager.add(contact.entity.getType().getDescriptionId(), isHostile, !isHostile);
            BufferedImage mobSkin = this.getCustomMobImage(identifier, identifierSimple);
            if (mobSkin != null) {
                icon = this.textureAtlas.registerIconForBufferedImage(identifier + "custom", mobSkin);
                this.newMobs = true;
                contact.icon = icon;
                contact.custom = true;
            } else {
                this.textureAtlas.registerFailedIcon(identifier + "custom");
            }
        } else if (icon != this.textureAtlas.getFailedImage()) {
            contact.custom = true;
            contact.icon = icon;
        }
    }

    private BufferedImage getCustomMobImage(String identifier, String identifierSimple) {
        BufferedImage mobSkin = null;

        try {
            int intendedSize = 8;
            String fullPath = ("textures/icons/" + identifier + ".png").toLowerCase();
            InputStream inputStream = null;

            try {
                inputStream = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
            } catch (IOException ignored) {}

            if (inputStream == null) {
                fullPath = ("textures/icons/" + identifierSimple + ".png").toLowerCase();
                try {
                    inputStream = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
                } catch (IOException ignored) {}
            }

            if (inputStream == null) {
                fullPath = ("textures/icons/" + identifier + "8.png").toLowerCase();
                try {
                    inputStream = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
                } catch (IOException ignored) {}
            }

            if (inputStream == null) {
                fullPath = ("textures/icons/" + identifierSimple + "8.png").toLowerCase();
                try {
                    inputStream = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
                } catch (IOException ignored) {}
            }

            if (inputStream == null) {
                intendedSize = 16;
                fullPath = ("textures/icons/" + identifier + "16.png").toLowerCase();
                try {
                    inputStream = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
                } catch (IOException ignored) {}
            }

            if (inputStream == null) {
                fullPath = ("textures/icons/" + identifierSimple + "16.png").toLowerCase();
                try {
                    inputStream = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
                } catch (IOException ignored) {}
            }

            if (inputStream == null) {
                intendedSize = 32;
                fullPath = ("textures/icons/" + identifier + "32.png").toLowerCase();
                try {
                    inputStream = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
                } catch (IOException ignored) {}
            }

            if (inputStream == null) {
                fullPath = ("textures/icons/" + identifierSimple + "32.png").toLowerCase();
                try {
                    inputStream = VoxelConstants.getMinecraft().getResourceManager().getResource(ResourceLocation.parse(fullPath)).get().open();
                } catch (IOException ignored) {}
            }

            if (inputStream != null) {
                mobSkin = ImageIO.read(inputStream);
                inputStream.close();
                mobSkin = ImageUtils.validateImage(mobSkin);
                float scale = (float) mobSkin.getWidth() / intendedSize;
                mobSkin = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(mobSkin, 4.0F / scale)), this.options.outlines, 2);
            }
        } catch (Exception var16) {
            mobSkin = null;
        }

        return mobSkin;
    }

    private void tryAutoIcon(Contact contact) {
        if (contact.type == EnumMobs.ENDER_DRAGON) {
            contact.icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(headIDs[4]);
            return;
        }

        String variantString = "";
        EntityRenderer<LivingEntity, LivingEntityRenderState> render = (EntityRenderer<LivingEntity, LivingEntityRenderState>) VoxelConstants.getMinecraft().getEntityRenderDispatcher().getRenderer(contact.entity);
        ResourceLocation resourceLocationPrimary = ((LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) render).getTextureLocation(render.createRenderState());
        ResourceLocation resourceLocationSecondary = null;
        ResourceLocation resourceLocationTertiary = null;
        ResourceLocation resourceLocationQuaternary = null;

        switch (contact.type) {
            case AXOLOTL -> {
                Axolotl axolotl = (Axolotl) contact.entity;
                resourceLocationPrimary = switch (axolotl.getVariant()) {
                    case BLUE -> ResourceLocation.parse("textures/entity/axolotl/axolotl_blue.png");
                    case CYAN -> ResourceLocation.parse("textures/entity/axolotl/axolotl_cyan.png");
                    case GOLD -> ResourceLocation.parse("textures/entity/axolotl/axolotl_gold.png");
                    case LUCY -> ResourceLocation.parse("textures/entity/axolotl/axolotl_lucy.png");
                    case WILD -> ResourceLocation.parse("textures/entity/axolotl/axolotl_wild.png");
                };
            }
            case BEE -> {
                Bee bee = (Bee)contact.entity;
                if (bee.isAngry()) resourceLocationPrimary = ResourceLocation.parse("textures/entity/bee/bee_angry.png");
                else resourceLocationPrimary = ResourceLocation.parse("textures/entity/bee/bee.png");
            }
            case CAT -> {
                Cat cat = (Cat) contact.entity;
                String variant = cat.getVariant().getRegisteredName();
                variant = variant.substring(variant.indexOf(':') + 1);
                resourceLocationPrimary = ResourceLocation.parse("textures/entity/cat/" + variant + ".png");
            }
            case FOX -> {
                Fox fox = (Fox)contact.entity;
                String resLocationName;
                resLocationName = switch (fox.getVariant()) {
                    case RED -> "textures/entity/fox/fox";
                    case SNOW -> "textures/entity/fox/snow_fox";
                };
                if (fox.isSleeping()) resLocationName += "_sleep";
                resLocationName += ".png";
                resourceLocationPrimary = ResourceLocation.parse(resLocationName);
            }
            case FROG -> {
                Frog frog = (Frog) contact.entity;
                String variant = frog.getVariant().getRegisteredName();
                variant = variant.substring(variant.indexOf(':') + 1);
                resourceLocationPrimary = ResourceLocation.parse("textures/entity/frog/" + variant + "_frog.png");
            }
            case GHAST -> {
                Ghast ghast = (Ghast) contact.entity;
                if (ghast.isCharging()) resourceLocationPrimary = ResourceLocation.parse("textures/entity/ghast/ghast_shooting.png");
                else resourceLocationPrimary = ResourceLocation.parse("textures/entity/ghast/ghast.png");
            }
            case HORSE -> {
                if (contact.entity instanceof Horse horse) {
                    resourceLocationPrimary = switch(horse.getVariant()) {
                        case BLACK -> ResourceLocation.parse("textures/entity/horse/horse_black.png");
                        case BROWN -> ResourceLocation.parse("textures/entity/horse/horse_brown.png");
                        case CHESTNUT -> ResourceLocation.parse("textures/entity/horse/horse_chestnut.png");
                        case CREAMY -> ResourceLocation.parse("textures/entity/horse/horse_creamy.png");
                        case DARK_BROWN -> ResourceLocation.parse("textures/entity/horse/horse_darkbrown.png");
                        case GRAY -> ResourceLocation.parse("textures/entity/horse/horse_gray.png");
                        case WHITE -> ResourceLocation.parse("textures/entity/horse/horse_white.png");
                    };
                    resourceLocationSecondary = switch(horse.getMarkings()) {
                        case NONE -> null;
                        case BLACK_DOTS -> ResourceLocation.parse("textures/entity/horse/horse_markings_blackdots.png");
                        case WHITE -> ResourceLocation.parse("textures/entity/horse/horse_markings_white.png");
                        case WHITE_DOTS -> ResourceLocation.parse("textures/entity/horse/horse_markings_whitedots.png");
                        case WHITE_FIELD -> ResourceLocation.parse("textures/entity/horse/horse_markings_whitefield.png");
                    };
                    if (this.options.showHelmetsMobs) {
                        ItemStack itemStack = horse.getBodyArmorItem();
                        String armorName = itemStack.getItem().builtInRegistryHolder().getRegisteredName();
                        resourceLocationTertiary = switch (armorName) {
                            case "minecraft:leather_horse_armor" -> ResourceLocation.parse("textures/entity/equipment/horse_body/leather.png");
                            case "minecraft:iron_horse_armor" -> ResourceLocation.parse("textures/entity/equipment/horse_body/iron.png");
                            case "minecraft:golden_horse_armor" -> ResourceLocation.parse("textures/entity/equipment/horse_body/gold.png");
                            case "minecraft:diamond_horse_armor" -> ResourceLocation.parse("textures/entity/equipment/horse_body/diamond.png");
                            default -> null;
                        };
                        contact.setArmorColor(DyedItemColor.getOrDefault(itemStack, -1));
                    }
                }
            }
            case MOOSHROOM -> {
                MushroomCow mushroomCow = (MushroomCow)contact.entity;
                resourceLocationPrimary = switch (mushroomCow.getVariant()) {
                    case RED -> ResourceLocation.parse("textures/entity/cow/red_mooshroom.png");
                    case BROWN -> ResourceLocation.parse("textures/entity/cow/brown_mooshroom.png");
                };
            }
            case PARROT -> {
                Parrot parrot = (Parrot)contact.entity;
                resourceLocationPrimary = switch (parrot.getVariant()) {
                    case BLUE -> ResourceLocation.parse("textures/entity/parrot/parrot_blue.png");
                    case GRAY -> ResourceLocation.parse("textures/entity/parrot/parrot_grey.png");
                    case GREEN -> ResourceLocation.parse("textures/entity/parrot/parrot_green.png");
                    case RED_BLUE -> ResourceLocation.parse("textures/entity/parrot/parrot_red_blue.png");
                    case YELLOW_BLUE -> ResourceLocation.parse("textures/entity/parrot/parrot_yellow_blue.png");
                };
            }
            case PUFFERFISH -> {
                Pufferfish pufferfish = (Pufferfish) contact.entity;
                variantString = Integer.toString(pufferfish.getPuffState());
            }
            case RABBIT -> {
                if (contact.entity.hasCustomName() && contact.entity.getCustomName().getString().equals("Toast")) {
                    resourceLocationPrimary = ResourceLocation.parse("textures/entity/rabbit/toast.png");
                } else {
                    Rabbit rabbit = (Rabbit) contact.entity;
                    resourceLocationPrimary = switch (rabbit.getVariant()) {
                        case BLACK -> ResourceLocation.parse("textures/entity/rabbit/black.png");
                        case BROWN -> ResourceLocation.parse("textures/entity/rabbit/brown.png");
                        case EVIL -> ResourceLocation.parse("textures/entity/rabbit/caerbannog.png");
                        case GOLD -> ResourceLocation.parse("textures/entity/rabbit/gold.png");
                        case SALT -> ResourceLocation.parse("textures/entity/rabbit/salt.png");
                        case WHITE -> ResourceLocation.parse("textures/entity/rabbit/white.png");
                        case WHITE_SPLOTCHED -> ResourceLocation.parse("textures/entity/rabbit/white_splotched.png");
                    };
                }
            }
            case SALMON -> {
                Salmon salmon = (Salmon) contact.entity;
                variantString = Float.toString(salmon.getSalmonScale());
            }
            case STRIDER -> {
                Strider strider = (Strider) contact.entity;
                if (strider.isSuffocating()) resourceLocationPrimary = ResourceLocation.parse("textures/entity/strider/strider_cold.png");
                else resourceLocationPrimary = ResourceLocation.parse("textures/entity/strider/strider.png");
            }
            case TROPICAL_FISH_A, TROPICAL_FISH_B -> {
                TropicalFish fish = (TropicalFish) contact.entity;
                resourceLocationSecondary = switch (fish.getVariant()) {
                    case KOB -> ResourceLocation.parse("textures/entity/fish/tropical_a_pattern_1.png");
                    case SUNSTREAK -> ResourceLocation.parse("textures/entity/fish/tropical_a_pattern_2.png");
                    case SNOOPER -> ResourceLocation.parse("textures/entity/fish/tropical_a_pattern_3.png");
                    case DASHER -> ResourceLocation.parse("textures/entity/fish/tropical_a_pattern_4.png");
                    case BRINELY -> ResourceLocation.parse("textures/entity/fish/tropical_a_pattern_5.png");
                    case SPOTTY -> ResourceLocation.parse("textures/entity/fish/tropical_a_pattern_6.png");
                    case FLOPPER -> ResourceLocation.parse("textures/entity/fish/tropical_b_pattern_1.png");
                    case STRIPEY -> ResourceLocation.parse("textures/entity/fish/tropical_b_pattern_2.png");
                    case GLITTER -> ResourceLocation.parse("textures/entity/fish/tropical_b_pattern_3.png");
                    case BLOCKFISH -> ResourceLocation.parse("textures/entity/fish/tropical_b_pattern_4.png");
                    case BETTY -> ResourceLocation.parse("textures/entity/fish/tropical_b_pattern_5.png");
                    case CLAYFISH -> ResourceLocation.parse("textures/entity/fish/tropical_b_pattern_6.png");
                };
                variantString = fish.getVariant().name() + fish.getBaseColor().getTextureDiffuseColor() + fish.getPatternColor().getTextureDiffuseColor();
            }
            case VEX -> {
                Vex vex = (Vex) contact.entity;
                if (vex.isCharging()) resourceLocationPrimary = ResourceLocation.parse("textures/entity/illager/vex_charging.png");
                else resourceLocationPrimary = ResourceLocation.parse("textures/entity/illager/vex.png");
            }
            case VILLAGER, ZOMBIE_VILLAGER -> {
                VillagerData villagerData = ((VillagerDataHolder) contact.entity).getVillagerData();
                VillagerProfession villagerProfession = villagerData.getProfession();
                String iconLocation = contact.type == EnumMobs.ZOMBIE_VILLAGER ? "textures/entity/zombie_villager" : "textures/entity/villager";

                resourceLocationSecondary = BuiltInRegistries.VILLAGER_TYPE.getKey(villagerData.getType());
                resourceLocationSecondary = ResourceLocation.fromNamespaceAndPath(resourceLocationSecondary.getNamespace(), iconLocation + "/type/" + resourceLocationSecondary.getPath() + ".png");

                if (villagerProfession != VillagerProfession.NONE && !contact.entity.isBaby()) {
                    resourceLocationTertiary = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villagerProfession);
                    resourceLocationTertiary = ResourceLocation.fromNamespaceAndPath(resourceLocationTertiary.getNamespace(), iconLocation + "/profession/" + resourceLocationTertiary.getPath() + ".png");

                    if (villagerProfession != VillagerProfession.NITWIT) {
                        resourceLocationQuaternary = villagerLevelID.get(Mth.clamp(villagerData.getLevel(), 1, villagerLevelID.size()));
                        resourceLocationQuaternary = ResourceLocation.fromNamespaceAndPath(resourceLocationQuaternary.getNamespace(), iconLocation + "/profession_level/" + resourceLocationQuaternary.getPath() + ".png");
                    }
                }

                VillagerMetadataSection.Hat biomeHatType = this.getHatType(resourceLocationSecondary);
                VillagerMetadataSection.Hat professionHatType = this.getHatType(resourceLocationTertiary);
                boolean showBiomeHat = professionHatType == VillagerMetadataSection.Hat.NONE || professionHatType == VillagerMetadataSection.Hat.PARTIAL && biomeHatType != VillagerMetadataSection.Hat.FULL;
                if (!showBiomeHat) {
                    resourceLocationSecondary = null;
                }
            }
            case WITHER -> {
                WitherBoss witherBoss = (WitherBoss) contact.entity;
                if (witherBoss.isInvulnerable()) resourceLocationPrimary = ResourceLocation.parse("textures/entity/wither/wither_invulnerable.png");
                else resourceLocationPrimary = ResourceLocation.parse("textures/entity/wither/wither.png");
            }
            case WOLF -> {
                Wolf wolf = (Wolf) contact.entity;
                String variant = wolf.getVariant().getRegisteredName();
                variant = variant.substring(variant.indexOf(':') + 1);
                String resLocationName = "textures/entity/wolf/wolf";
                if (!variant.equals("pale")) resLocationName += "_" + variant;
                if (wolf.isAngry()) resLocationName += "_angry";
                else if (wolf.isTame()) resLocationName += "_tame";
                resLocationName += ".png";
                resourceLocationPrimary = ResourceLocation.parse(resLocationName);
                if (this.options.showHelmetsMobs) {
                    String armorName = wolf.getBodyArmorItem().getItem().builtInRegistryHolder().getRegisteredName();
                    if (armorName.equals("minecraft:wolf_armor")) resourceLocationSecondary = ResourceLocation.parse("textures/entity/equipment/wolf_body/armadillo_scute.png");
                }
            }
            default -> resourceLocationSecondary = contact.type.secondaryResourceLocation;
        }


        String entityName = contact.vanillaType ? "minecraft." + contact.type.id : contact.entity.getClass().getName();
        String resourceLocationString = resourceLocationPrimary.toString();
        resourceLocationString += resourceLocationSecondary == null ? "" : resourceLocationSecondary.toString();
        resourceLocationString += resourceLocationTertiary == null ? "" : resourceLocationTertiary.toString();
        resourceLocationString += resourceLocationQuaternary == null ? "" : resourceLocationQuaternary.toString();
        resourceLocationString += (contact.armorColor != -1 ? contact.armorColor : "");
        String name = entityName + variantString + resourceLocationString;
        Sprite icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(name);

        if (icon == this.textureAtlas.getMissingImage()) {
            if (VoxelConstants.DEBUG) {
                VoxelConstants.getLogger().info("Radar: Creating icon: " + name);
            }
            Integer checkCount = this.contactsSkinGetTries.get(name);
            if (checkCount == null) {
                checkCount = 0;
            }

            BufferedImage mobImage = null;
            if (contact.type == EnumMobs.HORSE) {
                BufferedImage base = ImageUtils.createBufferedImageFromResourceLocation(resourceLocationPrimary);
                if (resourceLocationSecondary != null && base != null) {
                    BufferedImage variant = ImageUtils.createBufferedImageFromResourceLocation(resourceLocationSecondary);
                    variant = ImageUtils.scaleImage(variant, (float) base.getWidth() / variant.getWidth(), (float) base.getHeight() / variant.getHeight());
                    base = ImageUtils.addImages(base, variant, 0.0F, 0.0F, base.getWidth(), base.getHeight());
                    variant.flush();
                }

                if (resourceLocationTertiary != null && base != null) {
                    BufferedImage pattern = ImageUtils.createBufferedImageFromResourceLocation(resourceLocationTertiary);
                    pattern = ImageUtils.scaleImage(pattern, (float) base.getWidth() / pattern.getWidth(), (float) base.getHeight() / pattern.getHeight());
                    pattern = ImageUtils.colorify(pattern, contact.armorColor);
                    base = ImageUtils.addImages(base, pattern, 0.0F, 0.0F, base.getWidth(), base.getHeight());
                    pattern.flush();
                }

                if (resourceLocationQuaternary != null && base != null) {
                    BufferedImage armor = ImageUtils.createBufferedImageFromResourceLocation(resourceLocationQuaternary);
                    armor = ImageUtils.scaleImage(armor, (float) base.getWidth() / armor.getWidth(), (float) base.getHeight() / armor.getHeight());
                    armor = ImageUtils.colorify(armor, contact.armorColor);
                    base = ImageUtils.addImages(base, armor, 0.0F, 0.0F, base.getWidth(), base.getHeight());
                    armor.flush();
                }

                mobImage = this.createPreMappedIconFromImage(contact.type, base);
                base.flush();
            } else if (contact.type.expectedWidth > 0.5) {
                mobImage = this.createPreMappedIcon(contact.type, resourceLocationPrimary);
            }

            if (mobImage != null) {
                mobImage = this.trimAndOutlineImage(contact, mobImage, false, true);
            } else {
                mobImage = this.createAutoIconImageFromResourceLocations(contact, name, render, resourceLocationPrimary, resourceLocationSecondary, resourceLocationTertiary, resourceLocationQuaternary);
            }

            if (mobImage != null) {
                try {
                    icon = this.textureAtlas.registerIconForBufferedImage(name, mobImage);
                    contact.icon = icon;
                    this.newMobs = true;
                    this.contactsSkinGetTries.remove(name);
                } catch (Exception var16) {
                    checkCount = checkCount + 1;
                    if (checkCount > 4) {
                        this.textureAtlas.registerFailedIcon(name);
                        this.contactsSkinGetTries.remove(name);
                    } else {
                        this.contactsSkinGetTries.put(name, checkCount);
                    }
                }
            } else {
                checkCount = checkCount + 1;
                if (checkCount > 4) {
                    this.textureAtlas.registerFailedIcon(name);
                    this.contactsSkinGetTries.remove(name);
                } else {
                    this.contactsSkinGetTries.put(name, checkCount);
                }
            }
        } else if (icon != this.textureAtlas.getFailedImage()) {
            contact.icon = icon;
        }

    }

    public VillagerMetadataSection.Hat getHatType(ResourceLocation resourceLocation) {
        VillagerMetadataSection.Hat hatType = VillagerMetadataSection.Hat.NONE;
        if (resourceLocation != null) {
            try {
                Optional<Resource> resource = VoxelConstants.getMinecraft().getResourceManager().getResource(resourceLocation);
                if (resource.isPresent()) {
                    VillagerMetadataSection villagerResourceMetadata = resource.get().metadata().getSection(VillagerMetadataSection.TYPE).orElse(null);
                    if (villagerResourceMetadata != null) {
                        hatType = villagerResourceMetadata.hat();
                    }

                    resource.get().openAsReader().close();
                }
            } catch (IOException | ClassCastException ignored) {
                hatType = VillagerMetadataSection.Hat.NONE;
            }
        }

        return hatType;
    }

    private BufferedImage createAutoIconImageFromResourceLocations(Contact contact, String entityID, EntityRenderer<LivingEntity, LivingEntityRenderState> entityRenderer, ResourceLocation... resourceLocations) {
        Entity entity = contact.entity;
        EnumMobs type = contact.type;
        if (type != EnumMobs.UNKNOWN && entityIconMap.containsKey(entityID)) {
            return entityIconMap.get(entityID);
        }

        if (type == EnumMobs.UNKNOWN) {
            VoxelConstants.getLogger().info("Unknown Entity: " + entity.getType());
        }

        BufferedImage headImage = null;
        Model model = null;
        if (entityRenderer instanceof LivingEntityRenderer<?, ?, ?> render) {
            try {
                model = render.getModel();
                ArrayList<Field> submodels = ReflectionUtils.getFieldsByType(model, Model.class, ModelPart.class);
                ArrayList<Field> submodelArrays = ReflectionUtils.getFieldsByType(model, Model.class, ModelPart[].class);
                ModelPart[] headBits = null;
                ArrayList<ModelPartWithResourceLocation> headPartsWithResourceLocationList = new ArrayList<>();
                Properties properties = new Properties();
                String fullName = contact.vanillaType ? "minecraft." + type.id : entity.getClass().getName();
                String simpleName = contact.vanillaType ? type.id : entity.getClass().getSimpleName();
                String fullPath = ("textures/icons/" + fullName + ".properties").toLowerCase();

                ResourceManager resourceManager = VoxelConstants.getMinecraft().getResourceManager();
                Optional<Resource> resource = resourceManager.getResource(ResourceLocation.parse(fullPath));

                if (resource.isEmpty()) {
                    fullPath = ("textures/icons/" + simpleName + ".properties").toLowerCase();
                    resource = resourceManager.getResource(ResourceLocation.parse(fullPath));
                }
                if (resource.isPresent()) {
                    try (InputStream is = resource.get().open()) {
                        properties.load(is);
                        is.close();
                        String subModelNames = properties.getProperty("models", "").toLowerCase();
                        String[] submodelNamesArray = subModelNames.split(",");
                        List<String> subModelNamesList = Arrays.asList(submodelNamesArray);
                        HashSet<String> subModelNamesSet = new HashSet<>(subModelNamesList);
                        ArrayList<ModelPart> headPartsArrayList = new ArrayList<>();

                        for (Field submodelArray : submodelArrays) {
                            String name = submodelArray.getName().toLowerCase();
                            if (subModelNamesSet.contains(name) || subModelNames.equals("all")) {
                                ModelPart[] submodelArrayValue = (ModelPart[]) submodelArray.get(model);
                                if (submodelArrayValue != null) {
                                    Collections.addAll(headPartsArrayList, submodelArrayValue);
                                }
                            }
                        }

                        for (Field submodel : submodels) {
                            String name = submodel.getName().toLowerCase();
                            if ((subModelNamesSet.contains(name) || subModelNames.equals("all")) && submodel.get(model) != null) {
                                Object modelPartObjekt = submodel.get(model);
                                if (modelPartObjekt instanceof ModelPart modelPart) {
                                    headPartsArrayList.add(modelPart);
                                }
                            }
                        }

                        if (!headPartsArrayList.isEmpty()) {
                            headBits = headPartsArrayList.toArray(new ModelPart[0]);
                        }
                    }
                }

                if (headBits == null) {
                    switch (model) {
                        case AxolotlModel axolotlModel -> headBits = new ModelPart[]{axolotlModel.head};
                        case BeeModel beeModel -> headBits = new ModelPart[]{beeModel.bone.getChild("body")};
                        case ChickenModel chickenModel -> headBits = new ModelPart[]{chickenModel.head};
                        case CodModel codModel -> headBits = new ModelPart[]{codModel.root()};
                        case CreakingModel creakingModel -> headBits = new ModelPart[]{creakingModel.root().getChild("root").getChild("upper_body").getChild("head")};
                        case DolphinModel dolphinModel -> headBits = new ModelPart[]{dolphinModel.root()};
                        case DonkeyModel donkeyModel -> headBits = new ModelPart[]{donkeyModel.headParts};
                        case DrownedModel drownedModel -> headBits = new ModelPart[]{drownedModel.head, drownedModel.hat};
                        case EndermanModel<?> endermanModel -> headBits = new ModelPart[]{endermanModel.head};
                        case EndermiteModel endermiteModel -> headBits = new ModelPart[]{endermiteModel.root().getChild("segment0"), endermiteModel.root().getChild("segment1")};
                        case HoglinModel hoglinModel -> headBits = new ModelPart[]{hoglinModel.head};
                        case HorseModel horseModel -> headBits = new ModelPart[]{horseModel.headParts};
                        case LavaSlimeModel lavaSlimeModel -> headBits = lavaSlimeModel.bodyCubes;
                        case OcelotModel ocelotModel -> headBits = new ModelPart[]{ocelotModel.head};
                        case RavagerModel ravagerModel -> headBits = new ModelPart[]{ravagerModel.root().getChild("neck").getChild("head")};
                        case SalmonModel salmonModel -> headBits = new ModelPart[]{salmonModel.root()};
                        case ShulkerModel shulkerModel -> headBits = new ModelPart[]{shulkerModel.head};
                        case SilverfishModel silverfishModel -> headBits = new ModelPart[]{silverfishModel.root().getChild("segment0"), silverfishModel.root().getChild("segment1")};
                        case SlimeModel slimeModel -> headBits = new ModelPart[]{slimeModel.root()};
                        case SnifferModel snifferModel -> headBits = new ModelPart[]{snifferModel.root().getChild("bone").getChild("body").getChild("head")};
                        case SnowGolemModel snowGolemModel -> headBits = new ModelPart[]{snowGolemModel.getHead()};
                        case TropicalFishModelA tropicalFishModelA -> headBits = new ModelPart[]{tropicalFishModelA.root()};
                        case TropicalFishModelB tropicalFishModelB -> headBits = new ModelPart[]{tropicalFishModelB.root()};
                        case VillagerModel villagerModel -> headBits = new ModelPart[]{villagerModel.getHead()};
                        case WardenModel wardenModel -> headBits = new ModelPart[]{wardenModel.root().getChild("bone").getChild("body").getChild("head")};
                        case WitherBossModel witherBossModel -> headBits = new ModelPart[]{witherBossModel.root().getChild("center_head"), witherBossModel.root().getChild("left_head"), witherBossModel.root().getChild("right_head")};
                        case WolfModel wolfModel -> headBits = new ModelPart[]{wolfModel.head};

                        case PlayerModel playerModel -> {
                            boolean showHat = true;
                            if (entity instanceof Player player) {
                                showHat = player.isModelPartShown(PlayerModelPart.HAT);
                            }
                            if (showHat) {
                                headBits = new ModelPart[]{((PlayerModel) model).head, ((PlayerModel) model).hat};
                            } else {
                                headBits = new ModelPart[]{((PlayerModel) model).head};
                            }
                        }
                        case HumanoidModel<?> humanoidModel -> headBits = new ModelPart[]{humanoidModel.head, humanoidModel.hat};
                        case QuadrupedModel<?> quadrupedModel -> headBits = new ModelPart[]{quadrupedModel.head};
                        case EntityModel<?> entityModel -> {
                            try {
                                headBits = new ModelPart[]{entityModel.root().getChild("head")};
                            } catch (Exception ignored1) {
                                try {
                                    headBits = new ModelPart[]{entityModel.root().getChild("body").getChild("head")};
                                } catch (Exception ignored2) {
                                    try {
                                        headBits = new ModelPart[]{entityModel.root().getChild("body")};
                                    } catch (Exception ignored3) {
                                    }
                                }
                            }
                        }
                        default -> {
                        }
                    }
                }

                if (headBits != null && headBits.length > 0 && resourceLocations[0] != null) {

                    float scale = Float.parseFloat(properties.getProperty("scale", "1"));

                    ResourceLocation resourceLocation = this.combineResourceLocations(resourceLocations);
                    for (ModelPart headBit : headBits) {
                        headPartsWithResourceLocationList.add(new ModelPartWithResourceLocation(headBit, resourceLocation));
                    }

                    ModelPartWithResourceLocation[] headBitsWithLocations = headPartsWithResourceLocationList.toArray(new ModelPartWithResourceLocation[0]);
                    boolean success = this.drawModel(scale, 1000, (LivingEntity) entity, model, headBitsWithLocations);

                    if (success) { headImage = ImageUtils.createBufferedImageFromGLID(OpenGL.Utils.fboTextureId);}
                    if (VoxelConstants.DEBUG) { ImageUtils.saveImage(type.id, OpenGL.Utils.fboTextureId, 0, 512, 512); }
                }
            } catch (Exception exception) {
                VoxelConstants.getLogger().error(exception);
            }
        }
        if (headImage != null) { headImage = this.trimAndOutlineImage(contact, headImage, true, model instanceof HumanoidModel); }

        entityIconMap.put(entityID, headImage);
        return headImage;
    }

    private ResourceLocation combineResourceLocations(ResourceLocation... resourceLocations) {
        ResourceLocation resourceLocation = resourceLocations[0];
        if (resourceLocations.length > 1) {
            boolean hasAdditional = false;

            try {
                BufferedImage base = null;

                for (int t = 1; t < resourceLocations.length; ++t) {
                    if (resourceLocations[t] != null) {
                        if (!hasAdditional) {
                            base = ImageUtils.createBufferedImageFromResourceLocation(resourceLocation);
                        }

                        hasAdditional = true;
                        BufferedImage overlay = ImageUtils.createBufferedImageFromResourceLocation(resourceLocations[t]);
                        float xScale = ((float) base.getWidth() / overlay.getWidth());
                        float yScale = ((float) base.getHeight() / overlay.getHeight());
                        if (xScale != 1.0F || yScale != 1.0F) {
                            overlay = ImageUtils.scaleImage(overlay, xScale, yScale);
                        }

                        ImageUtils.addImages(base, overlay, 0.0F, 0.0F, base.getWidth(), base.getHeight());
                        overlay.flush();
                    }
                }

                if (hasAdditional) {
                    NativeImage nativeImage = OpenGL.Utils.nativeImageFromBufferedImage(base);
                    base.flush();
                    this.nativeBackedTexture.close();
                    this.nativeBackedTexture = new DynamicTexture(nativeImage);
                    OpenGL.Utils.register(this.nativeBackedTextureLocation, this.nativeBackedTexture);
                    resourceLocation = this.nativeBackedTextureLocation;
                }
            } catch (Exception var9) {
                VoxelConstants.getLogger().warn(var9);
            }
        }

        return resourceLocation;
    }

    private boolean drawModel(float scale, int captureDepth, LivingEntity livingEntity, Model model, ModelPartWithResourceLocation[] headBits) {
        boolean failed = false;
        float size = 64.0F * scale;
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
        matrixStack.translate(0.0f, 0.0f, -3000.0f + captureDepth);
        OpenGL.Utils.bindFramebuffer();
        OpenGL.glDepthMask(true);
        OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        OpenGL.glDisable(OpenGL.GL11_GL_CULL_FACE);
        OpenGL.glClearColor(1.0F, 1.0F, 1.0F, 0.0F);
        OpenGL.glClearDepth(1.0);
        OpenGL.glClear(OpenGL.GL11_GL_COLOR_BUFFER_BIT | OpenGL.GL11_GL_DEPTH_BUFFER_BIT);
        OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
        matrixStack.pushMatrix();
        matrixStack.translate(width / 2f, height / 2f, 0.0f);
        matrixStack.scale(size, size, size);
        matrixStack.rotate(Axis.ZP.rotationDegrees(180.0F));
        matrixStack.rotate(Axis.YP.rotationDegrees(180.0F));

        Vector4f fullbright2 = new Vector4f(1f, 1f, 1f, 0).mul(matrixStack);
        Vector3f fullbright3 = new Vector3f(fullbright2.x, fullbright2.y, fullbright2.z);
        RenderSystem.setShaderLights(fullbright3, fullbright3);

        try {

            PoseStack newMatrixStack = new PoseStack();
            MultiBufferSource.BufferSource immediate = VoxelConstants.getMinecraft().renderBuffers().bufferSource();

            float offsetByY = 0.0f;
            if (model instanceof EndermanModel<?>) {
                offsetByY = 16.0f;
            } else if ((model instanceof HumanoidModel) || (model instanceof SkullModel)) {
                offsetByY = 4.0f;
            }

            for (ModelPartWithResourceLocation headBit : headBits) {
                VertexConsumer vertexConsumer = immediate.getBuffer(model.renderType(headBit.resourceLocation));
                model.resetPose();

                float yPos = headBit.modelPart.y;
                float xRot = headBit.modelPart.xRot;
                float yRot = headBit.modelPart.yRot;
                float zRot = headBit.modelPart.zRot;

                headBit.modelPart.y += offsetByY;
                headBit.modelPart.xRot = 0.0f;
                headBit.modelPart.yRot = 0.0f;
                headBit.modelPart.zRot = 0.0f;

                switch (model) {
                    case CodModel ignored -> headBit.modelPart.yRot = (float)Math.toRadians(90.0);
                    case DolphinModel ignored -> headBit.modelPart.yRot = (float)Math.toRadians(90.0);
                    case DonkeyModel ignored -> headBit.modelPart.yRot = (float)Math.toRadians(90.0);
                    case GoatModel ignored -> headBit.modelPart.xRot = (float)Math.toRadians(25.0);
                    case HoglinModel ignored -> headBit.modelPart.xRot = (float)Math.toRadians(75.0);
                    case HorseModel ignored -> headBit.modelPart.yRot = (float)Math.toRadians(90.0);
                    case ParrotModel ignored -> headBit.modelPart.yRot = (float)Math.toRadians(90.0);
                    case SalmonModel ignored -> headBit.modelPart.yRot = (float)Math.toRadians(90.0);
                    case TropicalFishModelA ignored -> headBit.modelPart.yRot = (float)Math.toRadians(90.0);
                    case TropicalFishModelB ignored -> headBit.modelPart.yRot = (float)Math.toRadians(90.0);
                    default -> {}
                }

                headBit.modelPart.render(newMatrixStack, vertexConsumer, 0xF000F0, OverlayTexture.NO_OVERLAY);
                headBit.modelPart.y = yPos;
                headBit.modelPart.xRot = xRot;
                headBit.modelPart.yRot = yRot;
                headBit.modelPart.zRot = zRot;

                immediate.endBatch();
            }
        } catch (Exception var25) {
            VoxelConstants.getLogger().warn("Error attempting to render head bits for " + livingEntity.getClass().getSimpleName(), var25);
            failed = true;
        }

        matrixStack.popMatrix();
        matrixStack.popMatrix();
        OpenGL.glEnable(OpenGL.GL11_GL_CULL_FACE);
        OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
        OpenGL.glDepthMask(false);
        OpenGL.Utils.unbindFramebuffer();
        RenderSystem.setProjectionMatrix(minimapProjectionMatrix, ProjectionType.ORTHOGRAPHIC);
        OpenGL.glViewport(0, 0, VoxelConstants.getMinecraft().getWindow().getWidth(), VoxelConstants.getMinecraft().getWindow().getHeight());
        return !failed;
    }

    private void getGenericIcon(Contact contact) {
        contact.type = this.getUnknownMobNeutrality(contact.entity);
        String name = "minecraft." + contact.type.id + contact.type.resourceLocation.toString();
        contact.icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(name);
    }

    private BufferedImage trimAndOutlineImage(Contact contact, BufferedImage image, boolean auto, boolean centered) {
        if (auto) {
            image = centered ? ImageUtils.trimCentered(image) : ImageUtils.trim(image);
            double acceptableMax = 64.0;
            if (ImageUtils.percentageOfEdgePixelsThatAreSolid(image) < 30.0F) {
                acceptableMax = 128.0;
            }

            int maxDimension = Math.max(image.getWidth(), image.getHeight());
            float scale = (float) Math.ceil(maxDimension / acceptableMax);
            return ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(image, 1.0F / scale)), this.options.outlines, 2);
        } else {
            float scale = image.getWidth() / contact.type.expectedWidth;
            return ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(image, 4.0F / scale)), this.options.outlines, 2);
        }
    }

    private void handleMPplayer(Contact contact) {
        AbstractClientPlayer player = (AbstractClientPlayer) contact.entity;
        GameProfile gameProfile = player.getGameProfile();
        UUID uuid = gameProfile.getId();
        contact.setUUID(uuid);
        String playerName = this.scrubCodes(gameProfile.getName());
        Sprite icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(playerName);
        Integer checkCount;
        if (icon == this.textureAtlas.getMissingImage()) {
            checkCount = this.mpContactsSkinGetTries.get(playerName);
            if (checkCount == null) { checkCount = 0; }

            if (checkCount < 5) {
                AbstractTexture imageData; //TODO 1.21.4

                try {
                    ResourceLocation skinIdentifier = VoxelConstants.getMinecraft().getSkinManager().getInsecureSkin(player.getGameProfile()).texture();
                    if (skinIdentifier == DefaultPlayerSkin.get(player.getUUID()).texture()) {
                        throw new Exception("failed to get skin: skin is default");
                    }

                    imageData = VoxelConstants.getMinecraft().getTextureManager().getTexture(skinIdentifier);
                    if (imageData == null) {
                        throw new Exception("failed to get skin: image data was null");
                    }

                    EntityRenderer<LivingEntity, LivingEntityRenderState> render = (EntityRenderer<LivingEntity, LivingEntityRenderState>) VoxelConstants.getMinecraft().getEntityRenderDispatcher().getRenderer(contact.entity);
                    BufferedImage skinImage = this.createAutoIconImageFromResourceLocations(contact, contact.uuid.toString(), render, skinIdentifier, null);
                    icon = this.textureAtlas.registerIconForBufferedImage(playerName, skinImage);
                    this.newMobs = true;
                    this.mpContactsSkinGetTries.remove(playerName);
                } catch (Exception var11) {
                    icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("minecraft." + EnumMobs.PLAYER.id + EnumMobs.PLAYER.resourceLocation.toString());
                    checkCount = checkCount + 1;
                    this.mpContactsSkinGetTries.put(playerName, checkCount);
                }

                contact.icon = icon;
            }
        } else {
            contact.icon = icon;
        }

    }

    private void getArmor(Contact contact, Entity entity) {
        Sprite icon = null;
        Item helmet = null;
        ItemStack stack = ((LivingEntity) entity).getItemBySlot(EquipmentSlot.HEAD);
        if (stack.getCount() > 0) {
            helmet = stack.getItem();
        }

        if (contact.type == EnumMobs.SHEEP) {
            Sheep sheepEntity = (Sheep) contact.entity;
            if (!sheepEntity.isSheared()) {
                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(headIDs[6]);
                contact.setArmorColor(Sheep.getColor(sheepEntity.getColor()));
            }
        } else if (contact.type == EnumMobs.SLIME) {
            icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(headIDs[7]);
        } else if (helmet != null) {
            if (helmet == Items.SKELETON_SKULL) {
                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(headIDs[0]);
            } else if (helmet == Items.WITHER_SKELETON_SKULL) {
                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(headIDs[1]);
            } else if (helmet == Items.ZOMBIE_HEAD) {
                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(headIDs[2]);
            } else if (helmet == Items.CREEPER_HEAD) {
                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(headIDs[3]);
            } else if (helmet == Items.DRAGON_HEAD) {
                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(headIDs[4]);
            } else if (helmet == Items.PIGLIN_HEAD) {
                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched(headIDs[5]);
            } else if (helmet == Items.PLAYER_HEAD) {
                GameProfile gameProfile = null;
                ResolvableProfile profileComponent = stack.get(DataComponents.PROFILE);
                if (profileComponent != null && profileComponent.isResolved()) {
                    gameProfile = profileComponent.gameProfile();
                }

                ResourceLocation resourceLocation = DefaultPlayerSkin.getDefaultTexture();
                if (gameProfile != null) {
                    resourceLocation = VoxelConstants.getMinecraft().getSkinManager().getInsecureSkin(gameProfile).texture();
                }

                icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("minecraft." + EnumMobs.PLAYER.id + resourceLocation.toString() + "head");
                if (icon == this.textureAtlas.getMissingImage()) {
                    ModelPart outer = this.playerSkullModel.head;
                    ModelPartWithResourceLocation[] headBits = {new ModelPartWithResourceLocation(outer, resourceLocation)};
                    boolean success = this.drawModel(1.1875F, 1000, contact.entity, this.playerSkullModel, headBits);
                    if (success) {
                        BufferedImage headImage = ImageUtils.createBufferedImageFromGLID(OpenGL.Utils.fboTextureId);
                        headImage = this.trimAndOutlineImage(new Contact(VoxelConstants.getPlayer(), EnumMobs.PLAYER), headImage, true, true);
                        icon = this.textureAtlas.registerIconForBufferedImage("minecraft." + EnumMobs.PLAYER.id + resourceLocation + "head", headImage);
                        this.newMobs = true;
                    }
                }
            } else if (helmet instanceof ArmorItem helmetArmor) {
                if (this.isLeatherArmor(helmetArmor)) {
                    icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("armor " + this.armorNames[0]);
                } else {
                    boolean isPiglin = contact.type == EnumMobs.PIGLIN || contact.type == EnumMobs.ZOMBIFIED_PIGLIN;
                    icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("armor " + helmet.getDescriptionId() + (isPiglin ? "_piglin" : ""));
                    if (icon == this.textureAtlas.getMissingImage()) {
                        icon = this.createUnknownArmorIcons(contact, helmet);
                    } else if (icon == this.textureAtlas.getFailedImage()) {
                        icon = null;
                    }
                }
                contact.setArmorColor(DyedItemColor.getOrDefault(stack, -1));
            }

                // } else if (helmet instanceof BlockItem blockItem) {
                // Block block = blockItem.getBlock();
                // BlockState blockState = block.defaultBlockState();
                // int stateID = Block.getId(blockState);
                // icon = this.textureAtlas.getAtlasSprite("blockArmor " + stateID);
                // if (icon == this.textureAtlas.getMissingImage()) {
                // BufferedImage blockImage = VoxelConstants.getVoxelMapInstance().getColorManager().getBlockImage(blockState, stack, entity.level(), 4.9473686F, -8.0F);
                // if (blockImage != null) {
                // int width = blockImage.getWidth();
                // int height = blockImage.getHeight();
                // ImageUtils.eraseArea(blockImage, width / 2 - 15, height / 2 - 15, 30, 30, width, height);
                // BufferedImage blockImageFront = VoxelConstants.getVoxelMapInstance().getColorManager().getBlockImage(blockState, stack, entity.level(), 4.9473686F, 7.25F);
                // blockImageFront = blockImageFront.getSubimage(width / 2 - 15, height / 2 - 15, 30, 30);
                // ImageUtils.addImages(blockImage, blockImageFront, (width / 2f - 15), (height / 2f - 15), width, height);
                // blockImageFront.flush();
                // blockImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.trimCentered(blockImage)), this.options.outlines, true, 37.6F, 37.6F, 2);
                // icon = this.textureAtlas.registerIconForBufferedImage("blockArmor " + stateID, blockImage);
                // this.newMobs = true;
                // }
                // }
        }

        contact.armorIcon = icon;
    }

    private Sprite createUnknownArmorIcons(Contact contact, Item helmet) {
        Sprite icon = null;
        ResourceLocation resourceLocation = null;

        try {
            String armorKey = helmet.builtInRegistryHolder().getRegisteredName();
            String namespace = "minecraft";
            int separator = armorKey.indexOf(':');
            if (separator != -1) {
                armorKey = armorKey.substring(separator + 1);
                if (armorKey.startsWith("leather")) {
                    armorKey = "leather";
                } else if (armorKey.startsWith("iron")) {
                    armorKey = "iron";
                } else if (armorKey.startsWith("gold")) {
                    armorKey = "gold";
                } else if (armorKey.startsWith("diamond")) {
                    armorKey = "diamond";
                } else if (armorKey.startsWith("netherite")) {
                    armorKey = "netherite";
                } else if (armorKey.startsWith("chain")) {
                    armorKey = "chainmail";
                } else if (armorKey.startsWith("turtle")) {
                    armorKey = "turtle_scute";
                }
            }
            resourceLocation = ResourceLocation.parse(namespace + ":textures/entity/equipment/humanoid/" + armorKey + ".png");
        }
        catch (RuntimeException ignored) {
        }

        float intendedWidth = 9.0F;
        float intendedHeight = 9.0F;
        boolean isPiglin = contact.type == EnumMobs.PIGLIN || contact.type == EnumMobs.ZOMBIFIED_PIGLIN;
        HumanoidModel<HumanoidRenderState> armorModel;

        if (isPiglin) {
            armorModel = this.piglinArmorModel;
            intendedWidth = 11.5F;
        } else {
            armorModel = this.bipedArmorModel;
        }

        if (armorModel != null && resourceLocation != null) {
            ModelPartWithResourceLocation[] armorHeadBit = { new ModelPartWithResourceLocation(armorModel.head, resourceLocation), new ModelPartWithResourceLocation(armorModel.hat, resourceLocation) };

            this.drawModel(1.0F, 2, contact.entity, armorModel, armorHeadBit);
            BufferedImage armorImage = ImageUtils.createBufferedImageFromGLID(OpenGL.Utils.fboTextureId);
            armorImage = armorImage.getSubimage(200, 200, 112, 112);
            armorImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.trimCentered(armorImage)), this.options.outlines, true, intendedWidth * 4.0F, intendedHeight * 4.0F, 2);
            icon = this.textureAtlas.registerIconForBufferedImage("armor " + helmet.getDescriptionId() + (isPiglin ? "_piglin" : ""), armorImage);

            this.newMobs = true;
        }

        if (icon == null && resourceLocation != null) {
            BufferedImage armorTexture = ImageUtils.createBufferedImageFromResourceLocation(resourceLocation);
            if (armorTexture != null) {
                armorTexture = ImageUtils.addImages(ImageUtils.loadImage(armorTexture, 8, 8, 8, 8), ImageUtils.loadImage(armorTexture, 40, 8, 8, 8), 0.0F, 0.0F, 8, 8);
                float scale = armorTexture.getWidth() / 8.0F;
                if (isPiglin) {
                    BufferedImage armorImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(armorTexture, 4.0F / scale * 47.0F / 38.0F)), this.options.outlines, true, 47.0F, 37.6F, 2);
                    icon = this.textureAtlas.registerIconForBufferedImage("armor " + resourceLocation + "_piglin", armorImage);
                } else {
                    BufferedImage armorImage = ImageUtils.fillOutline(ImageUtils.pad(ImageUtils.scaleImage(armorTexture, 4.0F / scale * 47.0F / 38.0F)), this.options.outlines, true, 37.6F, 37.6F, 2);
                    icon = this.textureAtlas.registerIconForBufferedImage("armor " + resourceLocation, armorImage);
                }

                this.newMobs = true;
            }
        }

        if (icon == null) {
            VoxelConstants.getLogger().warn("can't get texture for custom armor type: " + helmet.getClass());
            this.textureAtlas.registerFailedIcon("armor " + helmet.getDescriptionId() + helmet.getClass().getName());
        }

        return icon;
    }

    private String scrubCodes(String string) {
        return string.replaceAll("(\\xA7.)", "");
    }

    private EnumMobs getUnknownMobNeutrality(Entity entity) {
        if (this.isHostile(entity)) {
            return EnumMobs.GENERIC_HOSTILE;
        } else {
            if (entity instanceof TamableAnimal tameableEntity) {
                if (tameableEntity.isTame() && (VoxelConstants.getMinecraft().hasSingleplayerServer() || tameableEntity.getOwner().equals(VoxelConstants.getPlayer()))) {
                    return EnumMobs.GENERIC_TAME;
                }
            }

            return EnumMobs.GENERIC_NEUTRAL;
        }
    }

    private boolean isLeatherArmor(ArmorItem helmet) {
        return helmet.getDescriptionId().equals("item.minecraft.leather_helmet");
    }

    public void renderMapMobs(GuiGraphics drawContext, Matrix4fStack matrixStack, int x, int y, float scaleProj) {
        double lastX = GameVariableAccessShim.xCoordDouble();
        double lastZ = GameVariableAccessShim.zCoordDouble();
        int lastY = GameVariableAccessShim.yCoord();
        double maxY = this.layoutVariables.zoomScaleAdjusted * 32.0;
        float iconScale = this.layoutVariables.fullscreenMap ? 0.5f : 1.0f;

        for (Contact contact : this.contacts) {
            RenderSystem.setShader(CoreShaders.POSITION_TEX);
            OpenGL.Utils.disp2(this.textureAtlas.getId());
            OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
            OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);

            contact.updateLocation();
            double wayX = lastX - contact.x;
            double wayZ = lastZ - contact.z;
            int wayY = lastY - contact.y;
            if (contact.type == EnumMobs.PHANTOM) {
                maxY *= 2;
            }
            double adjustedDiff = maxY - Math.max(Math.abs(wayY), 0);
            contact.brightness = (float) Math.max(adjustedDiff / maxY, 0.0);
            contact.brightness *= contact.brightness;
            contact.angle = (float) Math.toDegrees(Math.atan2(wayX, wayZ));
            contact.distance = Math.sqrt(wayX * wayX + wayZ * wayZ) / this.layoutVariables.zoomScaleAdjusted;

            if (wayY < 0) {
                OpenGL.glColor4f(1.0F, 1.0F, 1.0F, contact.brightness);
            } else {
                contact.brightness = Math.max(contact.brightness, 0.3f);
                OpenGL.glColor4f(contact.brightness, contact.brightness, contact.brightness, 1.0f);
            }
            if (this.layoutVariables.rotating) {
                contact.angle += this.direction;
            } else if (this.minimapOptions.oldNorth) {
                contact.angle -= 90.0F;
            }

            boolean inRange;
            if (this.minimapOptions.shape != 1 && !this.layoutVariables.fullscreenMap) {
                inRange = contact.distance < 31.0;
            } else {
                double radLocate = Math.toRadians(contact.angle);
                double squareRangeX = contact.distance * Math.cos(radLocate);
                double squareRangeY = contact.distance * Math.sin(radLocate);
                inRange = Math.abs(squareRangeX) <= 28.5 && Math.abs(squareRangeY) <= 28.5;
            }

            if (inRange) {
                try {
                    matrixStack.pushMatrix();
                    if (this.options.filtering) {
                        matrixStack.translate(x, y, 0.0f);
                        matrixStack.rotate(Axis.ZP.rotationDegrees(-contact.angle));
                        matrixStack.translate(0.0f, (float) -contact.distance, 0.0f);
                        matrixStack.rotate(Axis.ZP.rotationDegrees(contact.angle + contact.rotationFactor));
                        matrixStack.translate((-x), (-y), 0.0f);
                    } else {
                        wayX = Math.sin(Math.toRadians(contact.angle)) * contact.distance;
                        wayZ = Math.cos(Math.toRadians(contact.angle)) * contact.distance;
                        matrixStack.translate((float) Math.round(-wayX * this.layoutVariables.scScale) / this.layoutVariables.scScale, (float) Math.round(-wayZ * this.layoutVariables.scScale) / this.layoutVariables.scScale, 0.0f);
                    }

                    float yOffset = 0.0F;
                    if (contact.entity.getVehicle() != null && this.isEntityShown(contact.entity.getVehicle())) {
                        yOffset = -4.0F;
                    }

                    OpenGL.Utils.drawPre();
                    OpenGL.Utils.setMap(contact.icon, x, y + yOffset, ((int) (contact.icon.getIconWidth() / 4.0F * iconScale)));
                    OpenGL.Utils.drawPost();
                    if ((this.options.showHelmetsPlayers && contact.type == EnumMobs.PLAYER || this.options.showHelmetsMobs && contact.type != EnumMobs.PLAYER || contact.type == EnumMobs.SHEEP) && contact.armorIcon != null) {
                        Sprite icon = contact.armorIcon;
                        float armorOffset = 0.0F;
                        if (contact.type == EnumMobs.ZOMBIE_VILLAGER) {
                            armorOffset = -0.5F;
                        }

                        float armorScale = iconScale;
                        float red = 1.0F; float green = 1.0F; float blue = 1.0F;
                        if (contact.armorColor != -1) {
                            red = (contact.armorColor >> 16 & 0xFF) / 255.0F;
                            green = (contact.armorColor >> 8 & 0xFF) / 255.0F;
                            blue = (contact.armorColor & 0xFF) / 255.0F;
                            if (contact.type == EnumMobs.SHEEP) {
                                Sheep sheepEntity = (Sheep) contact.entity;
                                if (sheepEntity.hasCustomName() && "jeb_".equals(sheepEntity.getName().getString())) {
                                    int semiRandom = sheepEntity.tickCount / 25 + sheepEntity.getId();
                                    int numDyeColors = DyeColor.values().length;
                                    float lerpVal = ((sheepEntity.tickCount % 25) + VoxelConstants.getMinecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false)) / 25.0F;
                                    Color sheepColors1 = new Color(Sheep.getColor(DyeColor.byId(semiRandom % numDyeColors)));
                                    Color sheepColors2 = new Color(Sheep.getColor(DyeColor.byId((semiRandom + 1) % numDyeColors)));
                                    red = (sheepColors1.getRed() * (1.0F - lerpVal) + sheepColors2.getRed() * lerpVal) / 255.0F;
                                    green = (sheepColors1.getGreen() * (1.0F - lerpVal) + sheepColors2.getGreen() * lerpVal) / 255.0F;
                                    blue = (sheepColors1.getBlue() * (1.0F - lerpVal) + sheepColors2.getBlue() * lerpVal) / 255.0F;
                                }

                                armorScale = 1.04F * iconScale;
                            }

                            if (wayY < 0) {
                                OpenGL.glColor4f(red, green, blue, contact.brightness);
                            } else {
                                OpenGL.glColor3f(red * contact.brightness, green * contact.brightness, blue * contact.brightness);
                            }
                        }

                        OpenGL.Utils.drawPre();
                        OpenGL.Utils.setMap(icon, x, y + yOffset + armorOffset, ((int) (icon.getIconWidth() / 4.0F * armorScale)));
                        OpenGL.Utils.drawPost();
                        if (icon == this.leatherArmorIcon) {
                            if (wayY < 0) {
                                OpenGL.glColor4f(1.0F, 1.0F, 1.0F, contact.brightness);
                            } else {
                                OpenGL.glColor3f(contact.brightness, contact.brightness, contact.brightness);
                            }
                            icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("armor " + this.armorNames[2]);
                            OpenGL.Utils.drawPre();
                            OpenGL.Utils.setMap(icon, x, y + yOffset + armorOffset, icon.getIconWidth() / 4.0F * armorScale);
                            OpenGL.Utils.drawPost();

                            if (wayY < 0) {
                                OpenGL.glColor4f(red, green, blue, contact.brightness);
                            } else {
                                OpenGL.glColor3f(red * contact.brightness, green * contact.brightness, blue * contact.brightness);
                            }
                            icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("armor " + this.armorNames[1]);
                            OpenGL.Utils.drawPre();
                            OpenGL.Utils.setMap(icon, x, y + yOffset + armorOffset, icon.getIconWidth() / 4.0F * armorScale * 40.0F / 37.0F);
                            OpenGL.Utils.drawPost();

                            OpenGL.glColor3f(1.0F, 1.0F, 1.0F);
                            icon = this.textureAtlas.getAtlasSpriteIncludingYetToBeStitched("armor " + this.armorNames[3]);
                            OpenGL.Utils.drawPre();
                            OpenGL.Utils.setMap(icon, x, y + yOffset + armorOffset, icon.getIconWidth() / 4.0F * armorScale * 40.0F / 37.0F);
                            OpenGL.Utils.drawPost();
                        }
                    }

                    if (contact.name != null && ((this.options.showPlayerNames && this.isPlayer(contact.entity)) || (this.options.showMobNames && !this.isPlayer(contact.entity) && (!this.options.showNamesOnlyForTagged || contact.entity.hasCustomName())))) {
                        float fontSize = this.options.fontSize * iconScale;
                        float scaleFactor = 1f / fontSize;
                        String mobName = contact.entity.getDisplayName().getString();
                        int halfStringWidth = VoxelConstants.getMinecraft().font.width(mobName) / 2;
                        int textColor;
                        int textAlpha = (int) (contact.brightness * 255);
                        if (wayY < 0) {
                            textColor = (textAlpha << 24) | (255 << 16) | (255 << 8) | 255;
                        } else {
                            textColor = (255 << 24) | (textAlpha << 16) | (textAlpha << 8) | textAlpha;
                        }
                        PoseStack textMatrixStack = drawContext.pose();
                        textMatrixStack.pushPose();
                        textMatrixStack.setIdentity();
                        textMatrixStack.scale(scaleProj * fontSize, scaleProj * fontSize, 1.0f);
                        wayX = Math.sin(Math.toRadians(contact.angle)) * contact.distance;
                        wayZ = Math.cos(Math.toRadians(contact.angle)) * contact.distance;
                        textMatrixStack.translate(-wayX * scaleFactor, -wayZ * scaleFactor, 900.0f);
                        drawContext.drawString(VoxelConstants.getMinecraft().font, mobName, (int) (x * scaleFactor - halfStringWidth), (int) ((y + 2) * scaleFactor), textColor);
                        textMatrixStack.popPose();
                    }
                } catch (Exception e) {
                    VoxelConstants.getLogger().error("Error rendering mob icon! " + e.getLocalizedMessage() + " contact type " + contact.type, e);
                } finally {
                    matrixStack.popMatrix();
                }
            }
        }
    }

    private void applyFilteringParameters() {
        if (this.options.filtering) {
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_LINEAR);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_LINEAR);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_S, OpenGL.GL12_GL_CLAMP_TO_EDGE);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_WRAP_T, OpenGL.GL12_GL_CLAMP_TO_EDGE);
        } else {
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MIN_FILTER, OpenGL.GL11_GL_NEAREST);
            OpenGL.glTexParameteri(OpenGL.GL11_GL_TEXTURE_2D, OpenGL.GL11_GL_TEXTURE_MAG_FILTER, OpenGL.GL11_GL_NEAREST);
        }

    }

    private boolean isHostile(Entity entity) {
        if (entity instanceof ZombifiedPiglin zombifiedPiglinEntity) {
            return zombifiedPiglinEntity.getPersistentAngerTarget() != null && zombifiedPiglinEntity.getPersistentAngerTarget().equals(VoxelConstants.getPlayer().getUUID());
        } else if (entity instanceof Enemy) {
            return true;
        } else if (entity instanceof Bee beeEntity) {
            return beeEntity.isAngry();
        } else {
            if (entity instanceof PolarBear polarBearEntity) {
                for (PolarBear object : polarBearEntity.level().getEntitiesOfClass(PolarBear.class, polarBearEntity.getBoundingBox().inflate(8.0, 4.0, 8.0))) {
                    if (object.isBaby()) {
                        return true;
                    }
                }
            }

            if (entity instanceof Rabbit rabbitEntity) {
                return rabbitEntity.getVariant() == Rabbit.Variant.EVIL;
            } else if (entity instanceof Wolf wolfEntity) {
                return wolfEntity.isAngry();
            } else {
                return false;
            }
        }
    }

    private boolean isPlayer(Entity entity) {
        return entity instanceof RemotePlayer;
    }

    private boolean isNeutral(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        } else {
            return !(entity instanceof Player) && !this.isHostile(entity);
        }
    }

    private record ModelPartWithResourceLocation(ModelPart modelPart, ResourceLocation resourceLocation) {
    }
}
