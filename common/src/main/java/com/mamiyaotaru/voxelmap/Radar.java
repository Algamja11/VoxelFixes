package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.entityrender.EntityMapImageManager;
import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mamiyaotaru.voxelmap.util.MobCategory;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mojang.math.Axis;
import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;

public class Radar implements IRadar {
    private final MapSettingsManager minimapOptions;
    private final RadarSettingsManager options;
    private final ArrayList<Contact> contacts = new ArrayList<>(40);
    private final EntityMapImageManager entityMapImageManager;
    private final Minecraft minecraft = Minecraft.getInstance();

    private int timer = 500;
    private float direction;
    private boolean lastOutlines = true;
    private int calculateMobsPart;

    public Radar() {
        entityMapImageManager = new EntityMapImageManager();
        this.minimapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        entityMapImageManager.reset();
    }

    @Override
    public void onTickInGame(GuiGraphics drawContext, LayoutVariables layoutVariables) {
        entityMapImageManager.onRenderTick(drawContext);
        if (this.options.radarAllowed || this.options.radarMobsAllowed || this.options.radarPlayersAllowed) {
            if (this.options.isChanged()) {
                this.timer = 500;
                if (this.options.outlines != this.lastOutlines) {
                    this.lastOutlines = this.options.outlines;
                    entityMapImageManager.reset();
                }
            }

            this.direction = GameVariableAccessShim.rotationYaw() + 180.0F;

            while (this.direction >= 360.0F) {
                this.direction -= 360.0F;
            }

            while (this.direction < 0.0F) {
                this.direction += 360.0F;
            }

            if (this.timer > 15) {
                // long t0 = System.nanoTime();
                this.calculateMobs(layoutVariables);
                // long t1 = System.nanoTime();
                // VoxelConstants.getLogger().info("Calculate Mobs " + calculateMobsPart + " took " + ((t1 - t0) / 1000) + " micros");
                this.timer = 0;
            }

            ++this.timer;
            this.renderMapMobs(drawContext, layoutVariables);
        }
    }

    private boolean isEntityShown(Entity entity) {
        return entity != null && !entity.isInvisibleTo(VoxelConstants.getPlayer()) && (this.options.showHostiles && (this.options.radarAllowed || this.options.radarMobsAllowed) && this.isHostile(entity)
                || this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed) && this.isPlayer(entity) || this.options.showNeutrals && this.options.radarMobsAllowed && this.isNeutral(entity));
    }

    public void calculateMobs(LayoutVariables layoutVariables) {
        calculateMobsPart = (calculateMobsPart + 1) & 7;
        this.contacts.removeIf(e -> (e.uuid.getLeastSignificantBits() & 7) == calculateMobsPart);
        // this.contacts.clear();

        Iterable<Entity> entities = VoxelConstants.getClientWorld().entitiesForRendering();

        for (Entity entity : entities) {
            if ((entity.getUUID().getLeastSignificantBits() & 7) != calculateMobsPart) {
                continue;
            }
            try {
                if (this.isEntityShown(entity)) {
                    int halfMapSize = layoutVariables.mapSize / 2;
                    int wayX = GameVariableAccessShim.xCoord() - (int) entity.position().x();
                    int wayZ = GameVariableAccessShim.zCoord() - (int) entity.position().z();
                    int wayY = GameVariableAccessShim.yCoord() - (int) entity.position().y();
                    double hypot = wayX * wayX + wayZ * wayZ + wayY * wayY;
                    hypot *= layoutVariables.positionScale * layoutVariables.positionScale;
                    if (hypot < halfMapSize * halfMapSize) {

                        Contact contact = new Contact((LivingEntity) entity, MobCategory.forEntity(entity));
                        if (contact.entity.getVehicle() != null && this.isEntityShown(contact.entity.getVehicle())) {
                            contact.yFudge = 1;
                        }
                        if (VoxelMap.radarOptions.isMobEnabled(contact.entity)) {
                            if (contact.icon == null) {
                                contact.icon = entityMapImageManager.requestImageForMob(contact.entity, 32, true);
                            }

                            String scrubbedName = TextUtils.scrubCodes(contact.entity.getName().getString());
                            if ((scrubbedName.equals("Dinnerbone") || scrubbedName.equals("Grumm")) && (!(contact.entity instanceof Player) || ((Player) contact.entity).isModelPartShown(PlayerModelPart.CAPE))) {
                                contact.setRotationFactor(contact.rotationFactor + 180);
                            }

                            if (this.options.showHelmetsPlayers && contact.category == MobCategory.PLAYER || this.options.showHelmetsMobs && contact.category != MobCategory.PLAYER) {
                                // this.getArmor(contact, entity);
                            }

                            this.contacts.add(contact);
                        }
                    }
                }
            } catch (Exception var16) {
                VoxelConstants.getLogger().error(var16.getLocalizedMessage(), var16);
            }
        }

        this.contacts.sort((c1, c2) -> {
            double dy = c1.y - c2.y;
            if (dy != 0) {
                return dy > 0 ? 1 : -1;
            }
            double dx = c1.x - c2.x;
            if (dx != 0) {
                return dx > 0 ? 1 : -1;
            }
            double dz = c1.z - c2.z;
            if (dz != 0) {
                return dz > 0 ? 1 : -1;
            }
            return 0;
        });
    }

    public void renderMapMobs(GuiGraphics guiGraphics, LayoutVariables layoutVariables) {
        int mapX = layoutVariables.mapX;
        int mapY = layoutVariables.mapY;
        int halfMapSize = layoutVariables.mapSize / 2;
        int scScale = layoutVariables.scScale;

        double max = layoutVariables.zoomScaleAdjusted * 32.0;
        double lastX = GameVariableAccessShim.xCoordDouble();
        double lastZ = GameVariableAccessShim.zCoordDouble();
        double lastY = GameVariableAccessShim.yCoordDouble();

        for (Contact contact : this.contacts) {
            contact.updateLocation();
            double contactX = contact.x;
            double contactZ = contact.z;
            double contactY = contact.y;
            double wayX = lastX - contactX;
            double wayZ = lastZ - contactZ;
            double wayY = lastY - contactY;
            double entityMax = max;
            if (contact.entity.getType() == EntityType.PHANTOM) {
                entityMax *= 2;
            }
            double adjustedDiff = entityMax - Math.max(Math.abs(wayY), 0);
            contact.brightness = (float) Math.max(adjustedDiff / entityMax, 0.0);
            contact.brightness *= contact.brightness;
            contact.angle = (float) Math.toDegrees(Math.atan2(wayX, wayZ));
            contact.distance = Math.sqrt(wayX * wayX + wayZ * wayZ) * layoutVariables.positionScale;

            int color;
            if (wayY < 0) {
                color = ARGB.colorFromFloat(contact.brightness, 1.0F, 1.0F, 1.0F);
            } else {
                if (contact.brightness < 0.3f) {
                    contact.brightness = 0.3f;
                }
                color = ARGB.colorFromFloat(1.0f, contact.brightness, contact.brightness, contact.brightness);
            }

            if (layoutVariables.rotates) {
                contact.angle += this.direction;
            } else if (this.minimapOptions.oldNorth) {
                contact.angle -= 90.0F;
            }

            boolean inRange;
            if (!layoutVariables.squareMap) {
                inRange = contact.distance < (halfMapSize - 3.5);
            } else {
                double radLocate = Math.toRadians(contact.angle);
                double dispX = contact.distance * Math.cos(radLocate);
                double dispY = contact.distance * Math.sin(radLocate);
                inRange = Math.abs(dispX) <= (halfMapSize - 3.5) && Math.abs(dispY) <= (halfMapSize - 3.5);
            }

            if (inRange) {
                try {
                    guiGraphics.pose().pushPose();
                    if (this.options.filtering) {
                        guiGraphics.pose().translate(mapX, mapY, 0.0f);
                        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-contact.angle));
                        guiGraphics.pose().translate(0.0f, (float) -contact.distance, 0.0f);
                        guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(contact.angle + contact.rotationFactor));
                        guiGraphics.pose().translate(-mapX, -mapY, 0.0f);
                    } else {
                        wayZ = Math.cos(Math.toRadians(contact.angle)) * contact.distance;
                        wayX = Math.sin(Math.toRadians(contact.angle)) * contact.distance;
                        guiGraphics.pose().translate((float) Math.round(-wayX * scScale) / scScale, (float) Math.round(-wayZ * scScale) / scScale, 0.0f);
                    }

                    float yOffset = 0.0F;
                    if (contact.entity.getVehicle() != null && this.isEntityShown(contact.entity.getVehicle())) {
                        yOffset = -4.0F;
                    }

                    // if (Stream.of(EnumMobs.GHAST, EnumMobs.GHASTATTACKING, EnumMobs.WITHER, EnumMobs.WITHERINVULNERABLE, EnumMobs.VEX, EnumMobs.VEXCHARGING, EnumMobs.PUFFERFISH, EnumMobs.PUFFERFISHHALF, EnumMobs.PUFFERFISHFULL).anyMatch(enumMobs -> contact.type == enumMobs)) {
                    // if (contact.type != EnumMobs.GHAST && contact.type != EnumMobs.GHASTATTACKING) {
                    // if (contact.type != EnumMobs.WITHER && contact.type != EnumMobs.WITHERINVULNERABLE) {
                    // if (contact.type != EnumMobs.VEX && contact.type != EnumMobs.VEXCHARGING) {
                    // int size = ((Pufferfish) contact.entity).getPuffState();
                    // switch (size) {
                    // case 0 -> contact.type = EnumMobs.PUFFERFISH;
                    // case 1 -> contact.type = EnumMobs.PUFFERFISHHALF;
                    // case 2 -> contact.type = EnumMobs.PUFFERFISHFULL;
                    // }
                    // } else {
                    // if (contact.entity instanceof Vex vex) {
                    // contact.type = vex.isCharging() ? EnumMobs.VEXCHARGING : EnumMobs.VEX;
                    // }
                    // }
                    // } else {
                    // if (contact.entity instanceof WitherBoss witherBoss) {
                    // contact.type = witherBoss.getInvulnerableTicks() > 0 ? EnumMobs.WITHERINVULNERABLE : EnumMobs.WITHER;
                    // }
                    // }
                    // } else {
                    // if (contact.entity instanceof Ghast ghast) {
                    // contact.type = ghast.isCharging() ? EnumMobs.GHASTATTACKING : EnumMobs.GHAST;
                    // }
                    // }
                    // }

                    float imageSize = contact.icon.getIconWidth() / 8.0F;
                    contact.icon.blit(guiGraphics, GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH, mapX - imageSize / 2, mapY + yOffset - imageSize / 2, imageSize, imageSize, color);

                    if (contact.name != null && ((this.options.showPlayerNames && contact.category == MobCategory.PLAYER) || (this.options.showMobNames && contact.category != MobCategory.PLAYER && contact.entity.hasCustomName()))) {
                        float fontSize = this.options.fontSize / 4.0F;
                        guiGraphics.pose().scale(fontSize, fontSize, 1.0F);

                        int labelColor = ((int) (contact.brightness * 255.0F) << 24) | (contact.entity.getTeamColor() & 0x00FFFFFF);
                        int halfStringWidth = minecraft.font.width(contact.name) / 2;
                        guiGraphics.fill((int) (mapX / fontSize - halfStringWidth - 1), (int) ((mapY + 3) / fontSize + 9), (int) (mapX / fontSize + halfStringWidth + 1), (int) ((mapY + 3) / fontSize - 1), 0x40000000);
                        guiGraphics.drawString(minecraft.font, contact.name, (int) (mapX / fontSize - halfStringWidth), (int) ((mapY + 3) / fontSize), labelColor, true);
                    }
                } catch (Exception e) {
                    VoxelConstants.getLogger().error("Error rendering mob icon! " + e.getLocalizedMessage() + " contact type " + BuiltInRegistries.ENTITY_TYPE.getKey(contact.entity.getType()), e);
                } finally {
                    guiGraphics.pose().popPose();
                }
            }
        }
    }

    private boolean isHostile(Entity entity) {
        switch (entity) {
            case Bee bee -> { return bee.isAngry(); }
            case PolarBear polarBear -> {
                for (PolarBear object : polarBear.level().getEntitiesOfClass(PolarBear.class, polarBear.getBoundingBox().inflate(8.0, 4.0, 8.0))) {
                    if (object.isBaby()) {
                        return true;
                    }
                }
                return polarBear.isAngry();
            }
            case Rabbit rabbit -> { return rabbit.getVariant() == Rabbit.Variant.EVIL; }
            case Wolf wolf -> { return wolf.isAngry(); }
            case ZombifiedPiglin zombifiedPiglin -> { return zombifiedPiglin.isAngry(); }
            case Enemy ignored -> { return true; }

            default -> { return false; }
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

    public void onJoinServer() {
        entityMapImageManager.reset();
    }

    public EntityMapImageManager getEntityMapImageManager() {
        return entityMapImageManager;
    }
}
