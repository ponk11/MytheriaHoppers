package me.mytheria.hoppers.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import me.mytheria.hoppers.hopper.HopperType;

public class HopperGUI {

    public static final String ADMIN_TITLE = ChatColor.DARK_RED + "Hopper Administration";
    public static final String CONFIRM_TITLE = ChatColor.DARK_RED + "Confirm Hopper Removal";
    public static final String FINAL_CONFIRM_TITLE = ChatColor.DARK_RED + "Final Removal Confirmation";
    public static final String SETTINGS_TITLE = ChatColor.DARK_GRAY + "Mytheria Admin Settings";
    public static final String TYPE_DELETE_TITLE = ChatColor.DARK_RED + "Delete Hopper Type";
    public static final String TYPE_CONFIRM_TITLE = ChatColor.DARK_RED + "Confirm Type Deletion";
    public static final String HELP_TITLE = ChatColor.DARK_PURPLE + "Mytheria Hoppers Help";
    private final MytheriaHoppers plugin;
    public static final Map<UUID, Location> openHopperLocations = new HashMap<>();
    private final Map<UUID, Integer> adminPages = new HashMap<>();
    private final Map<UUID, String> pendingRemovalWorlds = new HashMap<>();
    private final Map<UUID, HopperType> pendingRemovalTypes = new HashMap<>();

    public HopperGUI(MytheriaHoppers plugin) {
        this.plugin = plugin;
    }

    public void openGUI(Player player, Location location) {
        openHopperLocations.put(player.getUniqueId(), location);

        HopperData data = plugin.getHopperManager().getData(location);
        Inventory gui = Bukkit.createInventory(null, 27,
                data.getType().getColor() + data.getType().getDisplayName() + " Hopper Upgrades");

        gui.setItem(4, createGuiItem(
                data.getType().getIcon(),
                data.getType().coloredName(),
                ChatColor.GRAY + "How it works:",
                ChatColor.GRAY + "- " + data.getType().getDescription()[0],
                ChatColor.GRAY + "- " + data.getType().getDescription()[1],
                ChatColor.GRAY + "- " + data.getType().getDescription()[2],
                ChatColor.GRAY + "Range upgrades: " + ChatColor.AQUA
                        + (plugin.getConfig().getBoolean("hopper-types.enabled", true)
                        && data.getType() != HopperType.ALL ? "chunk radius" : "block radius"),
                ChatColor.GRAY + "Starting coverage: " + ChatColor.AQUA
                        + (plugin.getConfig().getBoolean("hopper-types.enabled", true)
                        && data.getType() != HopperType.ALL ? "current chunk" : "nearby blocks"),
                ChatColor.DARK_GRAY + "Managed by an administrator"
        ));

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
                ChatColor.GRAY + "Coverage: " + ChatColor.AQUA
                        + (plugin.getConfig().getBoolean("hopper-types.enabled", true)
                        && data.getType() != HopperType.ALL ? "nearby chunks" : "nearby blocks"),
                ChatColor.GRAY + "Each range upgrade: " + ChatColor.AQUA
                        + (plugin.getConfig().getBoolean("hopper-types.enabled", true)
                        && data.getType() != HopperType.ALL ? "+1 chunk radius" : "larger block radius"),
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
        fillEmptySlots(gui, data.getType().getGuiPane());

        player.openInventory(gui);
    }

    public void openFilterGUI(Player player, Location location) {
        openHopperLocations.put(player.getUniqueId(), location);

        HopperData data = plugin.getHopperManager().getData(location);
        Inventory gui = Bukkit.createInventory(null, 27,
                data.getType().getColor() + data.getType().getDisplayName() + " Hopper Filter");

        if (!data.isFilterUnlocked()) {
            double unlockCost = plugin.getConfig().getDouble("filter.unlock-cost", plugin.getConfig().getDouble("filter-unlock-cost", 5000000.0));
            ItemStack unlockItem = createGuiItem(
                    Material.DIAMOND_BLOCK,
                    ChatColor.GOLD + "Unlock Item Filter",
                    ChatColor.GRAY + "Cost: $" + unlockCost,
                    ChatColor.YELLOW + "Click to unlock!"
            );
            gui.setItem(13, unlockItem);
        } else {
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

    public void openAdminGUI(Player player, int page) {
        List<String> worlds = getWorldNames();
        int maxPage = Math.max(0, (worlds.size() - 1) / 45);
        int safePage = Math.max(0, Math.min(page, maxPage));
        adminPages.put(player.getUniqueId(), safePage);

        Inventory gui = Bukkit.createInventory(null, 54, ADMIN_TITLE);
        int start = safePage * 45;
        for (int slot = 0; slot < 45 && start + slot < worlds.size(); slot++) {
            String worldName = worlds.get(start + slot);
            int count = countHoppers(worldName);
            gui.setItem(slot, createGuiItem(
                    Material.HOPPER,
                    ChatColor.GOLD + worldName,
                    ChatColor.GRAY + "Tracked hoppers: " + ChatColor.YELLOW + count,
                    ChatColor.YELLOW + "Shift-right-click to begin removal",
                    ChatColor.DARK_RED + "This requires two confirmations"
            ));
        }

        if (safePage > 0) {
            gui.setItem(45, createGuiItem(Material.ARROW, ChatColor.YELLOW + "Previous page"));
        }
        if (safePage < maxPage) {
            gui.setItem(53, createGuiItem(Material.ARROW, ChatColor.YELLOW + "Next page"));
        }
        if (plugin.getHopperManager().canUndoLastRemoval()) {
            gui.setItem(49, createGuiItem(
                    Material.EMERALD,
                    ChatColor.GREEN + "Undo last removal",
                    ChatColor.GRAY + "Restores the last removed hopper set",
                    ChatColor.YELLOW + "Click to restore"
            ));
        }
        gui.setItem(50, createGuiItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                ChatColor.GRAY + "Close this menu"
        ));
        gui.setItem(48, createGuiItem(
                Material.COMPARATOR,
                ChatColor.LIGHT_PURPLE + "Plugin settings",
                ChatColor.GRAY + "Toggle hopper types and review defaults",
                ChatColor.YELLOW + "Click to open"
        ));
        gui.setItem(47, createGuiItem(
                Material.TNT,
                ChatColor.RED + "Delete by hopper type",
                ChatColor.GRAY + "Remove every hopper of one type",
                ChatColor.YELLOW + "Click to choose"
        ));
        fillEmptySlots(gui, Material.GRAY_STAINED_GLASS_PANE);
        player.openInventory(gui);
    }

    public void openAdminSettings(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, SETTINGS_TITLE);
        boolean enabled = plugin.getConfig().getBoolean("hopper-types.enabled", true);
        gui.setItem(11, createGuiItem(
                enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                ChatColor.LIGHT_PURPLE + "Typed hoppers",
                ChatColor.GRAY + "Crop, mob, ore and universal variants",
                ChatColor.GRAY + "Status: " + (enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"),
                ChatColor.YELLOW + "Click to toggle"
        ));
        boolean recipesEnabled = plugin.getConfig().getBoolean("crafting-recipes.enabled", true);
        gui.setItem(13, createGuiItem(
                recipesEnabled ? Material.CRAFTING_TABLE : Material.BARRIER,
                ChatColor.GOLD + "Hopper crafting recipe",
                ChatColor.GRAY + "Status: " + (recipesEnabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"),
                ChatColor.YELLOW + "Click to toggle"
        ));
        gui.setItem(14, createGuiItem(
                Material.CLOCK,
                ChatColor.YELLOW + "Reload configuration",
                ChatColor.GRAY + "Apply config.yml without a restart",
                ChatColor.YELLOW + "Click to reload"
        ));
        gui.setItem(15, createGuiItem(Material.BARRIER, ChatColor.RED + "Close"));
        fillEmptySlots(gui, Material.BLACK_STAINED_GLASS_PANE);
        player.openInventory(gui);
    }

    public void openHelpGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, HELP_TITLE);
        gui.setItem(10, createGuiItem(Material.BOOK, ChatColor.AQUA + "/mh",
                ChatColor.GRAY + "Open this help menu"));
        gui.setItem(12, createGuiItem(Material.COMPARATOR, ChatColor.YELLOW + "/mh admin",
                ChatColor.GRAY + "Open hopper administration"));
        gui.setItem(14, createGuiItem(Material.CHEST, ChatColor.GREEN + "/mh give <player> <type> [amount]",
                ChatColor.GRAY + "Give a typed hopper item",
                ChatColor.GRAY + "Types: all, crop, mob, ore"));
        gui.setItem(16, createGuiItem(Material.EMERALD, ChatColor.LIGHT_PURPLE + "/mh reload",
                ChatColor.GRAY + "Reload the plugin configuration"));
        fillEmptySlots(gui, Material.BLACK_STAINED_GLASS_PANE);
        player.openInventory(gui);
    }

    public void openTypeRemovalGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, TYPE_DELETE_TITLE);
        HopperType[] types = HopperType.values();
        for (int i = 0; i < types.length; i++) {
            HopperType type = types[i];
            gui.setItem(10 + i, createGuiItem(type.getIcon(),
                    type.getColor() + "Delete " + type.getDisplayName() + " Hoppers",
                    ChatColor.GRAY + "Tracked: " + ChatColor.YELLOW + countType(type),
                    ChatColor.RED + "Click to continue"));
        }
        gui.setItem(15, createGuiItem(Material.BARRIER, ChatColor.GREEN + "Cancel"));
        fillEmptySlots(gui, Material.BLACK_STAINED_GLASS_PANE);
        player.openInventory(gui);
    }

    public void openTypeRemovalConfirmation(Player player, HopperType type) {
        pendingRemovalTypes.put(player.getUniqueId(), type);
        Inventory gui = Bukkit.createInventory(null, 27, TYPE_CONFIRM_TITLE);
        gui.setItem(11, createGuiItem(Material.RED_WOOL,
                ChatColor.RED + "DELETE " + type.getDisplayName().toUpperCase() + " HOPPERS",
                ChatColor.GRAY + "Count: " + ChatColor.YELLOW + countType(type),
                ChatColor.DARK_RED + "This removes the blocks and saved data",
                ChatColor.RED + "Click only if you are certain"));
        gui.setItem(15, createGuiItem(Material.BARRIER, ChatColor.GREEN + "Cancel"));
        player.openInventory(gui);
    }

    public void openRemovalConfirmation(Player player, String worldName) {
        pendingRemovalWorlds.put(player.getUniqueId(), worldName);
        Inventory gui = Bukkit.createInventory(null, 27, CONFIRM_TITLE);
        gui.setItem(11, createGuiItem(
                Material.YELLOW_WOOL,
                ChatColor.YELLOW + "Confirm " + worldName,
                ChatColor.GRAY + "Step 1 of 2",
                ChatColor.RED + "This will prepare every tracked hopper in this world",
                ChatColor.YELLOW + "Click to continue"
        ));
        gui.setItem(15, createGuiItem(Material.BARRIER, ChatColor.RED + "Cancel"));
        player.openInventory(gui);
    }

    public void openFinalRemovalConfirmation(Player player) {
        String worldName = pendingRemovalWorlds.get(player.getUniqueId());
        if (worldName == null) {
            openAdminGUI(player, 0);
            return;
        }
        Inventory gui = Bukkit.createInventory(null, 27, FINAL_CONFIRM_TITLE);
        gui.setItem(11, createGuiItem(
                Material.RED_WOOL,
                ChatColor.RED + "DELETE ALL HOPPERS",
                ChatColor.GRAY + "World: " + ChatColor.YELLOW + worldName,
                ChatColor.GRAY + "Count: " + ChatColor.YELLOW + countHoppers(worldName),
                ChatColor.DARK_RED + "Step 2 of 2: this cannot be undone after another removal",
                ChatColor.RED + "Click only if you are certain"
        ));
        gui.setItem(15, createGuiItem(Material.BARRIER, ChatColor.GREEN + "Cancel"));
        player.openInventory(gui);
    }

    public String getAdminWorldForSlot(Player player, int slot) {
        if (slot < 0 || slot >= 45) return null;
        List<String> worlds = getWorldNames();
        Integer storedPage = adminPages.get(player.getUniqueId());
        int page = storedPage == null ? 0 : storedPage;
        int index = page * 45 + slot;
        return index < worlds.size() ? worlds.get(index) : null;
    }

    public String getPendingRemovalWorld(Player player) {
        return pendingRemovalWorlds.get(player.getUniqueId());
    }

    public void clearPendingRemoval(Player player) {
        pendingRemovalWorlds.remove(player.getUniqueId());
    }

    public HopperType getPendingRemovalType(Player player) {
        return pendingRemovalTypes.get(player.getUniqueId());
    }

    public void clearPendingRemovalType(Player player) {
        pendingRemovalTypes.remove(player.getUniqueId());
    }

    private List<String> getWorldNames() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            counts.put(world.getName(), 0);
        }
        for (Location location : plugin.getHopperManager().getHoppers().keySet()) {
            if (location.getWorld() != null) {
                counts.putIfAbsent(location.getWorld().getName(), 0);
                counts.put(location.getWorld().getName(), counts.get(location.getWorld().getName()) + 1);
            }
        }
        List<String> worlds = new ArrayList<>(counts.keySet());
        Collections.sort(worlds, String.CASE_INSENSITIVE_ORDER);
        return worlds;
    }

    private int countHoppers(String worldName) {
        int count = 0;
        for (Location location : plugin.getHopperManager().getHoppers().keySet()) {
            if (location.getWorld() != null && location.getWorld().getName().equals(worldName)) count++;
        }
        return count;
    }

    private int countType(HopperType type) {
        int count = 0;
        for (HopperData data : plugin.getHopperManager().getHoppers().values()) {
            if (data.getType() == type) count++;
        }
        return count;
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

    private void fillEmptySlots(Inventory inventory, Material material) {
        ItemStack filler = createGuiItem(material, " ");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) inventory.setItem(slot, filler);
        }
    }
}
