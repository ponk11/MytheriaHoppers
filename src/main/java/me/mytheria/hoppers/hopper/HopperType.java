package me.mytheria.hoppers.hopper;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public enum HopperType {
    ALL("Universal", Material.HOPPER, ChatColor.DARK_PURPLE),
    CROP("Crop", Material.WHEAT, ChatColor.DARK_GREEN),
    MOB("Mob", Material.ROTTEN_FLESH, ChatColor.DARK_RED),
    ORE("Ore", Material.DIAMOND, ChatColor.BLUE);

    private final String displayName;
    private final Material icon;
    private final ChatColor color;

    HopperType(String displayName, Material icon, ChatColor color) {
        this.displayName = displayName;
        this.icon = icon;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public ChatColor getColor() {
        return color;
    }

    public Material getGuiPane() {
        switch (this) {
            case CROP:
                return Material.GREEN_STAINED_GLASS_PANE;
            case MOB:
                return Material.RED_STAINED_GLASS_PANE;
            case ORE:
                return Material.BLUE_STAINED_GLASS_PANE;
            default:
                return Material.PURPLE_STAINED_GLASS_PANE;
        }
    }

    public Material getGuiAccent() {
        switch (this) {
            case CROP:
                return Material.GREEN_WOOL;
            case MOB:
                return Material.RED_WOOL;
            case ORE:
                return Material.CYAN_WOOL;
            default:
                return Material.PURPLE_WOOL;
        }
    }

    public String[] getDescription() {
        switch (this) {
            case CROP:
                return new String[]{
                        "Collects crops and farm drops.",
                        "Starts in its current chunk only.",
                        "Each range upgrade adds 1 chunk radius."
                };
            case MOB:
                return new String[]{
                        "Collects mob drops and loot.",
                        "Starts in its current chunk only.",
                        "Each range upgrade adds 1 chunk radius."
                };
            case ORE:
                return new String[]{
                        "Collects ores and mining drops.",
                        "Starts in its current chunk only.",
                        "Each range upgrade adds 1 chunk radius."
                };
            default:
                return new String[]{
                        "Collects every item type.",
                        "Picks up items around the hopper.",
                        "Range upgrades increase block coverage."
                };
        }
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
        return color + displayName + " Hopper";
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
