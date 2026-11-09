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
                color("&d&lMytheria " + type.getDisplayName() + " Hopper")
        );

        List<String> lore =
                new ArrayList<>();

        lore.add(color("&7"));
        lore.add(
                color(
                        "&7Type: &d"
                                + type.getDisplayName()
                )
        );
        lore.add(
                color(
                        "&7Speed Level: &d"
                                + speedLevel
                )
        );
        lore.add(
                color(
                        "&7Range Level: &d"
                                + rangeLevel
                )
        );
        lore.add(color("&7"));
        lore.add(
                color(
                        "&8Place this hopper to keep"
                )
        );
        lore.add(
                color(
                        "&8its upgrades."
                )
        );

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
