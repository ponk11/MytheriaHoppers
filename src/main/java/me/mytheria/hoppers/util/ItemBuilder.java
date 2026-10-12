package me.mytheria.hoppers.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ItemBuilder {


    private final ItemStack item;


    public ItemBuilder(Material material) {

        item = new ItemStack(material);

    }


    @SuppressWarnings("deprecation")
    public ItemBuilder name(String name) {

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(
                ColorUtil.color(name)
        );

        item.setItemMeta(meta);

        return this;

    }



    @SuppressWarnings("deprecation")
    public ItemBuilder lore(String... lore) {

        ItemMeta meta = item.getItemMeta();

        meta.setLore(
                Arrays.stream(lore)
                        .map(ColorUtil::color)
                        .collect(Collectors.toList())
        );

        item.setItemMeta(meta);

        return this;

    }



    public ItemStack build() {

        return item;

    }

}
