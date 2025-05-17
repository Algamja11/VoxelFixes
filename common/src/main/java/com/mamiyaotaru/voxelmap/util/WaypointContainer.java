package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Comparator;
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
        public boolean target;

        public ExtendedWaypoint(Waypoint waypoint) {
            this.waypoint = waypoint;
        }

        public int compareTo(ExtendedWaypoint o) {
            boolean skip1 = diff == -1.0 || (!waypoint.enabled && !target) || !waypoint.inWorld || !waypoint.inDimension;
            boolean skip2 = o.diff == -1.0 || (!o.waypoint.enabled && !o.target) || !o.waypoint.inWorld || !o.waypoint.inDimension;

            if (skip1 && !skip2) return 1;
            if (!skip1 && skip2) return -1;

            return Double.compare(diff, o.diff);
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

        if (this.options.showWaypointSigns && !minecraft.options.hideGui) {
            TextureAtlas textureAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
            this.wayPts.sort(Comparator.reverseOrder());
            boolean shiftDown = minecraft.options.keyShift.isDown();
            int last = this.wayPts.size() - 1;
            int count = 0;
            for (ExtendedWaypoint pt : this.wayPts) {
                boolean highlighted = pt.waypoint == this.highlightedWaypoint;
                if (pt.waypoint.isActive() || highlighted) {
                    int x = pt.waypoint.getX();
                    int z = pt.waypoint.getZ();
                    int y = pt.waypoint.getY();
                    double distance = Math.sqrt(pt.waypoint.getDistanceSqToCamera(camera));
                    if (highlighted || this.options.maxWaypointDisplayDistance < 0 || distance < this.options.maxWaypointDisplayDistance) {
                        pt.diff = this.getDiff(pt.waypoint, distance, camera);
                        pt.target = highlighted;
                        if (shiftDown) {
                            this.renderIcon(poseStack, bufferSource, pt.waypoint, false, pt.diff != -1.0, distance, x - renderPosX, y - renderPosY + 1.12, z - renderPosZ, textureAtlas);
                        } else {
                            this.renderIcon(poseStack, bufferSource, pt.waypoint, false, pt.diff != -1.0 && count == last, distance, x - renderPosX, y - renderPosY + 1.12, z - renderPosZ, textureAtlas);
                        }
                    }
                }
                ++count;
            }

            if (this.highlightedWaypoint != null) {
                int x = this.highlightedWaypoint.getX();
                int z = this.highlightedWaypoint.getZ();
                int y = this.highlightedWaypoint.getY();
                double distance = Math.sqrt(this.highlightedWaypoint.getDistanceSqToCamera(camera));
                boolean showLabel = this.getDiff(this.highlightedWaypoint, distance, camera) >= 0.0;
                this.renderIcon(poseStack, bufferSource, this.highlightedWaypoint, true, showLabel, distance, x - renderPosX, y - renderPosY + 1.12, z - renderPosZ, textureAtlas);
            }
        }
    }

    private double getDiff(Waypoint waypoint, double distance, Camera camera) {
        double degrees = 3.0 + Math.min(5.0 / distance, 5.0);
        double angle = Math.toRadians(degrees);
        double size = Math.sin(angle) * distance * options.waypointIconSize;
        Vec3 cameraPos = camera.getPosition();
        Vector3f lookVector = camera.getLookVector();
        Vec3 lookingPos = cameraPos.add(lookVector.x * distance, lookVector.y * distance, lookVector.z * distance);
        float ptX = waypoint.getX() + 0.5F;
        float ptY = waypoint.getY() + 1.65F;
        float ptZ = waypoint.getZ() + 0.5F;
        AABB boundingBox = new AABB(ptX - size, ptY - size, ptZ - size, ptX + size, ptY + size,  ptZ + size);
        Optional<Vec3> raytraceResult = boundingBox.clip(cameraPos, lookingPos);
        if (boundingBox.contains(cameraPos)) {
            return 0.0;
        }
        if (raytraceResult.isPresent()) {
            return lookingPos.distanceToSqr(ptX, ptY, ptZ);
        }
        return -1.0;
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

    private void renderIcon(PoseStack poseStack, BufferSource bufferSource, Waypoint pt, boolean target, boolean showLabel, double distance, double baseX, double baseY, double baseZ, TextureAtlas textureAtlas) {
        float red = pt.red;
        float green = pt.green;
        float blue = pt.blue;
        String name = pt.name;
        if (target) {
            if (red == 2.0F && green == 0.0F && blue == 0.0F) {
                name = "X:" + pt.getX() + ", Y:" + pt.getY() + ", Z:" + pt.getZ();
            } else {
                red = 1.0F;
                green = 0.0F;
                blue = 0.0F;
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

        float scale = ((float) adjustedDistance * 0.1F + 1.0F) * 0.0266F * this.options.waypointIconSize;
        poseStack.pushPose();
        poseStack.translate((float) baseX + 0.5F, (float) baseY + 0.5F, (float) baseZ + 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-minecraft.getEntityRenderDispatcher().camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(minecraft.getEntityRenderDispatcher().camera.getXRot()));
        poseStack.scale(-scale, -scale, -scale);
        float fade = distance > 5.0 ? 1.0F : (float) distance / 5.0F;
        fade = Math.min(fade, !pt.enabled ? 0.5F : 1.0F);
        float fadeNoDepth = fade * 0.5F;
        float width = 10.0F;
        Sprite icon = target ? textureAtlas.getAtlasSprite("voxelmap:images/waypoints/target.png") : textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + pt.imageSuffix + ".png");
        if (icon == textureAtlas.getMissingImage()) {
            icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
        }

        RenderType renderType = GLUtils.WAYPOINT_ICON_DEPTHTEST.apply(icon.getResourceLocation());
        VertexConsumer vertexIconDepthtest = bufferSource.getBuffer(renderType);
        vertexIconDepthtest.addVertex(poseStack.last(), -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(red, green, blue, fade);
        vertexIconDepthtest.addVertex(poseStack.last(), -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(red, green, blue, fade);
        vertexIconDepthtest.addVertex(poseStack.last(), width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(red, green, blue, fade);
        vertexIconDepthtest.addVertex(poseStack.last(), width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(red, green, blue,  fade);

        renderType = GLUtils.WAYPOINT_ICON_NO_DEPTHTEST.apply(icon.getResourceLocation());
        VertexConsumer vertexIconNoDepthtest = bufferSource.getBuffer(renderType);
        vertexIconNoDepthtest.addVertex(poseStack.last(), -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(red, green, blue, fadeNoDepth);
        vertexIconNoDepthtest.addVertex(poseStack.last(), -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(red, green, blue, fadeNoDepth);
        vertexIconNoDepthtest.addVertex(poseStack.last(), width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(red, green, blue, fadeNoDepth);
        vertexIconNoDepthtest.addVertex(poseStack.last(), width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(red, green, blue, fadeNoDepth);

        Font fontRenderer = minecraft.font;
        if (showLabel) {
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
            int labelY = aboveIcon ? -24 : 10;
            int halfLabelWidth = fontRenderer.width(name) / 2;

            if (!name.isEmpty()) {
                renderType =  GLUtils.WAYPOINT_TEXT_BACKGROUND_DEPTHTEST;
                VertexConsumer vertexBackgroundDepthtest = bufferSource.getBuffer(renderType);
                vertexBackgroundDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBackgroundDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBackgroundDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBackgroundDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                GLUtils.polygonOffset(poseStack, 0.1F);
                vertexBackgroundDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 1), (-1 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBackgroundDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 1), (8 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBackgroundDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 1), (8 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBackgroundDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 1), (-1 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                GLUtils.resetPolygonOffset(poseStack);

                renderType = GLUtils.WAYPOINT_TEXT_BACKGROUND_NO_DEPTHTEST;
                VertexConsumer vertexBackgroundNoDepthtest = bufferSource.getBuffer(renderType);
                vertexBackgroundNoDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                vertexBackgroundNoDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                vertexBackgroundNoDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                vertexBackgroundNoDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                GLUtils.polygonOffset(poseStack, 0.1F);
                vertexBackgroundNoDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 1), (-1 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);
                vertexBackgroundNoDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 1), (8 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);
                vertexBackgroundNoDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 1), (8 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);
                vertexBackgroundNoDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 1), (-1 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);
                GLUtils.resetPolygonOffset(poseStack);

                GLUtils.polygonOffset(poseStack, 0.2F);
                fontRenderer.drawInBatch(Component.literal(name), (-fontRenderer.width(name) / 2f), labelY, textColor, false, poseStack.last().pose(), bufferSource, DisplayMode.SEE_THROUGH, 0, 0x00F000F0);
                GLUtils.resetPolygonOffset(poseStack);
            }

            if (!distanceStr.isEmpty()) {
                labelY = aboveIcon ? -20 : 26;
                halfLabelWidth = fontRenderer.width(distanceStr) / 2;

                poseStack.pushPose();
                poseStack.scale(0.75F, 0.75F, 1.0F);

                renderType = GLUtils.WAYPOINT_TEXT_BACKGROUND_DEPTHTEST;
                vertexIconDepthtest = bufferSource.getBuffer(renderType);
                vertexIconDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexIconDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexIconDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexIconDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                GLUtils.polygonOffset(poseStack, 0.1F);
                vertexIconDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 1), (-1 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexIconDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 1), (8 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexIconDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 1), (8 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexIconDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 1), (-1 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                GLUtils.resetPolygonOffset(poseStack);

                renderType = GLUtils.WAYPOINT_TEXT_BACKGROUND_NO_DEPTHTEST;
                vertexIconDepthtest = bufferSource.getBuffer(renderType);
                vertexIconDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                vertexIconDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                vertexIconDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 2), (9 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                vertexIconDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 2), (-2 + labelY), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fadeNoDepth);
                GLUtils.polygonOffset(poseStack, 0.1F);
                vertexIconDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 1), (-1 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);
                vertexIconDepthtest.addVertex(poseStack.last(), (-halfLabelWidth - 1), (8 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);
                vertexIconDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 1), (8 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);
                vertexIconDepthtest.addVertex(poseStack.last(), (halfLabelWidth + 1), (-1 + labelY), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fadeNoDepth);
                GLUtils.resetPolygonOffset(poseStack);

                GLUtils.polygonOffset(poseStack, 0.2F);
                fontRenderer.drawInBatch(Component.literal(distanceStr), (-fontRenderer.width(distanceStr) / 2f), labelY, textColor, false, poseStack.last().pose(), bufferSource, DisplayMode.SEE_THROUGH, 0, 0x00F000F0);
                GLUtils.resetPolygonOffset(poseStack);

                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }
}
