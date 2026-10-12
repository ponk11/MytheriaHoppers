package me.mytheria.hoppers.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.mytheria.hoppers.MytheriaHoppers;
import me.mytheria.hoppers.hopper.HopperData;

public class HopperGUI {

    private final MytheriaHoppers plugin;
    public static final Map<UUID, Location> openHopperLocations = new HashMap<>();

    public HopperGUI(MytheriaHoppers plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player, Location location) {
        openHopperLocations.put(player.getUniqueId(), location);

        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Hopper Upgrades");
        HopperData data = plugin.getHopperManager().getData(location);

        int nextSpeedLevel = data.getSpeedLevel() + 1;
        double speedCost = plugin.getConfig().getDouble("speed-upgrades." + nextSpeedLevel + ".cost", -1);
        String speedCostText = speedCost > 0 ? "$" + speedCost : "MAX";

        ItemStack speedItem = createGuiItem(
                Material.SUGAR,
                ChatColor.GREEN + "Speed Upgrade",
                ChatColor.GRAY + "Current Level: " + ChatColor.YELLOW + data.getSpeedLevel(),
                ChatColor.GRAY + "Next Level Cost: " + ChatColor.GOLD + speedCostText,
                ChatColor.YELLOW + "Click to upgrade!"
        );
        gui.setItem(11, speedItem);

        int nextRangeLevel = data.getRangeLevel() + 1;
        double rangeCost = plugin.getConfig().getDouble("range-upgrades." + nextRangeLevel + ".cost", -1);
        String rangeCostText = rangeCost > 0 ? "$" + rangeCost : "MAX";

        ItemStack rangeItem = createGuiItem(
                Material.COMPASS,
                ChatColor.AQUA + "Range Upgrade",
                ChatColor.GRAY + "Current Level: " + ChatColor.YELLOW + data.getRangeLevel(),
                ChatColor.GRAY + "Next Level Cost: " + ChatColor.GOLD + rangeCostText,
                ChatColor.YELLOW + "Click to upgrade!"
        );
        gui.setItem(13, rangeItem);

        String filterStatus = data.isFilterUnlocked() ? ChatColor.GREEN + "Unlocked" : ChatColor.RED + "Locked";
        ItemStack filterItem = createGuiItem(
                Material.HOPPER,
                ChatColor.GOLD + "Item Filter Settings",
                ChatColor.GRAY + "Filter Status: " + filterStatus,
                ChatColor.YELLOW + "Click to manage item filter"
        );
        gui.setItem(15, filterItem);

        player.openInventory(gui);
    }

    public void openFilterGUI(Player player, Location location) {
        openHopperLocations.put(player.getUniqueId(), location);

        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_GRAY + "Item Filter Selection");
        HopperData data = plugin.getHopperManager().getData(location);

        if (!data.isFilterUnlocked()) {
            // Show unlock button
            double unlockCost = plugin.getConfig().getDouble("filter.unlock-cost", plugin.getConfig().getDouble("filter-unlock-cost", 5000000.0));
            ItemStack unlockItem = createGuiItem(
                    Material.DIAMOND_BLOCK,
                    ChatColor.GOLD + "Unlock Item Filter",
                    ChatColor.GRAY + "Cost: $" + unlockCost,
                    ChatColor.YELLOW + "Click to unlock!"
            );
            gui.setItem(13, unlockItem);
        } else {
            // Slot 26 clears the current selection.
            if (!data.getFilteredMaterials().isEmpty()) {
                ItemStack selectedItem = createGuiItem(
                        Material.BARRIER,
                        ChatColor.RED + "Clear Filter",
                        ChatColor.YELLOW + "Click to remove all selected items"
                );
                gui.setItem(26, selectedItem);
            } else {
                ItemStack emptySlot = createGuiItem(
                        Material.PAPER,
                        ChatColor.GRAY + "No Items Selected",
                        ChatColor.YELLOW + "Click an empty slot with an item on your cursor"
                );
                gui.setItem(26, emptySlot);
            }

            // Each unlocked slot stores one allowed material.
            int availableSlots = data.getUnlockedFilterSlots();
            for (int i = 0; i < availableSlots && i < 18; i++) {
                Material filtered = data.getFilteredMaterials().stream()
                        .sorted(java.util.Comparator.comparing(Material::name))
                        .skip(i)
                        .findFirst()
                        .orElse(null);
                if (filtered != null) {
                    gui.setItem(i, createGuiItem(filtered, ChatColor.GREEN + filtered.name(), ChatColor.YELLOW + "Click to remove"));
                } else {
                    ItemStack slot = createGuiItem(
                            Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                            ChatColor.GRAY + "Empty Slot",
                            ChatColor.YELLOW + "Click with an item on your cursor to add it"
                    );
                    gui.setItem(i, slot);
                }
            }

            // Show unlock slot buttons (18-25)
            if (availableSlots < 18) {
                double slotUnlockCost = plugin.getConfig().getDouble("filter.slot-cost", plugin.getConfig().getDouble("filter-slot-unlock-cost", 250000.0));
                int maxSlots = Math.max(1, Math.min(18, plugin.getConfig().getInt("filter.max-slots", 18)));
                int slotsToUnlock = Math.min(8, maxSlots - availableSlots);
                
                for (int i = 0; i < slotsToUnlock; i++) {
                    ItemStack unlockSlot = createGuiItem(
                            Material.GOLD_BLOCK,
                            ChatColor.GOLD + "Unlock Slot " + (availableSlots + i + 1),
                            ChatColor.GRAY + "Cost: $" + slotUnlockCost,
                            ChatColor.YELLOW + "Click to unlock!"
                    );
                    gui.setItem(18 + i, unlockSlot);
                }
            }
        }

        player.openInventory(gui);
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> loreList = new ArrayList<>();
            for (String line : lore) {
                loreList.add(line);
            }
            meta.setLore(loreList);
            item.setItemMeta(meta);
        }
        return item;
    }
}
