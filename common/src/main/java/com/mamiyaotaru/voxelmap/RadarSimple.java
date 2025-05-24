package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.MapVariables;
import com.mamiyaotaru.voxelmap.util.MobCategory;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class RadarSimple implements IRadar {
    public final MapSettingsManager minimapOptions;
    public final RadarSettingsManager options;
    private final TextureAtlas textureAtlas;
    public static final ResourceLocation resourceTextureAtlasMarker = ResourceLocation.fromNamespaceAndPath("voxelmap", "atlas/radarsimple/marker");
    private boolean completedLoading;
    private int timer = 500;
    private float direction;
    private final ArrayList<Contact> contacts = new ArrayList<>(40);

    public RadarSimple() {
        this.minimapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
        this.textureAtlas = new TextureAtlas("pings", resourceTextureAtlasMarker);
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.loadTexturePackIcons();
    }

    private void loadTexturePackIcons() {
        this.completedLoading = false;

        try {
            this.textureAtlas.reset();
            NativeImage contact = TextureContents.load(Minecraft.getInstance().getResourceManager(), ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/contact.png")).image();
            contact = ImageUtils.fillOutline(contact, false, true, 32.0F, 32.0F, 0);
            this.textureAtlas.registerIconForBufferedImage("contact", contact);
            NativeImage facing = TextureContents.load(Minecraft.getInstance().getResourceManager(), ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/contact_facing.png")).image();
            facing = ImageUtils.fillOutline(facing, false, true, 32.0F, 32.0F, 0);
            this.textureAtlas.registerIconForBufferedImage("facing", facing);
            this.textureAtlas.stitch();
            this.completedLoading = true;
        } catch (Exception var4) {
            VoxelConstants.getLogger().error("Failed getting mobs " + var4.getLocalizedMessage(), var4);
        }

    }

    @Override
    public void onTickInGame(GuiGraphics guiGraphics, MapVariables mapVariables) {
        if (this.options.radarAllowed || this.options.radarMobsAllowed || this.options.radarPlayersAllowed) {
            if (this.options.isChanged()) {
                this.timer = 500;
            }

            this.direction = GameVariableAccessShim.rotationYaw() + 180.0F;

            while (this.direction >= 360.0F) {
                this.direction -= 360.0F;
            }

            while (this.direction < 0.0F) {
                this.direction += 360.0F;
            }

            if (this.completedLoading && this.timer > 95) {
                this.calculateMobs(mapVariables);
                this.timer = 0;
            }

            ++this.timer;
            if (this.completedLoading) {
                this.renderMapMobs(guiGraphics, mapVariables);
            }
        }
    }

    public void calculateMobs(MapVariables mapVariables) {
        this.contacts.clear();

        for (Entity entity : VoxelConstants.getClientWorld().entitiesForRendering()) {
            try {
                if (entity != null && !entity.isInvisibleTo(VoxelConstants.getPlayer()) && (this.options.showHostiles && (this.options.radarAllowed || this.options.radarMobsAllowed) && MobCategory.isHostile(entity)
                        || this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed) && MobCategory.isPlayer(entity) || this.options.showNeutrals && this.options.radarMobsAllowed && MobCategory.isNeutral(entity))) {
                    int wayX = GameVariableAccessShim.xCoord() - (int) entity.position().x();
                    int wayZ = GameVariableAccessShim.zCoord() - (int) entity.position().z();
                    int wayY = GameVariableAccessShim.yCoord() - (int) entity.position().y();

                    float range = mapVariables.mapSize / 2.0F - 3.5F;
                    range *= range;
                    double hypot = wayX * wayX + wayZ * wayZ;
                    hypot *= mapVariables.positionScale * mapVariables.positionScale;

                    boolean inRange = false;
                    if (Math.abs(wayY) <= mapVariables.zoomScale * 32.0) {
                        if (!mapVariables.squareMap) {
                            inRange = hypot <= range;
                        } else {
                            double radLocate = Math.atan2(wayX, wayZ);
                            double dispX = hypot * Math.cos(radLocate);
                            double dispY = hypot * Math.sin(radLocate);
                            inRange = Math.abs(dispX) <= range && Math.abs(dispY) <= range;
                        }
                    }

                    if (inRange) {
                        Contact contact = new Contact((LivingEntity) entity, MobCategory.forEntity(entity));
                        this.contacts.add(contact);
                    }
                }
            } catch (Exception var11) {
                VoxelConstants.getLogger().error(var11.getLocalizedMessage(), var11);
            }
        }

        this.contacts.sort(Comparator.comparingDouble(contact -> contact.y));
    }

    public void renderMapMobs(GuiGraphics guiGraphics, MapVariables mapVariables) {
        int mapX = mapVariables.mapX;
        int mapY = mapVariables.mapY;

        float range = mapVariables.mapSize / 2.0F - 3.5F;
        double max = mapVariables.zoomScale * 32.0;

        for (Contact contact : this.contacts) {
            contact.updateLocation();
            double contactX = contact.x;
            double contactZ = contact.z;
            double contactY = contact.y;
            double wayX = GameVariableAccessShim.xCoordDouble() - contactX;
            double wayZ = GameVariableAccessShim.zCoordDouble() - contactZ;
            double wayY = GameVariableAccessShim.yCoord() - contactY;
            double adjustedDiff = max - Math.abs(wayY);
            contact.brightness = (float) Math.max(adjustedDiff / max, 0.0);
            contact.brightness *= contact.brightness;
            contact.angle = (float) Math.toDegrees(Math.atan2(wayX, wayZ));
            contact.distance = Math.sqrt(wayX * wayX + wayZ * wayZ) * mapVariables.positionScale;

            int color = wayY < 0 ? ARGB.colorFromFloat(contact.brightness, 1, 1, 1) : ARGB.colorFromFloat(1, contact.brightness, contact.brightness, contact.brightness);

            if (mapVariables.rotates) {
                contact.angle += this.direction;
            } else if (this.minimapOptions.oldNorth) {
                contact.angle -= 90.0F;
            }

            boolean inRange;
            if (!mapVariables.squareMap) {
                inRange = contact.distance <= range;
            } else {
                double radLocate = Math.toRadians(contact.angle);
                double dispX = contact.distance * Math.cos(radLocate);
                double dispY = contact.distance * Math.sin(radLocate);
                inRange = Math.abs(dispX) <= range && Math.abs(dispY) <= range;
            }

            if (inRange) {
                try {
                    guiGraphics.pose().pushPose();
                    float contactFacing = contact.entity.getYHeadRot();
                    if (mapVariables.rotates) {
                        contactFacing -= this.direction;
                    } else if (this.minimapOptions.oldNorth) {
                        contactFacing += 90.0F;
                    }

                    guiGraphics.pose().translate(mapX, mapY, 0.0f);
                    guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-contact.angle));
                    guiGraphics.pose().translate(0.0f, (float) -contact.distance, 0.0f);
                    guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(contact.angle + contactFacing));
                    guiGraphics.pose().translate(-mapX, -mapY, 0.0f);

                    this.textureAtlas.getAtlasSprite("contact").blit(guiGraphics, GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH, mapX - 4, mapY - 4, 8, 8, color);
                    if (this.options.showFacing) {
                        this.textureAtlas.getAtlasSprite("facing").blit(guiGraphics, GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH, mapX - 4, mapY - 4, 8, 8, color);
                    }
                } catch (Exception e) {
                    VoxelConstants.getLogger().error("Error rendering mob icon! " + e.getLocalizedMessage() + " contact type " + BuiltInRegistries.ENTITY_TYPE.getKey(contact.entity.getType()));
                } finally {
                    guiGraphics.pose().popPose();
                }
            }
        }

    }
}
