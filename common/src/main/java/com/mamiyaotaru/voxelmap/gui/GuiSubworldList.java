package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;

class GuiSubworldList extends AbstractSelectionList<GuiSubworldList.WorldItem> {
    private final GuiSubworldsSelect parentGui;
    private long lastClicked;
    private boolean doubleClicked;

    GuiSubworldList(GuiSubworldsSelect parentGui) {
        super(VoxelConstants.getMinecraft(), 150, parentGui.getHeight() - 130, 40, 18);
        this.parentGui = parentGui;
        this.setX(10);

        ArrayList<WorldItem> items = new ArrayList<>();
        for (String name : VoxelConstants.getVoxelMapInstance().getWaypointManager().getKnownSubworldNames()) {
            WorldItem item = new WorldItem(this.parentGui, name);
            items.add(item);
        }

        items.forEach(this::addEntry);
    }

    @Override
    public int getRowWidth() {
        return 120;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.doubleClicked = System.currentTimeMillis() - this.lastClicked < 200L;
        this.lastClicked = System.currentTimeMillis();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void setSelected(GuiSubworldList.WorldItem entry) {
        super.setSelected(entry);

        if (this.getSelected() instanceof GuiSubworldList.WorldItem) {
            GameNarrator narrator = new GameNarrator(VoxelConstants.getMinecraft());
            narrator.sayNow((Component.translatable("narrator.select", this.getSelected().subworldName)).getString());
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public class WorldItem extends Entry<WorldItem> {
        private final GuiSubworldsSelect parentGui;
        private final String subworldName;

        protected WorldItem(GuiSubworldsSelect parentGui, String subworldName) {
            this.parentGui = parentGui;
            this.subworldName = subworldName;
        }

        public void render(GuiGraphics drawContext, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int width = GuiSubworldList.this.getWidth();
            drawContext.drawCenteredString(parentGui.getFont(), subworldName, x + width / 2 - (width - entryWidth) / 2, y + 3, 0xFFFFFF);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseY < GuiSubworldList.this.getY() || mouseY > GuiSubworldList.this.getBottom()) {
                return false;
            }

            GuiSubworldList.this.setSelected(this);

            if (GuiSubworldList.this.doubleClicked) {
                parentGui.selectWorld(subworldName);
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        public String getSubworldName() {
            return subworldName;
        }
    }
}
