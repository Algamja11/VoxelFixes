package com.mamiyaotaru.voxelmap.gui;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;

class GuiSlotSubworlds extends AbstractSelectionList<GuiSlotSubworlds.WorldItem> {
    private static final ResourceLocation ENABLED_ICON = ResourceLocation.parse("textures/gui/sprites/container/beacon/confirm.png");
    private static final ResourceLocation DISABLED_ICON = ResourceLocation.parse("textures/gui/sprites/container/beacon/cancel.png");

    private final GuiAddWaypoint parentGui;

    GuiSlotSubworlds(GuiAddWaypoint parentGui) {
        super(VoxelConstants.getMinecraft(), 101, 64, parentGui.getHeight() / 6 + 90, 18);
        this.parentGui = parentGui;
        this.setX(this.parentGui.getWidth() / 2);

        ArrayList<WorldItem> items = new ArrayList<>();
        for (String name : VoxelConstants.getVoxelMapInstance().getWaypointManager().getKnownSubworldNames()) {
            WorldItem item = new WorldItem(this.parentGui, name);
            items.add(item);
        }

        items.forEach(this::addEntry);
    }

    @Override
    public int getRowWidth() {
        return 100;
    }

    @Override
    public void setSelected(GuiSlotSubworlds.WorldItem entry) {
        super.setSelected(entry);

        if (this.getSelected() instanceof GuiSlotSubworlds.WorldItem) {
            GameNarrator narrator = new GameNarrator(VoxelConstants.getMinecraft());
            narrator.sayNow((Component.translatable("narrator.select", this.getSelected().subworldName)).getString());
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }

    public class WorldItem extends Entry<WorldItem> {
        private final GuiAddWaypoint parentGui;
        private final String subworldName;

        protected WorldItem(GuiAddWaypoint parentGui, String subworldName) {
            this.parentGui = parentGui;
            this.subworldName = subworldName;
        }

        public void render(GuiGraphics drawContext, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int itemWidth = GuiSlotSubworlds.this.getWidth();

            drawContext.drawCenteredString(parentGui.getFont(), subworldName, x + itemWidth / 2, y + 3, 0xFFFFFF);

            int iconWidth = 18;
            drawContext.blit(RenderType::guiTextured, parentGui.waypoint.world.equals(subworldName) ? ENABLED_ICON : DISABLED_ICON, x + itemWidth - iconWidth - 4, y - 3, 0, 0, 18, 18, 18, 18);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (mouseY < GuiSlotSubworlds.this.getY() || mouseY > GuiSlotSubworlds.this.getBottom()) {
                return false;
            }

            GuiSlotSubworlds.this.setSelected(this);
            int iconWidth = 18;
            int rightEdge = GuiSlotSubworlds.this.getX() + GuiSlotSubworlds.this.getWidth();
            if (mouseX >= (rightEdge - iconWidth) && mouseX <= rightEdge) {
                parentGui.selectOrToggleSubworld(subworldName);
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }
    }
}
