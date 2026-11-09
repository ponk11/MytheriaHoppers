package me.mytheria.hoppers.hopper;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum HopperType {
    ALL("Universal", Material.HOPPER),
    CROP("Crop", Material.WHEAT),
    MOB("Mob", Material.ROTTEN_FLESH),
    ORE("Ore", Material.DIAMOND);

    private final String displayName;
    private final Material icon;

    HopperType(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public boolean accepts(Material material) {
        if (this == ALL) return true;
        if (this == CROP) {
            return material.name().endsWith("_SEEDS")
                    || material.name().endsWith("_WHEAT")
                    || material.name().contains("CARROT")
                    || material.name().contains("POTATO")
                    || material.name().contains("BEETROOT")
                    || material == Material.WHEAT
                    || material == Material.MELON
                    || material == Material.MELON_SLICE
                    || material == Material.PUMPKIN
                    || material == Material.SUGAR_CANE
                    || material == Material.CACTUS
                    || material == Material.COCOA_BEANS;
        }
        if (this == MOB) {
            return material == Material.ROTTEN_FLESH
                    || material == Material.BONE
                    || material == Material.STRING
                    || material == Material.SPIDER_EYE
                    || material == Material.GUNPOWDER
                    || material == Material.ENDER_PEARL
                    || material == Material.SLIME_BALL
                    || material == Material.MAGMA_CREAM
                    || material == Material.LEATHER
                    || material == Material.FEATHER
                    || material == Material.RABBIT_HIDE
                    || material == Material.RABBIT_FOOT
                    || material == Material.PHANTOM_MEMBRANE
                    || material == Material.SHULKER_SHELL
                    || material == Material.BLAZE_ROD
                    || material == Material.GHAST_TEAR;
        }
        return material.name().endsWith("_ORE")
                || material == Material.DIAMOND
                || material == Material.EMERALD
                || material == Material.GOLD_INGOT
                || material == Material.IRON_INGOT
                || material == Material.REDSTONE
                || material == Material.LAPIS_LAZULI
                || material == Material.COAL
                || material == Material.QUARTZ;
    }

    public String coloredName() {
        return ChatColor.LIGHT_PURPLE + displayName + " Hopper";
    }

    public static HopperType fromName(String value) {
        if (value == null) return ALL;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return ALL;
        }
    }
}
