package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.gui.overridden.EnumOptionsMinimap;
import com.mamiyaotaru.voxelmap.interfaces.ISubSettingsManager;
import com.mamiyaotaru.voxelmap.util.CustomMob;
import com.mamiyaotaru.voxelmap.util.CustomMobsManager;
import com.mamiyaotaru.voxelmap.util.EnumMobs;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import net.minecraft.client.resources.language.I18n;

public class RadarSettingsManager implements ISubSettingsManager {
    private boolean somethingChanged;
    public boolean showRadar = true;
    public int radarMode = 2;
    public boolean showNeutrals = false;
    public boolean showHostiles = true;
    public boolean showPlayers = true;
    public boolean showHelmetsMobs = true;
    public boolean showHelmetsPlayers = true;
    public boolean showMobNames = true;
    public boolean showPlayerNames = true;
    public boolean outlines = true;
    public boolean filtering = true;
    public boolean showFacing = true;

    public float fontSize = 0.25f;
    public boolean showNamesOnlyForTagged = true;

    public boolean radarAllowed = true;
    public boolean radarPlayersAllowed = true;
    public boolean radarMobsAllowed = true;

    @Override
    public void loadSettings(File settingsFile) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(settingsFile));

            String sCurrentLine;
            while ((sCurrentLine = in.readLine()) != null) {
                String[] curLine = sCurrentLine.split(":");
                switch (curLine[0]) {
                    case "Show Radar" -> this.showRadar = Boolean.parseBoolean(curLine[1]);
                    case "Radar Mode" -> this.radarMode = Math.max(1, Math.min(3, Integer.parseInt(curLine[1])));
                    case "Show Neutrals" -> this.showNeutrals = Boolean.parseBoolean(curLine[1]);
                    case "Show Hostiles" -> this.showHostiles = Boolean.parseBoolean(curLine[1]);
                    case "Show Players" -> this.showPlayers = Boolean.parseBoolean(curLine[1]);
                    case "Show Mob Helmets" -> this.showHelmetsMobs = Boolean.parseBoolean(curLine[1]);
                    case "Show Player Helmets" -> this.showHelmetsPlayers = Boolean.parseBoolean(curLine[1]);
                    case "Show Mob Names" -> this.showMobNames = Boolean.parseBoolean(curLine[1]);
                    case "Show Player Names" -> this.showPlayerNames = Boolean.parseBoolean(curLine[1]);
                    case "Filter Icons" -> this.filtering = Boolean.parseBoolean(curLine[1]);
                    case "Outline Icons" -> this.outlines = Boolean.parseBoolean(curLine[1]);
                    case "Show Facing" -> this.showFacing = Boolean.parseBoolean(curLine[1]);
                    case "Font Size" -> this.fontSize = Float.parseFloat(curLine[1]);
                    case "Show Names Only For Tagged Mobs" -> this.showNamesOnlyForTagged = Boolean.parseBoolean(curLine[1]);
                    case "Hidden Mobs" -> this.applyHiddenMobSettings(curLine[1]);
                }
            }

            in.close();
        } catch (IOException | ArrayIndexOutOfBoundsException ignored) {}

    }

    private void applyHiddenMobSettings(String hiddenMobs) {
        String[] mobsToHide = hiddenMobs.split(",");

        for (String s : mobsToHide) {
            boolean builtIn = false;

            for (EnumMobs mob : EnumMobs.values()) {
                if (mob.id.equals(s)) {
                    mob.enabled = false;
                    builtIn = true;
                }
            }

            if (!builtIn) {
                CustomMobsManager.add(s, false);
            }
        }

    }

    @Override
    public void saveAll(PrintWriter out) {
        out.println("Show Radar:" + this.showRadar);
        out.println("Radar Mode:" + this.radarMode);
        out.println("Show Neutrals:" + this.showNeutrals);
        out.println("Show Hostiles:" + this.showHostiles);
        out.println("Show Players:" + this.showPlayers);
        out.println("Show Mob Helmets:" + this.showHelmetsMobs);
        out.println("Show Player Helmets:" + this.showHelmetsPlayers);
        out.println("Show Mob Names:" + this.showMobNames);
        out.println("Show Player Names:" + this.showPlayerNames);
        out.println("Filter Icons:" + this.filtering);
        out.println("Outline Icons:" + this.outlines);
        out.println("Show Facing:" + this.showFacing);
        out.println("Font Size:" + this.fontSize);
        out.println("Show Names Only For Tagged Mobs" + this.showNamesOnlyForTagged);
        out.print("Hidden Mobs:");
        for (EnumMobs mob : EnumMobs.values()) {
            if (!mob.enabled) {
                out.print(mob.id + ",");
            }
        }
        for (CustomMob mob : CustomMobsManager.mobs) {
            if (!mob.enabled) {
                out.print(mob.id + ",");
            }
        }

        out.println();
    }

    @Override
    public String getKeyText(EnumOptionsMinimap options) {
        String s = I18n.get(options.getName()) + ": ";
        if (options.isBoolean()) {
            return this.getOptionBooleanValue(options) ? s + I18n.get("options.on") : s + I18n.get("options.off");
        } else if (options.isList()) {
            String state = this.getOptionListValue(options);
            return s + state;
        } else {
            return s + (0.25f + (getOptionFloatValue(options) * 0.25f)) + "x";
        }
    }

    public boolean getOptionBooleanValue(EnumOptionsMinimap par1EnumOptions) {
        return switch (par1EnumOptions) {
            case SHOWRADAR -> this.showRadar;
            case SHOWPLAYERS -> this.showPlayers;
            case SHOWMOBHELMETS -> this.showHelmetsMobs;
            case SHOWPLAYERHELMETS -> this.showHelmetsPlayers;
            case SHOWMOBNAMES -> this.showMobNames;
            case SHOWPLAYERNAMES -> this.showPlayerNames;
            case RADAROUTLINE -> this.outlines;
            case RADARFILTERING -> this.filtering;
            case SHOWFACING -> this.showFacing;
            case SHOWNAMESONLYFORTAGGED -> this.showNamesOnlyForTagged;
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName() + ". (possibly not a boolean)");
        };
    }

    public String getOptionListValue(EnumOptionsMinimap par1EnumOptions) {
        switch (par1EnumOptions) {
            case RADARMODE -> {
                if (this.radarMode == 1) {
                    return I18n.get("options.minimap.radar.radarmode.simple");
                } else if (this.radarMode == 2) {
                    return I18n.get("options.minimap.radar.radarmode.icon");
                } else {
                    if (this.radarMode == 3) {
                        return I18n.get("options.minimap.radar.radarmode.dynamic");
                    }
                    return "error";
                }
            }
            case SHOWMOBS -> {
                if (this.showNeutrals && this.showHostiles) {
                    return I18n.get("options.minimap.radar.showmobs.both");
                } else if (this.showNeutrals) {
                    return I18n.get("options.minimap.radar.showmobs.neutrals");
                } else if (this.showHostiles) {
                    return I18n.get("options.minimap.radar.showmobs.hostiles");
                } else {
                    return I18n.get("options.off");
                }
            }
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName() + ". (possibly not a list value applicable to minimap)");
        }
    }

    @Override
    public void setOptionFloatValue(EnumOptionsMinimap options, float value) {
        if (options == EnumOptionsMinimap.RADARFONTSIZE) {
            float tempVal = ((int) (value * 8f) / 8f);
            this.fontSize = 0.25f + (tempVal * 0.25f);
        }
        this.somethingChanged = true;
    }

    public void setOptionValue(EnumOptionsMinimap par1EnumOptions) {
        switch (par1EnumOptions) {
            case SHOWRADAR -> this.showRadar = !this.showRadar;
            case SHOWPLAYERS -> this.showPlayers = !this.showPlayers;
            case SHOWMOBHELMETS -> this.showHelmetsMobs = !this.showHelmetsMobs;
            case SHOWPLAYERHELMETS -> this.showHelmetsPlayers = !this.showHelmetsPlayers;
            case SHOWMOBNAMES -> this.showMobNames = !this.showMobNames;
            case SHOWPLAYERNAMES -> this.showPlayerNames = !this.showPlayerNames;
            case RADAROUTLINE -> this.outlines = !this.outlines;
            case RADARFILTERING -> this.filtering = !this.filtering;
            case SHOWFACING -> this.showFacing = !this.showFacing;
            case SHOWNAMESONLYFORTAGGED -> this.showNamesOnlyForTagged = !this.showNamesOnlyForTagged;
            case RADARMODE -> {
                ++this.radarMode;
                if (this.radarMode > 3) {
                    this.radarMode = 1;
                }
            }
            case SHOWMOBS -> {
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
            default -> throw new IllegalArgumentException("Add code to handle EnumOptionMinimap: " + par1EnumOptions.getName());
        }

        this.somethingChanged = true;
    }

    public boolean isChanged() {
        if (this.somethingChanged) {
            this.somethingChanged = false;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public float getOptionFloatValue(EnumOptionsMinimap options) {
        if (options == EnumOptionsMinimap.RADARFONTSIZE) {
            return (this.fontSize - 0.25f) / 0.25f;
        } else {
            return 0.0f;
        }
    }
}
