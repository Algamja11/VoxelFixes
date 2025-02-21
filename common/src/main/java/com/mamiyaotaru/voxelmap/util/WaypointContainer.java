package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.CoreShaders;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WaypointContainer {
    private final List<Waypoint> waypoints = new ArrayList<>();
    private final List<Map.Entry<Waypoint, Double>> centeredWaypoints = new ArrayList<>();
    private Waypoint highlightedWaypoint;
    public final MapSettingsManager options;

    public WaypointContainer(MapSettingsManager options) {
        this.options = options;
    }

    public void addWaypoint(Waypoint newWaypoint) {
        this.waypoints.add(newWaypoint);
    }

    public void removeWaypoint(Waypoint waypoint) {
        this.waypoints.remove(waypoint);
    }

    public void setHighlightedWaypoint(Waypoint highlightedWaypoint) {
        this.highlightedWaypoint = highlightedWaypoint;
    }

    public void renderWaypoints(Matrix4fStack matrixStack, boolean withDepth, boolean withoutDepth) {
        this.waypoints.sort(Collections.reverseOrder());
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        double renderPosX = cameraPos.x;
        double renderPosY = cameraPos.y;
        double renderPosZ = cameraPos.z;
        OpenGL.glEnable(OpenGL.GL11_GL_CULL_FACE);
        if (this.options.showBeacons) {
            OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
            OpenGL.glDepthMask(false);
            OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
            OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, 1);
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            for (Waypoint point : this.waypoints) {
                if (point.isActive() || point == this.highlightedWaypoint) {
                    int x = point.getX();
                    int z = point.getZ();
                    double distance = Math.sqrt(Math.pow(x - renderPosX, 2) + Math.pow(z - renderPosZ, 2));
                    LevelChunk chunk = VoxelConstants.getPlayer().level().getChunk(x >> 4, z >> 4);
                    if (chunk != null && !chunk.isEmpty() && VoxelConstants.getPlayer().level().hasChunk(x >> 4, z >> 4)) {
                        double bottomOfWorld = VoxelConstants.getPlayer().level().getMinY() - renderPosY;
                        this.renderBeam(point, distance, x - renderPosX, bottomOfWorld, z - renderPosZ, matrixStack);
                    }
                }
            }

            OpenGL.glDisable(OpenGL.GL11_GL_BLEND);
            OpenGL.glDepthMask(true);
        }

        if (this.options.showWaypoints) {
            OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
            OpenGL.glBlendFuncSeparate(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA, 1, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);

            this.centeredWaypoints.clear();
            for (Waypoint point : this.waypoints) {
                if (point.isActive() || point == this.highlightedWaypoint) {
                    double distance = Math.sqrt(point.getDistanceSqToCamera(camera));
                    if ((distance < this.options.maxWaypointDisplayDistance || this.options.maxWaypointDisplayDistance < 0 || point == this.highlightedWaypoint) && !VoxelConstants.getMinecraft().options.hideGui) {
                        double distFromCenter = this.isPointedAt(point, distance, camera);
                        if (distFromCenter >= 0.0) {
                            centeredWaypoints.add(new AbstractMap.SimpleEntry<>(point, distFromCenter));
                        } else {
                            int x = point.getX();
                            int y = point.getY();
                            int z = point.getZ();
                            this.renderLabel(matrixStack, point, distance, false, false, x - renderPosX, y - renderPosY + 1.12, z - renderPosZ, withDepth, withoutDepth);
                        }
                    }
                }
            }
            if (!this.centeredWaypoints.isEmpty()) {
                this.centeredWaypoints.sort(Comparator.comparingDouble(entry -> -entry.getValue()));
                int listSize = centeredWaypoints.size();
                for (int i = 0; i < listSize; i++) {
                    Waypoint point = centeredWaypoints.get(i).getKey();
                    int x = point.getX();
                    int y = point.getY();
                    int z = point.getZ();
                    double distance = Math.sqrt(point.getDistanceSqToCamera(camera));
                    this.renderLabel(matrixStack, point, distance, i == (listSize - 1), false, x - renderPosX, y - renderPosY + 1.12, z - renderPosZ, withDepth, withoutDepth);
                }
            }

            if (this.highlightedWaypoint != null && !VoxelConstants.getMinecraft().options.hideGui) {
                int x = this.highlightedWaypoint.getX();
                int y = this.highlightedWaypoint.getY();
                int z = this.highlightedWaypoint.getZ();
                double distance = Math.sqrt(this.highlightedWaypoint.getDistanceSqToCamera(camera));
                boolean isPointedAt = this.isPointedAt(this.highlightedWaypoint, distance, camera) >= 0.0;
                this.renderLabel(matrixStack, this.highlightedWaypoint, distance, isPointedAt, true, x - renderPosX, y - renderPosY + 1.12, z - renderPosZ, withDepth, withoutDepth);
            }

            OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
            OpenGL.glDepthMask(true);
            OpenGL.glDisable(OpenGL.GL11_GL_BLEND);
        }

    }

    private double isPointedAt(Waypoint waypoint, double distance, Camera camera) {
        double degrees = 5.0 + Math.min(5.0 / distance, 5.0);
        double angle = degrees * 0.0174533;
        double size = Math.sin(angle) * distance;
        Vec3 cameraPos = camera.getPosition();
        Vector3f lookVector = camera.getLookVector();
        Vec3 lookVectorAdjusted = cameraPos.add(lookVector.x * distance, lookVector.y * distance, lookVector.z * distance);
        float centeredX = waypoint.getX() + 0.5f;
        float centeredY = waypoint.getY() + 1.65f;
        float centeredZ = waypoint.getZ() + 0.5f;
        AABB axisAlignedBB = new AABB(
                centeredX - size, centeredY - size, centeredZ - size,
                centeredX + size, centeredY + size, centeredZ + size
        );
        Optional<Vec3> raycastResult = axisAlignedBB.clip(cameraPos, lookVectorAdjusted);
        if (axisAlignedBB.contains(cameraPos) || raycastResult.isPresent()) {
            if (distance > 5.0) {
                double dx = lookVectorAdjusted.x - centeredX;
                double dy = lookVectorAdjusted.y - centeredY;
                double dz = lookVectorAdjusted.z - centeredZ;
                return Math.sqrt(dx * dx + dy * dy + dz * dz) / size;
            } else {
                return 0.0;
            }
        } else {
            return -1.0;
        }
    }

    private void renderBeam(Waypoint waypoint, double distance, double baseX, double baseY, double baseZ, Matrix4f matrix4f) {
        Tesselator tesselator = Tesselator.getInstance();
        int height = VoxelConstants.getClientWorld().getHeight();
        float brightness = 0.06F;
        double widthFactor = Math.max(1.0, distance / 50.0);
        float r = waypoint.red;
        float g = waypoint.green;
        float b = waypoint.blue;
        float fade = Math.min(5.0f, (float)distance) / 5.0f;

        for (int width = 0; width < 4; ++width) {
            BufferBuilder vertexBuffer = tesselator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            double beamWidth = (0.1 + width * 0.2) * widthFactor;

            for (int side = 0; side < 5; ++side) {
                float vertexX_1 = (float) (baseX + 0.5 - beamWidth);
                float vertexZ_1 = (float) (baseZ + 0.5 - beamWidth);
                if (side == 1 || side == 2) {
                    vertexX_1 = (float) (vertexX_1 + beamWidth * 2.0);
                }
                if (side == 2 || side == 3) {
                    vertexZ_1 = (float) (vertexZ_1 + beamWidth * 2.0);
                }

                float vertexX_2 = (float) (baseX + 0.5 - beamWidth);
                float vertexY_2 = (float) (baseZ + 0.5 - beamWidth);
                if (side == 1 || side == 2) {
                    vertexX_2 = (float) (vertexX_2 + beamWidth * 2.0);
                }
                if (side == 2 || side == 3) {
                    vertexY_2 = (float) (vertexY_2 + beamWidth * 2.0);
                }

                vertexBuffer.addVertex(matrix4f, vertexX_1, (float) baseY, vertexZ_1).setColor(r * brightness, g * brightness, b * brightness, fade);
                vertexBuffer.addVertex(matrix4f, vertexX_2, (float) (baseY + height), vertexY_2).setColor(r * brightness, g * brightness, b * brightness, fade);
            }

            BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
        }

    }

    private void renderLabel(Matrix4fStack matrixStack, Waypoint point, double distance, boolean showLabel, boolean target, double baseX, double baseY, double baseZ, boolean withDepth, boolean withoutDepth) {
        String name = point.name;
        if (target) {
            if (point.red == 2.0F && point.green == 0.0F && point.blue == 0.0F) {
                name = "X:" + point.getX() + ", Y:" + point.getY() + ", Z:" + point.getZ();
            } else {
                showLabel = false;
            }
        }
        String distStr;
        if (this.options.distanceUnitConversion && distance > 10000.0) {
            distStr = (Math.round(distance / 100.0) / 10.0) + "km";
        } else if (distance >= 9999999.0F) {
            distStr = (int) distance  + "m";
        } else {
            distStr = (Math.round(distance * 10.0) / 10.0) + "m";
        }
        if (!this.options.waypointDistanceBelowName) {
            name = name + " (" + distStr + ")";
        }
        double maxDistance = VoxelConstants.getMinecraft().options.simulationDistance().get() * 16.0 * 0.99;
        double adjustedDistance = distance;
        if (distance > maxDistance) {
            baseX = baseX / distance * maxDistance;
            baseY = baseY / distance * maxDistance;
            baseZ = baseZ / distance * maxDistance;
            adjustedDistance = maxDistance;
        }

        float distanceBasedScale = ((float) adjustedDistance * 0.1F + 1.0F) * 0.0266F * this.options.waypointSize;
        matrixStack.pushMatrix();
        matrixStack.translate((float) baseX + 0.5F, (float) baseY + 0.5F, (float) baseZ + 0.5F);
        matrixStack.rotate(Axis.YP.rotationDegrees(-VoxelConstants.getMinecraft().getEntityRenderDispatcher().camera.getYRot()));
        matrixStack.rotate(Axis.XP.rotationDegrees(VoxelConstants.getMinecraft().getEntityRenderDispatcher().camera.getXRot()));
        matrixStack.scale(-distanceBasedScale);
        Tesselator tessellator = Tesselator.getInstance();
        float fade = distance > 5.0 ? 1.0F : (float) distance / 5.0F;
        fade = Math.min(fade, !point.enabled && !target ? 0.3F : 1.0F);
        float width = 10.0F;
        float r = target ? 1.0F : point.red;
        float g = target ? 0.0F : point.green;
        float b = target ? 0.0F : point.blue;
        TextureAtlas textureAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
        Sprite icon = target ? textureAtlas.getAtlasSprite("voxelmap:images/waypoints/target.png") : textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + point.imageSuffix + ".png");
        if (icon == textureAtlas.getMissingImage()) {
            icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
        }

        RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
        OpenGL.Utils.disp2(textureAtlas.getId());
        if (withDepth) {
            OpenGL.glDepthMask(distance < maxDistance);
            OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
            BufferBuilder vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            vertexBuffer.addVertex(matrixStack, -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(r, g, b, fade);
            vertexBuffer.addVertex(matrixStack, -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(r, g, b, fade);
            vertexBuffer.addVertex(matrixStack, width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(r, g, b, fade);
            vertexBuffer.addVertex(matrixStack, width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(r, g, b, fade);
            BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
        }

        if (withoutDepth) {
            OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
            OpenGL.glDepthMask(false);
            BufferBuilder vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            vertexBuffer.addVertex(matrixStack, -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(r, g, b, 0.3F * fade);
            vertexBuffer.addVertex(matrixStack, -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(r, g, b, 0.3F * fade);
            vertexBuffer.addVertex(matrixStack, width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(r, g, b, 0.3F * fade);
            vertexBuffer.addVertex(matrixStack, width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(r, g, b, 0.3F * fade);
            BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
        }

        Font fontRenderer = VoxelConstants.getMinecraft().font;
        if (showLabel && fontRenderer != null) {
            byte elevateBy = this.options.waypointNameBelowIcon ? (byte) 10 : (byte) -19;
            byte elevateDistBy = this.options.waypointNameBelowIcon ? (byte) 30: (byte) -39;
            float distTextScale = 0.65F;
            OpenGL.glEnable(OpenGL.GL11_GL_POLYGON_OFFSET_FILL);
            int halfStringWidth = fontRenderer.width(name) / 2;
            int halfDistStringWidth = fontRenderer.width(distStr) / 2;
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            if (withDepth) {
                OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
                OpenGL.glDepthMask(distance < maxDistance);
                OpenGL.glPolygonOffset(1.0F, 7.0F);
                BufferBuilder vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 2), (-2 + elevateBy), 0.0F).setColor(point.red, point.green, point.blue, 0.6F * fade);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 2), (9 + elevateBy), 0.0F).setColor(point.red, point.green, point.blue, 0.6F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 2), (9 + elevateBy), 0.0F).setColor(point.red, point.green, point.blue, 0.6F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 2), (-2 + elevateBy), 0.0F).setColor(point.red, point.green, point.blue, 0.6F * fade);
                BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
                OpenGL.glPolygonOffset(1.0F, 5.0F);
                vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());

                if (this.options.waypointDistanceBelowName) {
                    matrixStack.pushMatrix();
                    matrixStack.scale(distTextScale);
                    OpenGL.glPolygonOffset(1.0F, 7.0F);
                    vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                    vertexBuffer.addVertex(matrixStack, (-halfDistStringWidth - 2), (-2 + elevateDistBy), 0.0F).setColor(point.red, point.green, point.blue, 0.6F * fade);
                    vertexBuffer.addVertex(matrixStack, (-halfDistStringWidth - 2), (9 + elevateDistBy), 0.0F).setColor(point.red, point.green, point.blue, 0.6F * fade);
                    vertexBuffer.addVertex(matrixStack, (halfDistStringWidth + 2), (9 + elevateDistBy), 0.0F).setColor(point.red, point.green, point.blue, 0.6F * fade);
                    vertexBuffer.addVertex(matrixStack, (halfDistStringWidth + 2), (-2 + elevateDistBy), 0.0F).setColor(point.red, point.green, point.blue, 0.6F * fade);
                    BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
                    OpenGL.glPolygonOffset(1.0F, 5.0F);
                    vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                    vertexBuffer.addVertex(matrixStack, (-halfDistStringWidth - 1), (-1 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                    vertexBuffer.addVertex(matrixStack, (-halfDistStringWidth - 1), (8 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                    vertexBuffer.addVertex(matrixStack, (halfDistStringWidth + 1), (8 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                    vertexBuffer.addVertex(matrixStack, (halfDistStringWidth + 1), (-1 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                    BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
                    matrixStack.popMatrix();
                }
            }

            if (withoutDepth) {
                OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
                OpenGL.glDepthMask(false);
                OpenGL.glPolygonOffset(1.0F, 11.0F);
                BufferBuilder vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 2), (-2 + elevateBy), 0.0F).setColor(point.red, point.green, point.blue, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 2), (9 + elevateBy), 0.0F).setColor(point.red, point.green, point.blue, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 2), (9 + elevateBy), 0.0F).setColor(point.red, point.green, point.blue, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 2), (-2 + elevateBy), 0.0F).setColor(point.red, point.green, point.blue, 0.15F * fade);
                BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
                OpenGL.glPolygonOffset(1.0F, 9.0F);
                vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());

                if (this.options.waypointDistanceBelowName) {
                    matrixStack.pushMatrix();
                    matrixStack.scale(distTextScale);
                    OpenGL.glPolygonOffset(1.0F, 11.0F);
                    vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                    vertexBuffer.addVertex(matrixStack, (-halfDistStringWidth - 2), (-2 + elevateDistBy), 0.0F).setColor(point.red, point.green, point.blue, 0.15F * fade);
                    vertexBuffer.addVertex(matrixStack, (-halfDistStringWidth - 2), (9 + elevateDistBy), 0.0F).setColor(point.red, point.green, point.blue, 0.15F * fade);
                    vertexBuffer.addVertex(matrixStack, (halfDistStringWidth + 2), (9 + elevateDistBy), 0.0F).setColor(point.red, point.green, point.blue, 0.15F * fade);
                    vertexBuffer.addVertex(matrixStack, (halfDistStringWidth + 2), (-2 + elevateDistBy), 0.0F).setColor(point.red, point.green, point.blue, 0.15F * fade);
                    BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
                    OpenGL.glPolygonOffset(1.0F, 9.0F);
                    vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                    vertexBuffer.addVertex(matrixStack, (-halfDistStringWidth - 1), (-1 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                    vertexBuffer.addVertex(matrixStack, (-halfDistStringWidth - 1), (8 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                    vertexBuffer.addVertex(matrixStack, (halfDistStringWidth + 1), (8 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                    vertexBuffer.addVertex(matrixStack, (halfDistStringWidth + 1), (-1 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                    BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
                    matrixStack.popMatrix();
                }
            }

            OpenGL.glDisable(OpenGL.GL11_GL_POLYGON_OFFSET_FILL);
            OpenGL.glDepthMask(false);
            MultiBufferSource.BufferSource vertexConsumerProvider = VoxelConstants.getMinecraft().renderBuffers().bufferSource();
            if (withoutDepth) {
                int textColor = (int) (255.0F * fade) << 24 | 13421772;
                OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
                fontRenderer.drawInBatch(Component.literal(name), (-fontRenderer.width(name) / 2f), elevateBy, textColor, false, matrixStack, vertexConsumerProvider, DisplayMode.SEE_THROUGH, 0, 15728880);
                if (this.options.waypointDistanceBelowName) {
                    matrixStack.scale(distTextScale);
                    fontRenderer.drawInBatch(Component.literal(distStr), (-fontRenderer.width(distStr) / 2f), elevateDistBy, textColor, false, matrixStack, vertexConsumerProvider, DisplayMode.SEE_THROUGH, 0, 15728880);
                    matrixStack.scale(1.0F / distTextScale);
                }
                vertexConsumerProvider.endBatch();
            }

            OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        }

        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        matrixStack.popMatrix();
    }
}
