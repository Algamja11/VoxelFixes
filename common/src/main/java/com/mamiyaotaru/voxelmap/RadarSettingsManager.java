package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.ISubSettingsManager;
import com.mojang.serialization.DataResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class RadarSettingsManager implements ISubSettingsManager {
    private boolean somethingChanged;
    public boolean showRadar = true;
    public int radarMode = 2;
    public boolean showHostiles = true;
    public boolean showNeutrals;
    public boolean showMobNames = true;
    public boolean showHelmetsMobs = true;
    public boolean showPlayers = true;
    public boolean showPlayerNames = true;
    public boolean showHelmetsPlayers = true;
    public boolean showFacing = true;
    public boolean outlines = true;
    public boolean filtering = true;
    public float fontSize = 1.0F;

    public boolean radarAllowed = true;
    public boolean radarPlayersAllowed = true;
    public boolean radarMobsAllowed = true;
    public final HashSet<ResourceLocation> hiddenMobs = new HashSet<>();

    @Override
    public void loadAll(File settingsFile) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(settingsFile));

            String sCurrentLine;
            while ((sCurrentLine = in.readLine()) != null) {
                String[] curLine = sCurrentLine.split(":", 2);
                switch (curLine[0]) {
                    case "Show Radar" -> this.showRadar = Boolean.parseBoolean(curLine[1]);
                    case "Radar Mode" -> this.radarMode = Math.max(1, Math.min(2, Integer.parseInt(curLine[1])));
                    case "Show Hostiles" -> this.showHostiles = Boolean.parseBoolean(curLine[1]);
                    case "Show Neutrals" -> this.showNeutrals = Boolean.parseBoolean(curLine[1]);
                    case "Show Mob Names" -> this.showMobNames = Boolean.parseBoolean(curLine[1]);
                    case "Show Mob Helmets" -> this.showHelmetsMobs = Boolean.parseBoolean(curLine[1]);
                    case "Show Players" -> this.showPlayers = Boolean.parseBoolean(curLine[1]);
                    case "Show Player Names" -> this.showPlayerNames = Boolean.parseBoolean(curLine[1]);
                    case "Show Player Helmets" -> this.showHelmetsPlayers = Boolean.parseBoolean(curLine[1]);
                    case "Show Facing" -> this.showFacing = Boolean.parseBoolean(curLine[1]);
                    case "Filter Mob Icons" -> this.filtering = Boolean.parseBoolean(curLine[1]);
                    case "Outline Mob Icons" -> this.outlines = Boolean.parseBoolean(curLine[1]);
                    case "Radar Font Size" -> this.fontSize = Math.max(0.5F, Math.min(2.0F, Float.parseFloat(curLine[1])));
                    case "Hidden Mobs" -> this.applyHiddenMobSettings(curLine[1]);
                }
            }

            in.close();
        } catch (IOException | ArrayIndexOutOfBoundsException ignored) {
        }

    }

    @Override
    public void saveAll(PrintWriter out) {
        out.println("Show Radar:" + this.showRadar);
        out.println("Radar Mode:" + this.radarMode);
        out.println("Show Hostiles:" + this.showHostiles);
        out.println("Show Neutrals:" + this.showNeutrals);
        out.println("Show Mob Names:" + this.showMobNames);
        out.println("Show Mob Helmets:" + this.showHelmetsMobs);
        out.println("Show Players:" + this.showPlayers);
        out.println("Show Player Names:" + this.showPlayerNames);
        out.println("Show Player Helmets:" + this.showHelmetsPlayers);
        out.println("Show Facing:" + this.showFacing);
        out.println("Filter Mob Icons:" + this.filtering);
        out.println("Outline Mob Icons:" + this.outlines);
        out.println("Radar Font Size:" + this.fontSize);
        out.print("Hidden Mobs:");
        for (ResourceLocation mob : hiddenMobs) {
            out.print(mob.toString() + ",");
        }
        out.println();
    }

    @Override
    public String getKeyText(EnumOptionsMinimap option) {
        String name = I18n.get(option.getName()) + ": ";
        if (option.isBoolean()) {
            return this.getBooleanValue(option) ? name + I18n.get("options.on") : name + I18n.get("options.off");
        } else if (option.isList()) {
            String state = this.getListValue(option);
            return name + state;
        } else if (option.isFloat()) {
            float value = this.getFloatValue(option);
            return switch (option) {
                case RADAR_FONT_SIZE -> name + value + "x";
                default -> name + value;
            };
        } else {
            return name;
        }
    }

    @Override
    public boolean getBooleanValue(EnumOptionsMinimap option) {
        return switch (option) {
            case SHOW_RADAR -> this.showRadar;
            case SHOW_MOB_NAMES -> this.showMobNames;
            case SHOW_MOB_HELMETS -> this.showHelmetsMobs;
            case SHOW_PLAYERS -> this.showPlayers;
            case SHOW_PLAYER_NAMES -> this.showPlayerNames;
            case SHOW_PLAYER_HELMETS -> this.showHelmetsPlayers;
            case SHOW_FACING -> this.showFacing;
            case ICON_OUTLINES -> this.outlines;
            case ICON_FILTERING -> this.filtering;
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        };
    }

    @Override
    public String getListValue(EnumOptionsMinimap option) {
        switch (option) {
            case RADAR_MODE -> {
                if (this.radarMode == 1) {
                    return I18n.get("options.voxelmap.radar.radar_mode.simple");
                } else if (this.radarMode == 2) {
                    return I18n.get("options.voxelmap.radar.radar_mode.full");
                }

                return I18n.get("voxelmap.ui.error");
            }
            case SHOW_MOBS -> {
                if (this.showNeutrals && this.showHostiles) {
                    return I18n.get("options.voxelmap.radar.show_mobs.both");
                } else if (this.showNeutrals) {
                    return I18n.get("options.voxelmap.radar.show_mobs.neutrals");
                } else if (this.showHostiles) {
                    return I18n.get("options.voxelmap.radar.show_mobs.hostiles");
                }

                return I18n.get("options.off");
            }
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        }
    }

    @Override
    public float getFloatValue(EnumOptionsMinimap option) {
        return switch (option) {
            case RADAR_FONT_SIZE -> this.fontSize;

            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        };
    }

    @Override
    public void setValue(EnumOptionsMinimap option) {
        switch (option) {
            case SHOW_RADAR -> this.showRadar = !this.showRadar;
            case SHOW_MOB_NAMES -> this.showMobNames = !this.showMobNames;
            case SHOW_MOB_HELMETS -> this.showHelmetsMobs = !this.showHelmetsMobs;
            case SHOW_PLAYERS -> this.showPlayers = !this.showPlayers;
            case SHOW_PLAYER_NAMES -> this.showPlayerNames = !this.showPlayerNames;
            case SHOW_PLAYER_HELMETS -> this.showHelmetsPlayers = !this.showHelmetsPlayers;
            case SHOW_FACING -> this.showFacing = !this.showFacing;
            case ICON_OUTLINES -> this.outlines = !this.outlines;
            case ICON_FILTERING -> this.filtering = !this.filtering;
            case RADAR_MODE -> {
                if (this.radarMode == 2) {
                    this.radarMode = 1;
                } else {
                    this.radarMode = 2;
                }
            }
            case SHOW_MOBS -> {
                if (this.showNeutrals && this.showHostiles) {
                    this.showNeutrals = false;
                    this.showHostiles = false;
                } else if (this.showNeutrals) {
                    this.showNeutrals = false;
                    this.showHostiles = true;
                } else if (this.showHostiles) {
                    this.showNeutrals = true;
                } else {
                    this.showNeutrals = true;
                }
            }
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        }

        this.somethingChanged = true;
    }

    @Override
    public void setFloatValue(EnumOptionsMinimap option, float value) {
        switch (option) {
            case RADAR_FONT_SIZE -> {
                value = Math.round(value * 12.0F) / 12.0F;
                this.fontSize = (value * 1.5F) + 0.5F;
            }

            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + option.getName());
        }
    }

    private void applyHiddenMobSettings(String hiddenMobs) {
        String[] mobsToHide = hiddenMobs.split(",");

        this.hiddenMobs.clear();
        for (String s : mobsToHide) {
            DataResult<ResourceLocation> location = ResourceLocation.read(s);
            if (location.isSuccess()) {
                this.hiddenMobs.add(location.getOrThrow());
            }
        }
    }

    public boolean isMobEnabled(LivingEntity entity) {
        return isMobEnabled(entity.getType());
    }

    public boolean isMobEnabled(EntityType<?> type) {
        return !hiddenMobs.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    public boolean isChanged() {
        if (this.somethingChanged) {
            this.somethingChanged = false;
            return true;
        } else {
            return false;
        }
    }
}
