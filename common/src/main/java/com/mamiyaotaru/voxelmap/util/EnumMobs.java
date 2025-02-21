package com.mamiyaotaru.voxelmap.util;

import java.util.Arrays;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.animal.horse.Mule;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Evoker;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;

public enum EnumMobs {
    UNKNOWN(null, "Unknown", 8.0F, "textures/entity/player/wide/steve.png", "", true, true),
    GENERIC_TAME(null, "Unknown_Tame", 8.0F, "textures/entity/wolf/wolf.png", "", false, true),
    GENERIC_NEUTRAL(null, "Unknown_Neutral", 8.0F, "textures/entity/pig/pig.png", "", false, true),
    GENERIC_HOSTILE(null, "Unknown_Hostile", 8.0F, "textures/entity/zombie/zombie.png", "", true, false),

    ARMADILLO(Armadillo.class, "Armadillo", 0.0F, "textures/entity/armadillo/armadillo.png", "", false, true),
    ARMOR_STAND(ArmorStand.class, "Armor_Stand", 0.0F, "textures/entity/armorstand/wood.png", "", true, true),
    ALLAY(Allay.class, "Allay", 0.0F, "textures/entity/allay/allay.png", "", false, true),
    AXOLOTL(Axolotl.class, "Axolotl", 0.0F, "textures/entity/axolotl/axolotl_blue.png", "", false, true),

    BAT(Bat.class, "Bat", 0.0F, "textures/entity/bat.png", "", false, true),
    BEE(Bee.class, "Bee", 0.0F, "textures/entity/bee/bee.png", "", true, true),
    BLAZE(Blaze.class, "Blaze", 0.0F, "textures/entity/blaze.png", "", true, false),
    BOGGED(Bogged.class, "Bogged", 0.0F, "textures/entity/skeleton/bogged.png", "textures/entity/skeleton/bogged_overlay.png", true, false),
    BREEZE(Breeze.class, "Breeze", 0.0F, "textures/entity/breeze.png", "", true, false),

    CAMEL(Camel.class, "Camel", 0.0F, "textures/entity/camel/camel.png", "", false, true),
    CAT(Cat.class, "Cat", 0.0F, "textures/entity/cat/siamese.png", "", false, true),
    CAVE_SPIDER(CaveSpider.class, "Cave_Spider", 0.0F, "textures/entity/spider/cave_spider.png", "", true, false),
    CHICKEN(Chicken.class, "Chicken", 6.0F, "textures/entity/chicken.png", "", false, true),
    COD(Cod.class, "Cod", 0.0F, "textures/entity/fish/cod.png", "", false, true),
    COW(Cow.class, "Cow", 0.0F, "textures/entity/cow/cow.png", "", false, true),
    CREAKING(Creaking.class, "Creaking", 0.0F, "textures/entity/creaking/creaking.png", "textures/entity/creaking/creaking_eyes.png", true, false),
    CREEPER(Creeper.class, "Creeper", 0.0F, "textures/entity/creeper/creeper.png", "", true, false),

    DOLPHIN(Dolphin.class, "Dolphin", 0.0F, "textures/entity/dolphin.png", "", false, true),
    DROWNED(Drowned.class, "Drowned", 0.0F, "textures/entity/zombie/drowned.png", "textures/entity/zombie/drowned_outer_layer.png", true, false),

    ELDER_GUARDIAN(ElderGuardian.class, "Elder_Guardian", 0.0F, "textures/entity/guardian_elder.png", "", true, false),
    ENDER_DRAGON(EnderDragon.class, "Ender_Dragon", 0.0F, "textures/entity/enderdragon/dragon.png", "", true, false),
    ENDERMAN(EnderMan.class, "Enderman", 0.0F, "textures/entity/enderman/enderman.png", "textures/entity/enderman/enderman_eyes.png", true, false),
    ENDERMITE(Endermite.class, "Endermite", 0.0F, "textures/entity/endermite.png", "", true, false),
    EVOKER(Evoker.class, "Evoker", 0.0F, "textures/entity/illager/evoker.png", "", true, false),

    FOX(Fox.class, "Fox", 0.0F, "textures/entity/fox/fox.png", "", false, true),
    FROG(Frog.class, "Frog", 0.0F, "textures/entity/frog/cold_frog.png", "", false, true),

    GHAST(Ghast.class, "Ghast", 0.0F, "textures/entity/ghast/ghast.png", "", true, false),
    GLOW_SQUID(GlowSquid.class, "Glow_Squid", 0.0F, "textures/entity/squid/glow_squid.png", "", false, true),
    GOAT(Goat.class, "Goat", 0.0F, "textures/entity/goat/goat.png", "", false, true),
    GUARDIAN(Guardian.class, "Guardian", 0.0F, "textures/entity/guardian.png", "", true, false),

    HOGLIN(Hoglin.class, "Hoglin", 0.0F, "textures/entity/hoglin/hoglin.png", "", true, false),
    HORSE(Horse.class, "Horse", 0.0F, "textures/entity/horse/horse_creamy.png", "", false, true),
    HUSK(Husk.class, "Husk", 0.0F, "textures/entity/zombie/husk.png", "", true, false),

    ILLUSIONER(Illusioner.class, "Illusioner", 0.0F, "textures/entity/illager/illusioner.png", "", true, false),
    IRON_GOLEM(IronGolem.class, "Iron_Golem", 0.0F, "textures/entity/iron_golem/iron_golem.png", "", false, true),

    LLAMA(Llama.class, "Llama", 0.0F, "textures/entity/llama/brown.png", "", false, true),

    MAGMA_CUBE(MagmaCube.class, "Magma_Cube", 0.0F, "textures/entity/slime/magmacube.png", "", true, false),
    MOOSHROOM(MushroomCow.class, "Mooshroom", 0.0F, "textures/entity/cow/red_mooshroom.png", "", false, true),

    OCELOT(Ocelot.class, "Ocelot", 0.0F, "textures/entity/cat/ocelot.png", "", false, true),

    PANDA(Panda.class, "Panda", 0.0F, "textures/entity/panda/panda.png", "", true, true),
    PARROT(Parrot.class, "Parrot", 0.0F, "textures/entity/parrot/parrot_red_blue.png", "", false, true),
    PHANTOM(Phantom.class, "Phantom", 0.0F, "textures/entity/phantom.png", "textures/entity/phantom_eyes.png", true, false),
    PIG(Pig.class, "Pig", 0.0F, "textures/entity/pig/pig.png", "", false, true),
    PIGLIN(Piglin.class, "Piglin", 0.0F, "textures/entity/piglin/piglin.png", "", true, false),
    PIGLIN_BRUTE(PiglinBrute.class, "Piglin_Brute", 0.0F, "textures/entity/piglin/piglin_brute.png", "", true, false),
    PILLAGER(Pillager.class, "Pillager", 0.0F, "textures/entity/illager/pillager.png", "", true, false),
    PLAYER(RemotePlayer.class, "Player", 8.0F, "textures/entity/steve.png", "", false, false),
    POLAR_BEAR(PolarBear.class, "Polar_Bear", 0.0F, "textures/entity/bear/polarbear.png", "", true, true),
    PUFFERFISH(Pufferfish.class, "Pufferfish", 0.0F, "textures/entity/fish/pufferfish.png", "", false, true),

    RABBIT(Rabbit.class, "Rabbit", 0.0F, "textures/entity/rabbit/salt.png", "", false, true),
    RAVAGER(Ravager.class, "Ravager", 0.0F, "textures/entity/illager/ravager.png", "", true, false),

    SALMON(Salmon.class, "Salmon", 0.0F, "textures/entity/fish/salmon.png", "", false, true),
    SHEEP(Sheep.class, "Sheep", 0.0F, "textures/entity/sheep/sheep.png", "", false, true),
    SHULKER(Shulker.class, "Shulker", 0.0F, "textures/entity/shulker/shulker_purple.png", "", true, false),
    SILVERFISH(Silverfish.class, "Silverfish", 0.0F, "textures/entity/silverfish.png", "", true, false),
    SKELETON(Skeleton.class, "Skeleton", 0.0F, "textures/entity/skeleton/skeleton.png", "", true, false),
    SLIME(Slime.class, "Slime", 8.0F, "textures/entity/slime/slime.png", "", true, false),
    SNIFFER(Sniffer.class, "Sniffer", 0.0F, "textures/entity/sniffer/sniffer.png", "", false, true),
    SNOW_GOLEM(SnowGolem.class, "Snow_Golem", 0.0F, "textures/entity/snow_golem.png", "", false, true),
    SPIDER(Spider.class, "Spider", 0.0F, "textures/entity/spider/spider.png", "", true, false),
    SQUID(Squid.class, "Squid", 0.0F, "textures/entity/squid/squid.png", "", false, true),
    STRAY(Stray.class, "Stray", 0.0F, "textures/entity/skeleton/stray.png", "textures/entity/skeleton/stray_overlay.png", true, false),
    STRIDER(Strider.class, "Strider", 0.0F, "textures/entity/strider/strider.png", "", false, true),

    TADPOLE(Tadpole.class, "Tadpole", 0.0F, "textures/entity/tadpole/tadpole.png", "", false, true),
    TRADER_LLAMA(TraderLlama.class, "Trader_Llama", 0.0F, "textures/entity/llama/brown.png", "", false, true),
    TROPICAL_FISH_A(TropicalFish.class, "Tropical_Fish", 0.0F, "textures/entity/fish/tropical_a.png", "", false, true),
    TROPICAL_FISH_B(TropicalFish.class, "Tropical_Fish", 0.0F, "textures/entity/fish/tropical_b.png", "", false, true),
    TURTLE(Turtle.class, "Turtle", 0.0F, "textures/entity/turtle/big_sea_turtle.png", "", false, true),

    VEX(Vex.class, "Vex", 0.0F, "textures/entity/illager/vex.png", "", true, false),
    VILLAGER(Villager.class, "Villager", 0.0F, "textures/entity/villager/villager.png", "", false, true),
    VINDICATOR(Vindicator.class, "Vindicator", 0.0F, "textures/entity/illager/vindicator.png", "", true, false),

    WANDERING_TRADER(WanderingTrader.class, "Wandering_Trader", 0.0F, "textures/entity/wandering_trader.png", "", false, true),
    WITCH(Witch.class, "Witch", 0.0F, "textures/entity/witch.png", "", true, false),
    WITHER(WitherBoss.class, "Wither", 0.0F, "textures/entity/wither/wither.png", "", true, false),
    WITHER_SKELETON(WitherSkeleton.class, "Wither_Skeleton", 0.0F, "textures/entity/skeleton/wither_skeleton.png", "", true, false),
    WARDEN(Warden.class, "Warden", 0.0F, "textures/entity/warden/warden.png", "", true, false),
    WOLF(Wolf.class, "Wolf", 0.0F, "textures/entity/wolf/wolf.png", "", true, true),

    ZOGLIN(Zoglin.class, "Zoglin", 0.0F, "textures/entity/hoglin/zoglin.png", "", true, false),
    ZOMBIE(Zombie.class, "Zombie", 0.0F, "textures/entity/zombie/zombie.png", "", true, false),
    ZOMBIE_VILLAGER(ZombieVillager.class, "Zombie_villager", 0.0F, "textures/entity/zombie_villager/zombie_villager.png", "", true, false),
    ZOMBIFIED_PIGLIN(ZombifiedPiglin.class, "Zombified_Piglin", 0.0F, "textures/entity/piglin/zombified_piglin.png", "", true, true);

    public final Class<? extends Entity> clazz;
    public final String id;
    public final float expectedWidth;
    public final ResourceLocation resourceLocation;
    public ResourceLocation secondaryResourceLocation;
    public final boolean isHostile;
    public final boolean isNeutral;
    public boolean enabled;

    public static EnumMobs getMobByName(String par0) {
        return Arrays.stream(values()).filter(enumMob -> enumMob.id.equals(par0)).findFirst().orElse(null);
    }

    public static EnumMobs getMobTypeByEntity(Entity entity) {
        Class<? extends Entity> clazz = entity.getClass();
        if (clazz.equals(TropicalFish.class)) {
            return ((TropicalFish) entity).getVariant().getPackedId() == 0 ? TROPICAL_FISH_A : TROPICAL_FISH_B;
        } else {
            return getMobTypeByClass(clazz);
        }
    }

    private static EnumMobs getMobTypeByClass(Class<? extends Entity> clazz) {
        if (RemotePlayer.class.isAssignableFrom(clazz)) {
            return PLAYER;
        } else if (!clazz.equals(Horse.class) && !clazz.equals(Donkey.class) && !clazz.equals(Mule.class) && !clazz.equals(SkeletonHorse.class) && !clazz.equals(ZombieHorse.class)) {
            return Arrays.stream(values()).filter(enumMob -> clazz.equals(enumMob.clazz)).findFirst().orElse(UNKNOWN);
        } else {
            return HORSE;
        }
    }

    EnumMobs(Class<? extends Entity> clazz, String name, float expectedWidth, String primaryPath, String secondaryPath, boolean isHostile, boolean isNeutral) {
        this.clazz = clazz;
        this.id = name;
        this.expectedWidth = expectedWidth;
        this.resourceLocation = ResourceLocation.parse(primaryPath.toLowerCase());
        this.secondaryResourceLocation = secondaryPath.isEmpty() ? null : ResourceLocation.parse(secondaryPath.toLowerCase());
        this.isHostile = isHostile;
        this.isNeutral = isNeutral;
        this.enabled = true;
    }
}
