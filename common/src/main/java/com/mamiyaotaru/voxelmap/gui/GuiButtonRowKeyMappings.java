package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;

public class GuiButtonRowKeyMappings extends AbstractSelectionList<GuiButtonRowKeyMappings.RowItem> {
    private final MapSettingsManager options;
    private final GuiMinimapControls parentGui;
    private KeyMapping keyForEdit;
    private final ArrayList<KeyMapping> duplicateKeys = new ArrayList<>();

    public GuiButtonRowKeyMappings(GuiMinimapControls parentScreen, MapSettingsManager options) {
        super(VoxelConstants.getMinecraft(), parentScreen.getWidth(), parentScreen.getHeight() - 114, 40, 28);
        this.parentGui = parentScreen;
        this.options = options;
        ArrayList<RowItem> keyMappings = new ArrayList<>();
        for (int i = 0; i < this.options.keyBindings.length; i++) {
            keyMappings.add(new RowItem(this.parentGui, this.buildButton(i, false), this.buildButton(i, true), this.options.keyBindings[i]));
        }
        keyMappings.sort(Comparator.comparing(entry -> entry.keyMapping));
        this.checkDuplicateKeys();
        keyMappings.forEach(this::addEntry);
    }

    private Button buildButton(int index, boolean resetButton) {
        if (!resetButton) {
            return new Button.Builder(Component.literal(""), button -> this.keyForEdit = this.options.keyBindings[index]).bounds(0, 0, 75, 20).build();
        } else {
            return new Button.Builder(Component.literal(I18n.get("controls.reset")), button -> this.resetKeyMapping(index)).bounds(0, 0, 50, 20).build();
        }
    }

    public boolean keyEditing() {
        return this.keyForEdit != null;
    }

    private void resetKeyMapping(int index) {
        KeyMapping key = this.options.keyBindings[index];
        this.options.setKeyBinding(key, key.getDefaultKey());
        this.checkDuplicateKeys();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.keyEditing()) {
            this.options.setKeyBinding(keyForEdit, InputConstants.Type.MOUSE.getOrCreate(button));
            this.keyForEdit = null;
            this.checkDuplicateKeys();
            KeyMapping.resetMapping();
            return true;
        } else {
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.keyEditing()) {
            boolean isMenuKey = this.keyForEdit.same(this.options.keyBindMenu);
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (!isMenuKey) {
                    this.options.setKeyBinding(keyForEdit, InputConstants.UNKNOWN);
                }
            } else {
                this.options.setKeyBinding(keyForEdit, InputConstants.getKey(keyCode, scanCode));
            }
            this.keyForEdit = null;
            this.checkDuplicateKeys();
            KeyMapping.resetMapping();
            return true;
        } else {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    private void checkDuplicateKeys() {
        this.duplicateKeys.clear();
        for (KeyMapping key : this.options.keyBindings) {
            for (KeyMapping compareKey : VoxelConstants.getMinecraft().options.keyMappings) {
                if (key != compareKey && key.same(compareKey)) {
                    this.duplicateKeys.add(key);
                }
            }
        }
    }

    @Override
    public int getRowWidth() {
        return 300;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    public class RowItem extends AbstractSelectionList.Entry<RowItem> {
        private final GuiMinimapControls parentGui;
        private final Button button;
        private final Button buttonReset;
        private final KeyMapping keyMapping;

        protected RowItem(GuiMinimapControls parentScreen, Button button, Button buttonReset, KeyMapping keyMapping) {
            this.parentGui = parentScreen;
            this.button = button;
            this.buttonReset = buttonReset;
            this.keyMapping = keyMapping;
        }

        @Override
        public void render(GuiGraphics drawContext, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovered, float ticks) {
            if (this.button != null && this.buttonReset != null) {
                drawContext.drawString(this.parentGui.getFontRenderer(), I18n.get(this.keyMapping.getName()), x + 80, y + 9, 16777215);

                this.button.setX(x);
                this.button.setY(y + 2);
                MutableComponent keyText = this.keyMapping.getTranslatedKeyMessage().copy();
                if (GuiButtonRowKeyMappings.this.keyForEdit != null && GuiButtonRowKeyMappings.this.keyForEdit == this.keyMapping) {
                    keyText = Component.literal("> ").withStyle(ChatFormatting.YELLOW).append(keyText.copy()).append(" <");
                } else if (GuiButtonRowKeyMappings.this.duplicateKeys.contains(this.keyMapping)) {
                    keyText.withStyle(ChatFormatting.RED);
                }
                this.button.setMessage(keyText);
                this.button.render(drawContext, mouseX, mouseY, ticks);

                this.buttonReset.setX(x + width - 50);
                this.buttonReset.setY(y + 2);
                this.buttonReset.render(drawContext, mouseX, mouseY, ticks);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            GuiButtonRowKeyMappings.this.setSelected(this);
            boolean clicked = false;
            if (this.button != null && this.button.mouseClicked(mouseX, mouseY, button)) {
                clicked = true;
            } else if (this.buttonReset != null && this.buttonReset.mouseClicked(mouseX, mouseY, button)) {
                clicked = true;
            }
            return clicked;
        }
    }

}
