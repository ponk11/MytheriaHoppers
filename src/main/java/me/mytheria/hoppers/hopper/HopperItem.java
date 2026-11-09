package me.mytheria.hoppers.hopper;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class HopperItem {

    private HopperItem() {
    }

    @SuppressWarnings("deprecation")
    public static ItemStack create(
            int speedLevel,
            int rangeLevel
    ) {
        return create(HopperType.ALL, speedLevel, rangeLevel);
    }

    public static ItemStack create(HopperType type, int speedLevel, int rangeLevel) {

        ItemStack item =
                new ItemStack(Material.HOPPER);

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                type.getColor() + "Mytheria " + type.getDisplayName() + " Hopper"
        );

        List<String> lore =
                new ArrayList<>();

        lore.add(color("&8&m--------------------"));
        lore.add(color("&fType: " + type.getColor() + type.getDisplayName()));
        lore.add(color("&fCollection:"));
        for (String line : type.getDescription()) {
            lore.add(color("&7  " + line));
        }
        lore.add(color("&7"));
        lore.add(color("&fCurrent upgrades:"));
        lore.add(color("&7  Transfer speed: &dLevel " + speedLevel));
        lore.add(color("&7  Collection range: &dLevel " + rangeLevel));
        lore.add(color("&7"));
        lore.add(color("&ePlace this hopper to preserve"));
        lore.add(color("&eits upgrades and settings."));
        lore.add(color("&8&m--------------------"));

        meta.setLore(lore);

        meta.getPersistentDataContainer().set(
                HopperKeys.speedLevel(),
                PersistentDataType.INTEGER,
                speedLevel
        );

        meta.getPersistentDataContainer().set(
                HopperKeys.rangeLevel(),
                PersistentDataType.INTEGER,
                rangeLevel
        );
        meta.getPersistentDataContainer().set(
                HopperKeys.type(),
                PersistentDataType.STRING,
                type.name()
        );

        item.setItemMeta(meta);

        return item;
    }

    @SuppressWarnings("deprecation")
    private static String color(String text) {

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }
}
