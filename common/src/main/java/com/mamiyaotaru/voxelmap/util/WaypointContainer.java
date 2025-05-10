package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class WaypointContainer {
    private final List<ExtendedWaypoint> wayPts = new ArrayList<>();
    private Waypoint highlightedWaypoint;
    public final MapSettingsManager options;
    public final Minecraft minecraft = Minecraft.getInstance();

    public static class ExtendedWaypoint implements Comparable<ExtendedWaypoint> {
        public Waypoint waypoint;
        public double diff;

        public ExtendedWaypoint(Waypoint waypoint) {
            this.waypoint = waypoint;
        }

        public int compareTo(ExtendedWaypoint o) {
            if (this.diff < 0.0 && o.diff >= 0.0) return 1;
            if (this.diff >= 0.0 && o.diff < 0.0) return -1;

            return Double.compare(this.diff, o.diff);
        }
    }


    public WaypointContainer(MapSettingsManager options) {
        this.options = options;
    }

    public void addWaypoint(Waypoint newWaypoint) {
        this.wayPts.add(new ExtendedWaypoint(newWaypoint));
    }

    public void removeWaypoint(Waypoint waypoint) {
        this.wayPts.removeIf(extendedWaypoint -> extendedWaypoint.waypoint == waypoint);
    }

    public void setHighlightedWaypoint(Waypoint highlightedWaypoint) {
        this.highlightedWaypoint = highlightedWaypoint;
    }

    public void renderWaypoints(float gameTimeDeltaPartialTick, PoseStack poseStack, BufferSource bufferSource, Camera camera) {
        Vec3 cameraPos = camera.getPosition();
        double renderPosX = cameraPos.x;
        double renderPosY = cameraPos.y;
        double renderPosZ = cameraPos.z;

        if (this.options.showWaypointBeacons) {
            for (ExtendedWaypoint pt : this.wayPts) {
                if (pt.waypoint.isActive() || pt.waypoint == this.highlightedWaypoint) {
                    int x = pt.waypoint.getX();
                    int z = pt.waypoint.getZ();
                    double bottomOfWorld = VoxelConstants.getPlayer().level().getMinY() - renderPosY;
                    this.renderBeam(pt.waypoint, x - renderPosX, bottomOfWorld, z - renderPosZ, poseStack, bufferSource);
                }
            }
        }

        if (this.options.showWaypointSigns) {
            this.wayPts.sort(Collections.reverseOrder(ExtendedWaypoint::compareTo));
            int last = this.wayPts.size() - 1;
            int count = 0;
            for (ExtendedWaypoint pt : this.wayPts) {
                if (pt.waypoint.isActive() || pt.waypoint == this.highlightedWaypoint) {
                    int x = pt.waypoint.getX();
                    int z = pt.waypoint.getZ();
                    int y = pt.waypoint.getY();
                    double distance = Math.sqrt(pt.waypoint.getDistanceSqToCamera(camera));
                    if ((distance < this.options.maxWaypointDisplayDistance || this.options.maxWaypointDisplayDistance < 0 || pt.waypoint == this.highlightedWaypoint) && !minecraft.options.hideGui) {
                        pt.diff = this.getDiff(pt.waypoint, distance, camera);
                        if (this.minecraft.options.keyShift.isDown()) {
                            this.renderIcon(poseStack, bufferSource, pt.waypoint, distance, pt.diff >= 0.0, pt.waypoint.name, false, x - renderPosX, y - renderPosY + 1.12, z - renderPosZ);
                        } else {
                            this.renderIcon(poseStack, bufferSource, pt.waypoint, distance, pt.diff >= 0.0 && count == last, pt.waypoint.name, false, x - renderPosX, y - renderPosY + 1.12, z - renderPosZ);
                        }
                    }
                }
                ++count;
            }

            if (this.highlightedWaypoint != null && !minecraft.options.hideGui) {
                int x = this.highlightedWaypoint.getX();
                int z = this.highlightedWaypoint.getZ();
                int y = this.highlightedWaypoint.getY();
                double distance = Math.sqrt(this.highlightedWaypoint.getDistanceSqToCamera(camera));
                boolean isPointedAt = this.getDiff(this.highlightedWaypoint, distance, camera) >= 0.0;
                this.renderIcon(poseStack, bufferSource, this.highlightedWaypoint, distance, isPointedAt, "", true, x - renderPosX, y - renderPosY + 1.12, z - renderPosZ);
            }
        }
    }

    private double getDiff(Waypoint waypoint, double distance, Camera camera) {
        double degrees = 5.0 + Math.min(5.0 / distance, 5.0);
        double angle = Math.toRadians(degrees);
        double size = Math.sin(angle) * distance * this.options.waypointIconSize;
        Vec3 cameraPos = camera.getPosition();
        Vector3f lookVector = camera.getLookVector();
        Vec3 lookingPos = cameraPos.add(lookVector.x * distance, lookVector.y * distance, lookVector.z * distance);
        float centerX = waypoint.getX() + 0.5F;
        float centerY = waypoint.getY() + 1.65F;
        float centerZ = waypoint.getZ() + 0.5F;
        AABB boundingBox = new AABB(centerX - size, centerY - size, centerZ - size, centerX + size, centerY + size, centerZ + size);
        Optional<Vec3> raycastResult = boundingBox.clip(cameraPos, lookingPos);
        if (!boundingBox.contains(cameraPos) && raycastResult.isEmpty()) {
            return -1.0;
        } else if (distance <= 5.0) {
            return 0.0;
        } else {
            double dx = lookingPos.x - centerX;
            double dy = lookingPos.y - centerY;
            double dz = lookingPos.z - centerZ;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    private void renderBeam(Waypoint par1EntityWaypoint, double baseX, double baseY, double baseZ, PoseStack poseStack, BufferSource bufferSource) {
        int height = VoxelConstants.getClientWorld().getHeight();
        float brightness = 0.1F;
        double topWidthFactor = 1.05;
        double bottomWidthFactor = 1.05;
        float r = par1EntityWaypoint.red;
        float b = par1EntityWaypoint.blue;
        float g = par1EntityWaypoint.green;

        VertexConsumer vertexConsumerBeam = bufferSource.getBuffer(GLUtils.WAYPOINT_BEAM);

        for (int width = 0; width < 4; ++width) {
            double d6 = 0.1 + width * 0.2;
            d6 *= topWidthFactor;
            double d7 = 0.1 + width * 0.2;
            d7 *= bottomWidthFactor;

            for (int side = 0; side < 5; ++side) {
                float vertX2 = (float) (baseX + 0.5 - d6);
                float vertZ2 = (float) (baseZ + 0.5 - d6);
                if (side == 1 || side == 2) {
                    vertX2 = (float) (vertX2 + d6 * 2.0);
                }

                if (side == 2 || side == 3) {
                    vertZ2 = (float) (vertZ2 + d6 * 2.0);
                }

                float vertX1 = (float) (baseX + 0.5 - d7);
                float vertZ1 = (float) (baseZ + 0.5 - d7);
                if (side == 1 || side == 2) {
                    vertX1 = (float) (vertX1 + d7 * 2.0);
                }

                if (side == 2 || side == 3) {
                    vertZ1 = (float) (vertZ1 + d7 * 2.0);
                }

                vertexConsumerBeam.addVertex(poseStack.last(), vertX1, (float) baseY + 0.0F, vertZ1).setColor(r * brightness, g * brightness, b * brightness, 0.8F);
                vertexConsumerBeam.addVertex(poseStack.last(), vertX2, (float) baseY + height, vertZ2).setColor(r * brightness, g * brightness, b * brightness, 0.8F);
            }
        }
    }

    private void renderIcon(PoseStack poseStack, BufferSource bufferSource, Waypoint pt, double distance, boolean showLabel, String name, boolean target, double baseX, double baseY, double baseZ) {
        if (target) {
            if (pt.red == 2.0F && pt.green == 0.0F && pt.blue == 0.0F) {
                name = "X:" + pt.getX() + ", Y:" + pt.getY() + ", Z:" + pt.getZ();
            } else {
                showLabel = false;
            }
        }

        double maxDistance = minecraft.options.simulationDistance().get() * 16.0 * 0.99;
        double adjustedDistance = distance;
        if (distance > maxDistance) {
            baseX = baseX / distance * maxDistance;
            baseY = baseY / distance * maxDistance;
            baseZ = baseZ / distance * maxDistance;
            adjustedDistance = maxDistance;
        }

        float var14 = ((float) adjustedDistance * 0.1F + 1.0F) * 0.0266F * this.options.waypointIconSize;
        poseStack.pushPose();
        poseStack.translate((float) baseX + 0.5F, (float) baseY + 0.5F, (float) baseZ + 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-minecraft.getEntityRenderDispatcher().camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(minecraft.getEntityRenderDispatcher().camera.getXRot()));
        poseStack.scale(-var14, -var14, -var14);
        float fade = distance > 5.0 ? 1.0F : (float) distance / 5.0F;
        float fadeNoDepth = fade * 0.4F;
        fade = Math.min(fade, !pt.enabled && !target ? 0.3F : 1.0F);
        float width = 10.0F;
        float r = target ? 1.0F : pt.red;
        float g = target ? 0.0F : pt.green;
        float b = target ? 0.0F : pt.blue;
        TextureAtlas textureAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
        Sprite icon = target ? textureAtlas.getAtlasSprite("voxelmap:images/waypoints/target.png") : textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + pt.imageSuffix + ".png");
        if (icon == textureAtlas.getMissingImage()) {
            icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
        }

        RenderType renderType = GLUtils.WAYPOINT_ICON_DEPTHTEST.apply(icon.getResourceLocation());
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        vertexConsumer.addVertex(poseStack.last(), -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(r, g, b, fade);
        vertexConsumer.addVertex(poseStack.last(), -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(r, g, b, fade);
        vertexConsumer.addVertex(poseStack.last(), width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(r, g, b, fade);
        vertexConsumer.addVertex(poseStack.last(), width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(r, g, b, fade);

        renderType = GLUtils.WAYPOINT_ICON_NO_DEPTHTEST.apply(icon.getResourceLocation());
        vertexConsumer = bufferSource.getBuffer(renderType);
        vertexConsumer.addVertex(poseStack.last(), -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(r, g, b, fadeNoDepth);
        vertexConsumer.addVertex(poseStack.last(), -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(r, g, b, fadeNoDepth);
        vertexConsumer.addVertex(poseStack.last(), width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(r, g, b, fadeNoDepth);
        vertexConsumer.addVertex(poseStack.last(), width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(r, g, b, fadeNoDepth);

        Font fontRenderer = minecraft.font;
        if (showLabel && fontRenderer != null) {
            boolean aboveIcon = this.options.showWaypointNames == 1;
            String distanceStr = "";
            if (this.options.showWaypointNames == 0) {
                name = "";
            }
            if (this.options.showWaypointDistances != 0) {
                if (this.options.autoUnitConversion && distance >= 10000.0) {
                    double converted = distance / 1000.0;
                    distanceStr = (int) distance + "." + (int) ((converted - (int) converted) * 10) + "km";
                } else {
                    distanceStr = (int) distance + "." + (int) ((distance - (int) distance) * 10) + "m";
                }
                if (name.isEmpty()) {
                    aboveIcon = this.options.showWaypointDistances == 1;
                    name = "(" + distanceStr + ")";
                    distanceStr = "";
                } else if (this.options.showWaypointDistances == 1) {
                    name += " (" + distanceStr + ")";
                    distanceStr = "";
                }
            }

            int textColor = (int) (255.0F * fade) << 24 | 0x00FFFFFF;
            int textColorNoDepth = (int) (255.0F * fadeNoDepth) << 24 | 0x00FFFFFF;
            int labelY = aboveIcon ? -20 : 10;
            int halfLabelWidth = fontRenderer.width(name) / 2;
            float zOffset = 0.1F;

            if (!name.isEmpty()) {
                renderType = GLUtils.WAYPOINT_TEXT_BACKGROUND_DEPTHTEST;
                vertexConsumer = bufferSource.getBuffer(renderType);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 1), (-1 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 1), (8 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 1), (8 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 1), (-1 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);

                renderType = GLUtils.WAYPOINT_TEXT_BACKGROUND_NO_DEPTHTEST;
                vertexConsumer = bufferSource.getBuffer(renderType);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 1), (-1 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 1), (8 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 1), (8 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 1), (-1 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);

                poseStack.pushPose();
                poseStack.translate(0.0F, 0.0F, zOffset * 2.0F);
                fontRenderer.drawInBatch(Component.literal(name), (-fontRenderer.width(name) / 2f), labelY, textColor, false, poseStack.last().pose(), bufferSource, DisplayMode.NORMAL, 0, 0x00F000F0);
                fontRenderer.drawInBatch(Component.literal(name), (-fontRenderer.width(name) / 2f), labelY, textColorNoDepth, false, poseStack.last().pose(), bufferSource, DisplayMode.SEE_THROUGH, 0, 0x00F000F0);
                poseStack.popPose();
            }

            if (!distanceStr.isEmpty()) {
                labelY = aboveIcon ? -40 : 30;
                halfLabelWidth = fontRenderer.width(distanceStr) / 2;

                poseStack.pushPose();
                poseStack.scale(0.65F, 0.65F, 1.0F);

                renderType = GLUtils.WAYPOINT_TEXT_BACKGROUND_DEPTHTEST;
                vertexConsumer = bufferSource.getBuffer(renderType);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 1), (-1 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 1), (8 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 1), (8 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 1), (-1 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);

                renderType = GLUtils.WAYPOINT_TEXT_BACKGROUND_NO_DEPTHTEST;
                vertexConsumer = bufferSource.getBuffer(renderType);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 1), (-1 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);
                vertexConsumer.addVertex(poseStack.last(), (-halfLabelWidth - 1), (8 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 1), (8 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);
                vertexConsumer.addVertex(poseStack.last(), (halfLabelWidth + 1), (-1 + labelY), zOffset).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);

                poseStack.translate(0.0F, 0.0F, zOffset * 2.0F);
                fontRenderer.drawInBatch(Component.literal(distanceStr), (-fontRenderer.width(distanceStr) / 2f), labelY, textColor, false, poseStack.last().pose(), bufferSource, DisplayMode.NORMAL, 0, 0x00F000F0);
                fontRenderer.drawInBatch(Component.literal(distanceStr), (-fontRenderer.width(distanceStr) / 2f), labelY, textColorNoDepth, false, poseStack.last().pose(), bufferSource, DisplayMode.SEE_THROUGH, 0, 0x00F000F0);

                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }
}
