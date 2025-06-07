package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class GuiSubworldsSelect extends GuiScreenMinimap implements BooleanConsumer {
    private Component title;
    private Component select;
    private boolean multiworld;
    private EditBox newNameField;
    private boolean newWorld;
    private GuiSubworldList subworldList;
    private float yaw;
    private final CameraType lastCameraType;
    private final Screen parent;
    final LocalPlayer player;
    final LocalPlayer camera;
    private final WaypointManager waypointManager;

    public GuiSubworldsSelect(Screen parent) {
        ClientLevel clientWorld = VoxelConstants.getClientWorld();

        this.parent = parent;
        this.player = VoxelConstants.getPlayer();
        this.camera = new LocalPlayer(VoxelConstants.getMinecraft(), clientWorld, VoxelConstants.getMinecraft().getConnection(), this.player.getStats(), new ClientRecipeBook(), false, false);
        this.camera.input = new KeyboardInput(VoxelConstants.getMinecraft().options);
        this.camera.moveOrInterpolateTo(new Vec3(this.player.getX(), this.player.getY() + 0.35, this.player.getZ()), this.player.getYRot(), 0.0F);
        this.yaw = this.player.getYRot();
        this.lastCameraType = VoxelConstants.getMinecraft().options.getCameraType();
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
    }

    public void init() {
        if (!this.multiworld && !this.waypointManager.isMultiworld() && !VoxelConstants.isRealmServer()) {
            ConfirmScreen confirmScreen = new ConfirmScreen(this, Component.translatable("voxelmap.worldmap.multiworld.ismultiworld"), Component.translatable("voxelmap.worldmap.multiworld.explanation"), Component.translatable("gui.yes"), Component.translatable("gui.no"));
            VoxelConstants.getMinecraft().setScreen(confirmScreen);
        } else {
            VoxelConstants.getMinecraft().options.setCameraType(CameraType.FIRST_PERSON);
            VoxelConstants.getMinecraft().setCameraEntity(this.camera);
        }

        this.title = Component.translatable("voxelmap.worldmap.multiworld.title");
        this.select = Component.translatable("voxelmap.worldmap.multiworld.select");
        this.clearWidgets();

        if (!this.newWorld) {
            Button newNameBtn = new Button.Builder(Component.literal("< " + I18n.get("voxelmap.worldmap.multiworld.new_name") + " >"), button -> {
                this.newWorld = true;
                this.newNameField.setFocused(true);
            }).bounds(10, this.getHeight() - 80, 150, 20).build();
            this.addRenderableWidget(newNameBtn);
        }

        this.newNameField = new EditBox(this.getFont(), 11, this.getHeight() - 79, 148, 18, null);

        this.subworldList = new GuiSubworldList(this);

        Button selectBtn = new Button.Builder(Component.translatable("mco.template.button.select"), button -> {
            if (this.subworldList.getSelected() != null) {
                this.selectWorld(this.subworldList.getSelected().getSubworldName());
            }
        }).bounds(10, this.getHeight() - 60, 75, 20).build();
        this.addRenderableWidget(selectBtn);

        Button editBtn = new Button.Builder(Component.translatable("selectServer.edit"), button -> {
            if (this.subworldList.getSelected() != null) {
                this.editWorld(this.subworldList.getSelected().getSubworldName());
            }
        }).bounds(85, this.getHeight() - 60, 75, 20).build();
        this.addRenderableWidget(editBtn);

        Button cancelBtn = new Button.Builder(Component.translatable("gui.cancel"), button -> VoxelConstants.getMinecraft().setScreen(null)).bounds(this.getWidth() / 2 - 100, this.getHeight() - 30, 200, 20).build();
        this.addRenderableWidget(cancelBtn);
    }

    public void accept(boolean b) {
        if (!b) {
            VoxelConstants.getMinecraft().setScreen(this.parent);
        } else {
            this.multiworld = true;
            VoxelConstants.getMinecraft().setScreen(this);
        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.newWorld) {
            this.newNameField.mouseClicked(mouseX, mouseY, button);
        }

        this.subworldList.mouseClicked(mouseX, mouseY, button);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.subworldList.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        this.subworldList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        this.subworldList.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.newNameField.isFocused()) {
            this.newNameField.keyPressed(keyCode, scanCode, modifiers);
            if ((keyCode == 257 || keyCode == 335) && this.newNameField.isFocused()) {
                String newName = this.newNameField.getValue();
                if (newName != null && !newName.isEmpty()) {
                    this.selectWorld(newName);
                }
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        if (this.newNameField.isFocused()) {
            this.newNameField.charTyped(chr, modifiers);
            if (modifiers == 28) {
                String newName = this.newNameField.getValue();
                if (newName != null && !newName.isEmpty()) {
                    this.selectWorld(newName);
                }
            }
        }

        return super.charTyped(chr, modifiers);
    }

    public void tick() {
        super.tick();
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, mouseX, mouseY, delta);
        int titleStringWidth = this.getFont().width(this.title);
        titleStringWidth = Math.max(titleStringWidth, this.getFont().width(this.select));
        drawContext.fill(this.getWidth() / 2 - titleStringWidth / 2 - 5, 0, this.getWidth() / 2 + titleStringWidth / 2 + 5, 27, -1073741824);
        drawContext.drawCenteredString(this.getFont(), this.title, this.getWidth() / 2, 5, 16777215);
        drawContext.drawCenteredString(this.getFont(), this.select, this.getWidth() / 2, 15, 16711680);

        this.camera.xRotO = 0.0F;
        this.camera.setXRot(0.0F);
        this.camera.yRotO = this.yaw;
        this.camera.setYRot(this.yaw);
        float var4 = 0.475F;
        this.camera.yOld = this.camera.yo = this.player.getY();
        this.camera.xOld = this.camera.xo = this.player.getX() - var4 * Math.sin(this.yaw / 180.0 * Math.PI);
        this.camera.zOld = this.camera.zo = this.player.getZ() + var4 * Math.cos(this.yaw / 180.0 * Math.PI);
        this.camera.setPosRaw(this.camera.xo, this.camera.yo, this.camera.zo);
        float var5 = 5.0F * delta;
        this.yaw = (float) (this.yaw + var5 * (1.0 + 0.7F * Math.cos((this.yaw + 45.0F) / 45.0 * Math.PI)));

        this.subworldList.render(drawContext, mouseX, mouseY, delta);
        if (this.newWorld) {
            this.newNameField.render(drawContext, mouseX, mouseY, delta);
        }

    }

    @Override
    public void removed() {
        super.removed();
        VoxelConstants.getMinecraft().options.setCameraType(this.lastCameraType);
        VoxelConstants.getMinecraft().setCameraEntity(this.player);
    }

    public void selectWorld(String selectedSubworldName) {
        this.waypointManager.setSubworldName(selectedSubworldName, false);
        VoxelConstants.getMinecraft().setScreen(this.parent);
    }

    public void editWorld(String subworldNameToEdit) {
        VoxelConstants.getMinecraft().setScreen(new GuiSubworldEdit(this, subworldNameToEdit));
    }
}
