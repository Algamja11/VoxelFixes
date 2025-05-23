package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.ColorManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.gui.overridden.GuiScreenMinimap;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.DimensionContainer;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.GuiUtils;
import com.mamiyaotaru.voxelmap.util.Waypoint;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import org.lwjgl.glfw.GLFW;

import java.util.TreeSet;

public class GuiAddWaypoint extends GuiScreenMinimap {
    private static final ResourceLocation BLANK = ResourceLocation.parse("textures/misc/white.png");
    private static final ResourceLocation COLOR_PICKER = ResourceLocation.parse("voxelmap:images/color_picker.png");
    private static final ResourceLocation SUBWORLD_LIST = ResourceLocation.parse("voxelmap:images/icons/multiworld_list.png");
    private static final ResourceLocation DIMENSION_LIST = ResourceLocation.parse("voxelmap:images/icons/dimension_list.png");

    private final ColorManager colorManager;
    private final WaypointManager waypointManager;

    private final IGuiWaypoints parentGui;

    protected final Waypoint waypoint;
    private final float lastRed;
    private final float lastGreen;
    private final float lastBlue;
    private final String lastSuffix;
    private final boolean lastEnabled;
    private final String lastWorld;
    private final TreeSet<DimensionContainer> lastDimensions;
    private final int playerX;
    private final int playerY;
    private final int playerZ;

    private Button doneButton;
    private EditBox waypointName;
    private EditBox waypointX;
    private EditBox waypointY;
    private EditBox waypointZ;
    private Button buttonEnabled;
    private Component tooltip;
    private boolean choosingColor;
    private boolean choosingIcon;
    private final boolean editing;

    private AbstractSelectionList<?> selectedList;
    private GuiSlotSubworlds subworldList;
    protected String selectedSubworld;
    private GuiSlotDimensions dimensionList;
    protected DimensionContainer selectedDimension;

    public GuiAddWaypoint(IGuiWaypoints par1GuiScreen, Waypoint par2Waypoint, boolean editing) {
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
        this.colorManager = VoxelConstants.getVoxelMapInstance().getColorManager();
        this.parentGui = par1GuiScreen;
        this.waypoint = par2Waypoint;
        this.lastRed = this.waypoint.red;
        this.lastGreen = this.waypoint.green;
        this.lastBlue = this.waypoint.blue;
        this.lastSuffix = this.waypoint.imageSuffix;
        this.lastEnabled = this.waypoint.enabled;
        this.lastWorld = this.waypoint.world;
        this.lastDimensions = new TreeSet<>(this.waypoint.dimensions);
        this.editing = editing;
        this.parentScreen = (Screen) par1GuiScreen;
        this.playerX = VoxelConstants.getPlayer().getBlockX();
        this.playerY = VoxelConstants.getPlayer().getBlockY();
        this.playerZ = VoxelConstants.getPlayer().getBlockZ();
    }

    public void init() {
        this.clearWidgets();
        this.waypointName = new EditBox(this.getFont(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 13, 200, 20, null);
        this.waypointName.setValue(this.waypoint.name);
        this.waypointX = new EditBox(this.getFont(), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointX.setMaxLength(128);
        this.waypointX.setHint(Component.literal(String.valueOf(playerX)).withStyle(ChatFormatting.DARK_GRAY));
        this.waypointX.setValue(String.valueOf(this.waypoint.getX()));
        this.waypointY = new EditBox(this.getFont(), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointY.setMaxLength(128);
        this.waypointY.setHint(Component.literal(String.valueOf(playerY)).withStyle(ChatFormatting.DARK_GRAY));
        this.waypointY.setValue(String.valueOf(this.waypoint.getY()));
        this.waypointZ = new EditBox(this.getFont(), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41 + 13, 56, 20, null);
        this.waypointZ.setMaxLength(128);
        this.waypointZ.setHint(Component.literal(String.valueOf(playerZ)).withStyle(ChatFormatting.DARK_GRAY));
        this.waypointZ.setValue(String.valueOf(this.waypoint.getZ()));
        this.addRenderableWidget(this.waypointName);
        this.addRenderableWidget(this.waypointX);
        this.addRenderableWidget(this.waypointY);
        this.addRenderableWidget(this.waypointZ);
        int buttonListY = this.getHeight() / 6 + 82 + 6;
        this.buttonEnabled = new Button.Builder(Component.literal(I18n.get("voxelmap.waypoints.enabled") + ": " + (this.waypoint.enabled ? I18n.get("options.on") : I18n.get("options.off"))), button -> this.waypoint.enabled = !this.waypoint.enabled).bounds(this.getWidth() / 2 - 101, buttonListY, 100, 20).build();
        this.addRenderableWidget(this.buttonEnabled);
        this.addRenderableWidget(new Button.Builder(Component.literal(I18n.get("voxelmap.waypoints.sort.color") + ":     "), button -> this.choosingColor = true).bounds(this.getWidth() / 2 - 101, buttonListY + 24, 100, 20).build());
        this.addRenderableWidget(new Button.Builder(Component.literal(I18n.get("voxelmap.waypoints.sort.icon") + ":     "), button -> this.choosingIcon = true).bounds(this.getWidth() / 2 - 101, buttonListY + 48, 100, 20).build());
        this.doneButton = new Button.Builder(Component.translatable("addServer.add"), button -> this.acceptWaypoint()).bounds(this.getWidth() / 2 - 155, this.getHeight() / 6 + 168, 150, 20).build();
        this.addRenderableWidget(this.doneButton);
        this.addRenderableWidget(new Button.Builder(Component.translatable("gui.cancel"), button -> this.cancelWaypoint()).bounds(this.getWidth() / 2 + 5, this.getHeight() / 6 + 168, 150, 20).build());
        this.doneButton.active = this.isAcceptable();
        this.setFocused(this.waypointName);
        this.waypointName.setFocused(true);
        this.dimensionList = new GuiSlotDimensions(this);
        this.selectedList = this.dimensionList;
        if (waypointManager.isMultiworld()) {
            this.subworldList = new GuiSlotSubworlds(this);
            this.selectedSubworld = this.waypoint.world;
            this.addRenderableWidget(new Button.Builder(Component.empty(), button -> this.selectedList = this.dimensionList).bounds(this.getWidth() / 2 + 102, buttonListY, 20, 20).build());
            this.addRenderableWidget(new Button.Builder(Component.empty(), button -> this.selectedList = this.subworldList).bounds(this.getWidth() / 2 + 102, buttonListY + 24, 20, 20).build());
        }
    }

    protected void cancelWaypoint() {
        waypoint.red = lastRed;
        waypoint.green = lastGreen;
        waypoint.blue = lastBlue;
        waypoint.imageSuffix = lastSuffix;
        waypoint.enabled = lastEnabled;
        waypoint.world = lastWorld;
        waypoint.dimensions.clear();
        waypoint.dimensions.addAll(lastDimensions);

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
        boolean acceptable = this.isAcceptable();

        this.doneButton.active = acceptable;
        if (acceptable && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            this.acceptWaypoint();
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.cancelWaypoint();
        }

        return keyPressed;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.choosingColor || this.choosingIcon) {
            return false;
        }

        boolean charTyped = super.charTyped(chr, modifiers);

        this.doneButton.active = this.isAcceptable();

        return charTyped;
    }

    private boolean isAcceptable() {
        if (this.waypointName.getValue().isEmpty()) {
            return false;
        }

        try {
            String xString = this.waypointX.getValue();
            String yString = this.waypointY.getValue();
            String zString = this.waypointZ.getValue();
            if (!xString.isEmpty()) Integer.parseInt(xString);
            if (!yString.isEmpty()) Integer.parseInt(yString);
            if (!zString.isEmpty()) Integer.parseInt(zString);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
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

        this.selectedList.mouseClicked(mouseX, mouseY, button);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.selectedList.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        this.selectedList.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
        this.selectedList.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
    }

    @Override
    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        super.render(drawContext, this.choosingColor || this.choosingIcon ? 0 : mouseX, this.choosingColor || this.choosingIcon ? 0 : mouseY, delta);

        drawContext.drawCenteredString(this.getFont(), (this.parentGui == null || !this.parentGui.isEditing()) && !this.editing ? I18n.get("voxelmap.waypoints.new") : I18n.get("voxelmap.waypoints.edit"), this.getWidth() / 2, 20, 16777215);
        drawContext.drawString(this.getFont(), I18n.get("voxelmap.waypoints.name"), this.getWidth() / 2 - 100, this.getHeight() / 6, 16777215);
        drawContext.drawString(this.getFont(), I18n.get("X"), this.getWidth() / 2 - 100, this.getHeight() / 6 + 41, 16777215);
        drawContext.drawString(this.getFont(), I18n.get("Y"), this.getWidth() / 2 - 28, this.getHeight() / 6 + 41, 16777215);
        drawContext.drawString(this.getFont(), I18n.get("Z"), this.getWidth() / 2 + 44, this.getHeight() / 6 + 41, 16777215);

        this.tooltip = null;
        this.selectedList.render(drawContext, this.choosingColor || this.choosingIcon ? 0 : mouseX, this.choosingColor || this.choosingIcon ? 0 : mouseY, delta);
        this.buttonEnabled.setMessage(Component.literal(I18n.get("voxelmap.waypoints.enabled") + ": " + (this.waypoint.enabled ? I18n.get("options.on") : I18n.get("options.off"))));

        int buttonListY = this.getHeight() / 6 + 88;
        int color = this.waypoint.getUnifiedColor();
        drawContext.blit(RenderType::guiTextured, BLANK, this.getWidth() / 2 - 25, buttonListY + 24 + 5, 0, 0, 16, 10, 16, 10, color);
        Sprite waypointSprite = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas().getAtlasSprite("voxelmap:images/waypoints/waypoint" + waypoint.imageSuffix + ".png");
        waypointSprite.blit(drawContext, GLUtils.GUI_TEXTURED_EQUAL_DEPTH, this.getWidth() / 2 - 25, buttonListY + 48 + 2, 16, 16, color);
        if (waypointManager.isMultiworld()) {
            drawContext.blit(GLUtils.GUI_TEXTURED_EQUAL_DEPTH, DIMENSION_LIST, this.getWidth() / 2 + 102, buttonListY, 0.0F, 0.0F, 20, 20, 20, 20, 0xFFFFFFFF);
            drawContext.blit(GLUtils.GUI_TEXTURED_EQUAL_DEPTH, SUBWORLD_LIST, this.getWidth() / 2 + 102, buttonListY + 24, 0.0F, 0.0F, 20, 20, 20, 20, 0xFFFFFFFF);
        }
        drawContext.pose().translate(0, 0, 20);
        if (this.choosingColor || this.choosingIcon) {
            super.renderBackground(drawContext, mouseX, mouseY, delta);

            if (this.choosingColor) {
                int pickerSize = 200;
                int pickerX = this.getWidth() / 2 - pickerSize / 2;
                int pickerY = this.getHeight() / 2 - pickerSize / 2;
                drawContext.blit(GLUtils.GUI_TEXTURED_EQUAL_DEPTH, COLOR_PICKER, pickerX, pickerY, 0.0F, 0.0F, 200, 200, 200, 200);
                int pickedColor = this.pickColor(mouseX, mouseY, 200);
                if (pickedColor != -1) {
                    drawContext.drawCenteredString(this.getFont(), "R: " + ARGB.red(pickedColor) + ", G: " + ARGB.green(pickedColor) + ", B: " + ARGB.blue(pickedColor), this.getWidth() / 2, this.getHeight() / 2 + pickerSize / 2 + 8, pickedColor);
                }

                drawContext.drawCenteredString(this.getFont(), I18n.get("voxelmap.waypoints.choose_color"), this.getWidth() / 2, 20, 16777215);
            } else if (this.choosingIcon) {
                TextureAtlas chooser = waypointManager.getTextureAtlasChooser();
                int chooserX = this.getWidth() / 2 - chooser.getWidth() / 2;
                int chooserY = this.getHeight() / 2 - chooser.getHeight() / 2;
                drawContext.blit(GLUtils.GUI_TEXTURED_EQUAL_DEPTH, WaypointManager.resourceTextureAtlasWaypointChooser, chooserX, chooserY, 0f, 0f, chooser.getWidth(), chooser.getHeight(), chooser.getWidth(), chooser.getHeight(), 0xBFFFFFFF);
                Sprite pickedIcon = this.pickIcon(mouseX, mouseY);
                if (pickedIcon != null) {
                    int iconX = pickedIcon.getOriginX() + chooserX;
                    int iconY = pickedIcon.getOriginY() + chooserY;
                    pickedIcon.blit(drawContext, GLUtils.GUI_TEXTURED_EQUAL_DEPTH, iconX - 4, iconY - 4, 40, 40, color);

                    String suffix = pickedIcon.getIconName().toString().replace("voxelmap:images/waypoints/waypoint", "").replace(".png", "");
                    String translated = I18n.get("voxelmap.waypoints.icons." + suffix);
                    if (translated.equals("voxelmap.waypoints.icons." + suffix)) {
                        translated = suffix;
                    }
                    this.tooltip = Component.literal(translated);
                }

                drawContext.drawCenteredString(this.getFont(), I18n.get("voxelmap.waypoints.choose_icon"), this.getWidth() / 2, 20, 16777215);
            }
        }

        if (this.tooltip != null) {
            GuiUtils.renderTooltip(drawContext, this.tooltip, mouseX, mouseY);
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

    public void selectOrToggleSubworld(String world) {
        if (this.selectedSubworld.equals(world)) {
            this.selectedSubworld = "";
        } else {
            this.selectedSubworld = world;
        }
        this.waypoint.world = this.selectedSubworld;
    }

    static void setTooltip(GuiAddWaypoint par0GuiWaypoint, Component par1Str) {
        par0GuiWaypoint.tooltip = par1Str;
    }
}
