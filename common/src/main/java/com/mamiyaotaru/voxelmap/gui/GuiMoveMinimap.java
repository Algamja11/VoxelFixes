package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;

import com.mamiyaotaru.voxelmap.util.OpenGL;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.network.chat.Component;

public class GuiMoveMinimap extends GuiScreenMinimap {
    private final Screen parent;
    private final MapSettingsManager options;
    protected Component screenTitle;
    private double scScale;
    private boolean dragging;
    private int handleLeft;
    private int handleRight;
    private int handleTop;
    private int handleBottom;
    private float mapTargetX;
    private float mapTargetY;

    public GuiMoveMinimap(Screen parent) {
        this.parent = parent;
        this.options = VoxelConstants.getVoxelMapInstance().getMapOptions();
    }

    public void init() {
        this.screenTitle = Component.translatable("options.minimap.moveminimap");

        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.done"), button -> {
            this.options.mapCorner = -1;
            this.options.saveAll();
            VoxelConstants.getMinecraft().setScreen(this.parent);
        }).bounds(this.getWidth() / 2 - 100, this.getHeight() - 32, 200, 20).build());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double scaleProj = this.scScale / VoxelConstants.getMinecraft().getWindow().getGuiScale();
        this.mapTargetX = this.fromGuiPosition(this.options.mapX, 0, scaleProj);
        this.mapTargetY = this.fromGuiPosition(this.options.mapY, 1, scaleProj);
        this.dragging = mouseX >= this.handleLeft && mouseX <= this.handleRight && mouseY >= this.handleTop && mouseY <= this.handleBottom;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.dragging) {
            double scaleProj = this.scScale / VoxelConstants.getMinecraft().getWindow().getGuiScale();
            this.mapTargetX += deltaX;
            this.mapTargetY += deltaY;
            this.options.mapX = Math.max(0f, Math.min(320f, this.toGuiPosition(this.mapTargetX, 0, scaleProj)));
            this.options.mapY = Math.max(0f, Math.min(240f, this.toGuiPosition(this.mapTargetY, 1, scaleProj)));
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        int titleWidth = this.getFontRenderer().width(this.screenTitle);
        drawContext.fill(this.width / 2 - titleWidth / 2 - 5, 18, this.width / 2 + titleWidth / 2 + 5, 30, -1073741824);
        drawContext.drawCenteredString(this.getFontRenderer(), this.screenTitle, this.getWidth() / 2, 20, 16777215);

        int scScaleTemp = Math.min(VoxelConstants.getMinecraft().getWindow().getWidth() / 320, VoxelConstants.getMinecraft().getWindow().getHeight() / 240);
        this.scScale = Math.max(1, scScaleTemp) + this.options.sizeModifier;

        double scaleProj = this.scScale / VoxelConstants.getMinecraft().getWindow().getGuiScale();
        int handleSize = (int) (74.0 * scaleProj);
        this.handleLeft = (int) (this.fromGuiPosition(this.options.mapX, 0, scaleProj) - handleSize / 2f);
        this.handleRight = (int) (this.fromGuiPosition(this.options.mapX, 0, scaleProj) + handleSize / 2f);
        this.handleTop = (int) (this.fromGuiPosition(this.options.mapY, 1, scaleProj) - handleSize / 2f);
        this.handleBottom = (int) (this.fromGuiPosition(this.options.mapY, 1, scaleProj) + handleSize / 2f);

        OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        RenderSystem.setShader(CoreShaders.POSITION_COLOR);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder vertexBuffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        vertexBuffer.addVertex(this.handleLeft, this.handleBottom, 0).setUv(0.0F, 1.0F).setColor(0, 200, 255, 127);
        vertexBuffer.addVertex(this.handleRight, this.handleBottom, 0).setUv(1.0F, 1.0F).setColor(0, 200, 255, 127);
        vertexBuffer.addVertex(this.handleRight, this.handleTop, 0).setUv(1.0F, 0.0F).setColor(0, 200, 255, 127);
        vertexBuffer.addVertex(this.handleLeft, this.handleTop, 0).setUv(0.0F, 0.0F).setColor(0, 200, 255, 127);
        BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
        vertexBuffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        vertexBuffer.addVertex(this.handleLeft, this.handleBottom, 0).setColor(0, 200, 255, 255);
        vertexBuffer.addVertex(this.handleRight, this.handleBottom, 0).setColor(0, 200, 255, 255);
        vertexBuffer.addVertex(this.handleRight, this.handleTop, 0).setColor(0, 200, 255, 255);
        vertexBuffer.addVertex(this.handleLeft, this.handleTop, 0).setColor(0, 200, 255, 255);
        vertexBuffer.addVertex(this.handleLeft, this.handleBottom, 0).setColor(0, 200, 255, 255);
        BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
        OpenGL.glDisable(OpenGL.GL11_GL_BLEND);

        super.render(drawContext, mouseX, mouseY, delta);
    }

    private float toGuiPosition(float input, int axis, double scaleProj) {
        int border = (int) (37.0 * scaleProj);
        if (axis == 0) {
            return ((input - border) * 320f) / (this.getWidth() - border * 2f);
        } else {
            return ((input - border) * 240f) / (this.getHeight() - border * 2f);
        }
    }

    private float fromGuiPosition(float input, int axis, double scaleProj) {
        int border = (int) (37.0 * scaleProj);
        if (axis == 0) {
            return border + (input / 320f * (this.getWidth() - border * 2f));
        } else {
            return border + (input / 240f * (this.getHeight() - border * 2f));
        }
    }
}
