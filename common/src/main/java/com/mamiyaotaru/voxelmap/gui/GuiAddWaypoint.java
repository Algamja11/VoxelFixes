package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.ColorManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import org.lwjgl.glfw.GLFW;

public class GuiAddWaypoint extends GuiScreenMinimap {
    private static final ResourceLocation BLANK = ResourceLocation.parse("textures/misc/white.png");
    private static final ResourceLocation PICKER = ResourceLocation.parse("voxelmap:images/colorpicker.png");
    private static final ResourceLocation TARGET = ResourceLocation.parse("voxelmap:images/waypoints/target.png");
    final WaypointManager waypointManager;
    final ColorManager colorManager;
    private final IGuiWaypoints parentGui;
    private Button doneButton;
    private GuiSlotDimensions dimensionList;
    protected DimensionContainer selectedDimension;
    private Component tooltip;
    private EditBox waypointName;
    private EditBox waypointX;
    private EditBox waypointY;
    private EditBox waypointZ;
    private Button buttonEnabled;
    protected final Waypoint waypoint;
    private boolean choosingColor;
    private boolean choosingIcon;
    private final float red;
    private final float green;
    private final float blue;
    private final String suffix;
    private final boolean enabled;
    private final boolean editing;
    private final int playerX;
    private final int playerY;
    private final int playerZ;

    public GuiAddWaypoint(IGuiWaypoints par1GuiScreen, Waypoint par2Waypoint, boolean editing) {
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        this.colorManager = VoxelConstants.getVoxelMapInstance().getColorManager();
        this.parentGui = par1GuiScreen;
        this.waypoint = par2Waypoint;
        this.red = this.waypoint.red;
        this.green = this.waypoint.green;
        this.blue = this.waypoint.blue;
        this.suffix = this.waypoint.imageSuffix;
        this.enabled = this.waypoint.enabled;
        this.editing = editing;
        this.parentScreen = (Screen) par1GuiScreen;
        this.playerX = VoxelConstants.getPlayer().getBlockX();
        this.playerY = VoxelConstants.getPlayer().getBlockY();
        this.playerZ = VoxelConstants.getPlayer().getBlockZ();
    }

    public void init() {
        this.clearWidgets();
        this.waypointName = new EditBox(this.getFontRenderer(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 13, 200, 20, null);
        this.waypointName.setValue(this.waypoint.name);
        this.waypointX = new EditBox(this.getFontRenderer(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointX.setMaxLength(128);
        this.waypointX.setHint(Component.literal(String.valueOf(playerX)).withStyle(ChatFormatting.DARK_GRAY));
        this.waypointX.setValue(String.valueOf(this.waypoint.getX()));
        this.waypointY = new EditBox(this.getFontRenderer(), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointY.setMaxLength(128);
        this.waypointY.setHint(Component.literal(String.valueOf(playerY)).withStyle(ChatFormatting.DARK_GRAY));
        this.waypointY.setValue(String.valueOf(this.waypoint.getY()));
        this.waypointZ = new EditBox(this.getFontRenderer(), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointZ.setMaxLength(128);
        this.waypointZ.setHint(Component.literal(String.valueOf(playerZ)).withStyle(ChatFormatting.DARK_GRAY));
        this.waypointZ.setValue(String.valueOf(this.waypoint.getZ()));
        this.addRenderableWidget(this.waypointName);
        this.addRenderableWidget(this.waypointX);
        this.addRenderableWidget(this.waypointY);
        this.addRenderableWidget(this.waypointZ);
        int buttonListY = this.getHeight() / 6 + 82 + 6;
        this.addRenderableWidget(this.buttonEnabled = new Button.Builder(Component.literal("Enabled: " + (this.waypoint.enabled ? "On" : "Off")), button -> this.waypoint.enabled = !this.waypoint.enabled).bounds(this.getWidth() / 2 - 101, buttonListY, 100, 20).build());
        this.addRenderableWidget(new Button.Builder(Component.literal(I18n.get("voxelmap.waypoints.sort.color") + ":     "), button -> this.choosingColor = true).bounds(this.getWidth() / 2 - 101, buttonListY + 24, 100, 20).build());
        this.addRenderableWidget(new Button.Builder(Component.literal(I18n.get("voxelmap.waypoints.sort.icon") + ":     "), button -> this.choosingIcon = true).bounds(this.getWidth() / 2 - 101, buttonListY + 48, 100, 20).build());
        this.doneButton = new Button.Builder(Component.translatable("addServer.add"), button -> this.acceptWaypoint()).bounds(this.getWidth() / 2 - 155, this.getHeight() / 6 + 168, 150, 20).build();
        this.addRenderableWidget(this.doneButton);
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.cancel"), button -> this.cancelWaypoint()).bounds(this.getWidth() / 2 + 5, this.getHeight() / 6 + 168, 150, 20).build());
        this.doneButton.active = !this.waypointName.getValue().isEmpty();
        this.setFocused(this.waypointName);
        this.waypointName.setFocused(true);
        this.dimensionList = new GuiSlotDimensions(this);
        this.addRenderableWidget(dimensionList);
    }

    protected void cancelWaypoint() {
        waypoint.red = red;
        waypoint.green = green;
        waypoint.blue = blue;
        waypoint.imageSuffix = suffix;
        waypoint.enabled = enabled;

        if (parentGui != null) {
            parentGui.accept(false);
            return;
        }

        VoxelConstants.getMinecraft().setScreen(parentScreen);
    }

    protected void acceptWaypoint() {
        waypoint.name = waypointName.getValue();
        waypoint.setX(parseOrDefault(waypointX.getValue(), playerX));
        waypoint.setY(parseOrDefault(waypointY.getValue(), playerY));
        waypoint.setZ(parseOrDefault(waypointZ.getValue(), playerZ));

        if (parentGui != null) {
            parentGui.accept(true);

            return;
        }

        if (editing) {
            waypointManager.saveWaypoints();
            VoxelConstants.getMinecraft().setScreen(parentScreen);

            return;
        }

        waypointManager.addWaypoint(waypoint);
        VoxelConstants.getMinecraft().setScreen(parentScreen);
    }

    private int parseOrDefault(String string, int defaultValue) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.choosingColor || this.choosingIcon) {
            return false;
        }

        boolean keyPressed = super.keyPressed(keyCode, scanCode, modifiers);
        boolean acceptable = !this.waypointName.getValue().isEmpty();

        this.doneButton.active = acceptable;
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && acceptable) {
            this.acceptWaypoint();
        }

        return keyPressed;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.choosingColor || this.choosingIcon) {
            return false;
        }

        boolean charTyped = super.charTyped(chr, modifiers);
        boolean acceptable = !this.waypointName.getValue().isEmpty();

        this.doneButton.active = acceptable;

        return charTyped;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.choosingColor){
            int color = this.pickColor((int) mouseX, (int) mouseY, 200);
            if (color != -1) {
                this.waypoint.red = ARGB.redFloat(color);
                this.waypoint.green = ARGB.greenFloat(color);
                this.waypoint.blue = ARGB.blueFloat(color);
                this.choosingColor = false;
            }
            return false;
        } else if (this.choosingIcon){
            Sprite pickedIcon = this.pickIcon((int) mouseX, (int) mouseY);
            if (pickedIcon != null) {
                this.waypoint.imageSuffix = pickedIcon.getIconName().toString().replace("voxelmap:images/waypoints/waypoint", "").replace(".png", "");
                this.choosingIcon = false;
            }
            return false;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        this.tooltip = null;
        this.buttonEnabled.setMessage(Component.literal(I18n.get("voxelmap.waypoints.enabled") + ": " + (this.waypoint.enabled ? I18n.get("options.on") : I18n.get("options.off"))));

        this.renderDefaultBackground(drawContext);
        drawContext.drawCenteredString(this.getFontRenderer(), (this.parentGui == null || !this.parentGui.isEditing()) && !this.editing ? I18n.get("voxelmap.waypoints.new") : I18n.get("voxelmap.waypoints.edit"), this.getWidth() / 2, 20, 16777215);
        drawContext.drawString(this.getFontRenderer(), I18n.get("voxelmap.waypoints.name"), this.getWidth() / 2 - 100, this.getHeight() / 6, 16777215);
        drawContext.drawString(this.getFontRenderer(), I18n.get("X"), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41, 16777215);
        drawContext.drawString(this.getFontRenderer(), I18n.get("Y"), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41, 16777215);
        drawContext.drawString(this.getFontRenderer(), I18n.get("Z"), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41, 16777215);
        super.render(drawContext, this.choosingColor || this.choosingIcon ? 0 : mouseX, this.choosingColor || this.choosingIcon ? 0 : mouseY, delta);

        int buttonListY = this.getHeight() / 6 + 88;
        int color = this.waypoint.getUnifiedColor();
        drawContext.blit(RenderType::guiTextured, BLANK, this.getWidth() / 2 - 25, buttonListY + 24 + 5, 0, 0, 16, 10, 16, 10, color);
        Sprite waypointSprite = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas().getAtlasSprite("voxelmap:images/waypoints/waypoint" + waypoint.imageSuffix + ".png");
        waypointSprite.blit(drawContext, GLUtils.GUI_TEXTURED_EQUAL_DEPTH, this.getWidth() / 2 - 25, buttonListY + 48 + 2, 16, 16, color);
        drawContext.pose().translate(0, 0, 20);
        if (this.choosingColor || this.choosingIcon) {
            this.renderDefaultBackground(drawContext);

            if (this.choosingColor) {
                int pickerSize = 200;
                int pickerX = this.getWidth() / 2 - pickerSize / 2;
                int pickerY = this.getHeight() / 2 - pickerSize / 2;
                drawContext.blit(GLUtils.GUI_TEXTURED_EQUAL_DEPTH, PICKER, pickerX, pickerY, 0f, 0f, 200, 200, 200, 200);
                int pickedColor = this.pickColor(mouseX, mouseY, 200);
                if (pickedColor != -1) {
                    drawContext.blit(GLUtils.GUI_TEXTURED_EQUAL_DEPTH, TARGET, mouseX - 8, mouseY - 8, 0f, 0f, 16, 16, 16, 16);
                    drawContext.drawCenteredString(this.getFontRenderer(), "R: " + ARGB.red(pickedColor) + ", G: " + ARGB.green(pickedColor) + ", B: " + ARGB.blue(pickedColor), this.getWidth() / 2, this.getHeight() / 2 + pickerSize / 2 + 8, pickedColor);
                }
            } else if (this.choosingIcon) {
                TextureAtlas chooser = waypointManager.getTextureAtlasChooser();
                int chooserX = (int) (this.getWidth() / 2f - chooser.getWidth() / 2f);
                int chooserY = (int) (this.getHeight() / 2f - chooser.getHeight() / 2f);
                drawContext.blit(GLUtils.GUI_TEXTURED_EQUAL_DEPTH, WaypointManager.resourceTextureAtlasWaypointChooser, chooserX, chooserY, 0f, 0f, chooser.getWidth(), chooser.getHeight(), chooser.getWidth(), chooser.getHeight(), 0xBFFFFFFF);
                Sprite pickedIcon = this.pickIcon(mouseX, mouseY);
                if (pickedIcon != null) {
                    int iconX = pickedIcon.getOriginX() + chooserX;
                    int iconY = pickedIcon.getOriginY() + chooserY;
                    pickedIcon.blit(drawContext, GLUtils.GUI_TEXTURED_EQUAL_DEPTH, iconX - 4, iconY - 4, 40, 40, color);
                    String iconName = pickedIcon.getIconName().toString().replace("voxelmap:images/waypoints/waypoint", "").replace(".png", "");
                    if (iconName.isEmpty()) {
                        iconName = "Waypoint";
                    } else {
                        iconName = iconName.substring(0, 1).toUpperCase() + iconName.substring(1);
                    }
                    this.tooltip = Component.literal(iconName);
                }
            }
        }

        if (this.tooltip != null) {
            this.renderTooltip(drawContext, this.tooltip, mouseX, mouseY);
        }

    }

    private int pickColor(int mouseX, int mouseY, int size) {
        int pickerX = this.getWidth() / 2 - size / 2;
        int pickerY = this.getHeight() / 2 - size / 2;
        if (mouseX >= pickerX && mouseX <= pickerX + size && mouseY >= pickerY && mouseY <= pickerY + size) {
            int x = (int) ((float) (mouseX - pickerX) / size * 255.0F);
            int y = (int) ((float) (mouseY - pickerY) / size * 255.0F);
            return this.colorManager.getColorPicker().getRGB(x, y);
        }
        return -1;
    }

    private Sprite pickIcon(int mouseX, int mouseY) {
        TextureAtlas chooser = waypointManager.getTextureAtlasChooser();
        float chooserX = this.getWidth() / 2f - chooser.getWidth() / 2f;
        float chooserY = this.getHeight() / 2f - chooser.getHeight() / 2f;
        Sprite icon = chooser.getIconAt(mouseX - chooserX, mouseY - chooserY);
        if (icon != chooser.getMissingImage()) {
            return icon;
        }
        return null;
    }

    public void setSelectedDimension(DimensionContainer dimension) {
        this.selectedDimension = dimension;
    }

    public void toggleDimensionSelected() {
        if (this.waypoint.dimensions.size() > 1 && this.waypoint.dimensions.contains(this.selectedDimension) && this.selectedDimension != VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(VoxelConstants.getPlayer().level())) {
            this.waypoint.dimensions.remove(this.selectedDimension);
        } else {
            this.waypoint.dimensions.add(this.selectedDimension);
        }

    }

    static void setTooltip(GuiAddWaypoint par0GuiWaypoint, Component par1Str) {
        par0GuiWaypoint.tooltip = par1Str;
    }
}
