package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.EnumMobs;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4fStack;

public class RadarSimple implements IRadar {
    private LayoutVariables layoutVariables;
    public final MapSettingsManager minimapOptions;
    public final RadarSettingsManager options;
    private final TextureAtlas textureAtlas;
    private boolean completedLoading;
    private int timer = 500;
    private float direction;
    private final ArrayList<Contact> contacts = new ArrayList<>(40);

    public RadarSimple() {
        this.minimapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
        this.textureAtlas = new TextureAtlas("pings");
        this.textureAtlas.setFilter(false, false);
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.loadTexturePackIcons();
    }

    private void loadTexturePackIcons() {
        this.completedLoading = false;

        try {
            this.textureAtlas.reset();
            BufferedImage contact = ImageUtils.loadImage(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/contact.png"), 0, 0, 32, 32, 32, 32);
            contact = ImageUtils.fillOutline(contact, false, true, 32.0F, 32.0F, 0);
            this.textureAtlas.registerIconForBufferedImage("contact", contact);
            BufferedImage facing = ImageUtils.loadImage(ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/contact_facing.png"), 0, 0, 32, 32, 32, 32);
            facing = ImageUtils.fillOutline(facing, false, true, 32.0F, 32.0F, 0);
            this.textureAtlas.registerIconForBufferedImage("facing", facing);
            this.textureAtlas.stitch();
            applyFilteringParameters();
            this.completedLoading = true;
        } catch (Exception var4) {
            VoxelConstants.getLogger().error("Failed getting mobs " + var4.getLocalizedMessage(), var4);
        }

    }

    @Override
    public void onTickInGame(GuiGraphics drawContext, Matrix4fStack matrixStack, LayoutVariables layoutVariables, float scaleProj, float iconSize) {
        if (this.options.radarAllowed || this.options.radarMobsAllowed || this.options.radarPlayersAllowed) {
            this.layoutVariables = layoutVariables;
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
                this.calculateMobs();
                this.timer = 0;
            }

            ++this.timer;
            if (this.completedLoading) {
                this.renderMapMobs(drawContext, matrixStack, this.layoutVariables.mapX, this.layoutVariables.mapY, scaleProj, iconSize);
            }

            OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        }
    }

    public void calculateMobs() {
        this.contacts.clear();

        for (Entity entity : VoxelConstants.getClientWorld().entitiesForRendering()) {
            try {
                if (entity != null && !entity.isInvisibleTo(VoxelConstants.getPlayer()) && (this.options.showHostiles && (this.options.radarAllowed || this.options.radarMobsAllowed) && this.isHostile(entity)
                        || this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed) && this.isPlayer(entity) || this.options.showNeutrals && this.options.radarMobsAllowed && this.isNeutral(entity))) {
                    int wayX = GameVariableAccessShim.xCoord() - (int) entity.position().x();
                    int wayZ = GameVariableAccessShim.zCoord() - (int) entity.position().z();
                    int wayY = GameVariableAccessShim.yCoord() - (int) entity.position().y();
                    double hypot = wayX * wayX + wayZ * wayZ + wayY * wayY;
                    hypot /= this.layoutVariables.zoomScaleAdjusted * this.layoutVariables.zoomScaleAdjusted;
                    if (hypot < 961.0) {
                        Contact contact = new Contact((LivingEntity) entity, this.getUnknownMobNeutrality(entity));
                        String unscrubbedName = contact.entity.getDisplayName().getString();
                        contact.setName(unscrubbedName);
                        contact.updateLocation();
                        this.contacts.add(contact);
                    }
                }
            } catch (Exception var11) {
                VoxelConstants.getLogger().error(var11.getLocalizedMessage(), var11);
            }
        }

        this.contacts.sort(Comparator.comparingInt(contact -> contact.y));
    }

    private EnumMobs getUnknownMobNeutrality(Entity entity) {
        if (this.isHostile(entity)) {
            return EnumMobs.GENERIC_HOSTILE;
        } else {
            return !(entity instanceof TamableAnimal) || !((TamableAnimal) entity).isTame() || !VoxelConstants.getMinecraft().hasSingleplayerServer() && !((TamableAnimal) entity).getOwner().equals(VoxelConstants.getPlayer()) ? EnumMobs.GENERIC_NEUTRAL : EnumMobs.GENERIC_TAME;
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

    public void renderMapMobs(GuiGraphics drawContext, Matrix4fStack matrixStack, int x, int y, float scaleProj, float iconSize) {
        double lastX = GameVariableAccessShim.xCoordDouble();
        double lastZ = GameVariableAccessShim.zCoordDouble();
        int lastY = GameVariableAccessShim.yCoord();
        double max = this.layoutVariables.zoomScaleAdjusted * 32.0;
        OpenGL.Utils.disp2(this.textureAtlas.getId());

        for (Contact contact : this.contacts) {
            contact.updateLocation();
            double wayX = lastX - contact.x;
            double wayZ = lastZ - contact.z;
            int wayY = lastY - contact.y;
            double entityMax = max;
            if (contact.type == EnumMobs.PHANTOM) {
                entityMax *= 2;
            }
            double adjustedDiff = entityMax - Math.max(Math.abs(wayY), 0);
            contact.brightness = (float) Math.max(adjustedDiff / entityMax, 0.0);
            contact.brightness *= contact.brightness;
            contact.angle = (float) Math.toDegrees(Math.atan2(wayX, wayZ));
            contact.distance = Math.sqrt(wayX * wayX + wayZ * wayZ) / this.layoutVariables.zoomScaleAdjusted;
            OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);
            float red;
            float green;
            float blue;
            if (isHostile(contact.entity)) {
                red = 1f; green = 0.25f; blue = 0f;
            } else {
                red = 1f; green = 1f; blue = 1f;
            }
            if (wayY < 0) {
                OpenGL.glColor4f(red, green, blue, contact.brightness);
            } else {
                contact.brightness = Math.max(contact.brightness, 0.3f);
                OpenGL.glColor3f(red * contact.brightness, green * contact.brightness, blue * contact.brightness);
            }

            if (this.layoutVariables.rotating) {
                contact.angle += this.direction;
            } else if (this.minimapOptions.oldNorth) {
                contact.angle -= 90.0F;
            }

            boolean inRange;
            if (this.minimapOptions.shape != 1) {
                inRange = contact.distance < 31.0;
            } else {
                double radLocate = Math.toRadians(contact.angle);
                double dispX = contact.distance * Math.cos(radLocate);
                double dispY = contact.distance * Math.sin(radLocate);
                inRange = Math.abs(dispX) <= 28.5 && Math.abs(dispY) <= 28.5;
            }

            if (inRange) {
                try {
                    matrixStack.pushMatrix();
                    float contactFacing = contact.entity.getYHeadRot();
                    if (this.layoutVariables.rotating) {
                        contactFacing -= this.direction;
                    } else if (this.minimapOptions.oldNorth) {
                        contactFacing += 90.0F;
                    }

                    matrixStack.translate(x, y, 0.0f);
                    matrixStack.rotate(Axis.ZP.rotationDegrees(-contact.angle));
                    matrixStack.translate(0.0f, (float) -contact.distance, 0.0f);
                    matrixStack.rotate(Axis.ZP.rotationDegrees(contact.angle + contactFacing));
                    matrixStack.translate(-x, -y, 0.0f);

                    // this.applyFilteringParameters();
                    OpenGL.Utils.drawPre();
                    OpenGL.Utils.setMap(this.textureAtlas.getAtlasSprite("contact"), x, y, 12.0F * iconSize);
                    OpenGL.Utils.drawPost();
                    if (this.options.showFacing) {
                        // this.applyFilteringParameters();
                        OpenGL.Utils.drawPre();
                        OpenGL.Utils.setMap(this.textureAtlas.getAtlasSprite("facing"), x, y, 12.0F * iconSize);
                        OpenGL.Utils.drawPost();
                    }

                    if (contact.name != null && ((this.options.showPlayerNames && this.isPlayer(contact.entity)) || (this.options.showMobNames && !this.isPlayer(contact.entity) && (!this.options.showNamesOnlyForTagged || contact.entity.hasCustomName())))) {
                        float fontSize = this.options.fontSize * iconSize;
                        float scaleFactor = 1f / fontSize;
                        String mobName = contact.entity.getDisplayName().getString();
                        int halfStringWidth = VoxelConstants.getMinecraft().font.width(mobName) / 2;
                        int textR = ARGB.red(contact.entity.getTeamColor());
                        int textG = ARGB.green(contact.entity.getTeamColor());
                        int textB = ARGB.blue(contact.entity.getTeamColor());
                        if (wayY < 0) {
                            textR *= contact.brightness;
                            textG *= contact.brightness;
                            textB *= contact.brightness;
                        }
                        int textAlpha = (int) (contact.brightness * 255);
                        PoseStack textMatrixStack = drawContext.pose();
                        textMatrixStack.pushPose();
                        textMatrixStack.setIdentity();
                        textMatrixStack.scale(scaleProj * fontSize, scaleProj * fontSize, 1.0f);
                        wayX = Math.sin(Math.toRadians(contact.angle)) * contact.distance;
                        wayZ = Math.cos(Math.toRadians(contact.angle)) * contact.distance;
                        textMatrixStack.translate(-wayX * scaleFactor, -wayZ * scaleFactor, 900.0f);
                        drawContext.drawString(VoxelConstants.getMinecraft().font, mobName, (int) (x * scaleFactor - halfStringWidth), (int) (y * scaleFactor + 10.0f), ARGB.color(textAlpha, textR, textG, textB));
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
}
