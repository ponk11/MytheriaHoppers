package me.mytheria.hoppers.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import me.mytheria.hoppers.MytheriaHoppers;
import me.mytheria.hoppers.gui.HopperGUI;
import me.mytheria.hoppers.hopper.HopperData;

public class GUIListener implements Listener {

    private final MytheriaHoppers plugin;

    public GUIListener(MytheriaHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;

        String title = event.getView().getTitle();
        
        if (title != null && (title.contains("Hopper Upgrades") || title.contains("Item Filter Selection"))) {
            
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                if (title.contains("Hopper Upgrades") && event.getClickedInventory().equals(event.getInventory())) {
                    event.setCancelled(true);
                    if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                        handleGuiClick(player, event, title);
                    }
                } 
                else if (title.contains("Item Filter Selection")) {
                    Location hopperLocation = HopperGUI.openHopperLocations.get(player.getUniqueId());
                    if (hopperLocation != null) {
                        HopperData data = plugin.getHopperManager().getData(hopperLocation);
                        if (data != null && data.isFilterUnlocked()) {
                            if (!event.getClickedInventory().equals(event.getInventory()) && 
                                event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                                return;
                            }
                        }
                    }
                    
                    if (event.getClickedInventory().equals(event.getInventory())) {
                        event.setCancelled(true);
                        if (event.getCurrentItem() != null && event.getCurrentItem().hasItemMeta()) {
                            handleGuiClick(player, event, title);
                        }
                    }
                }
            }
        }
    }

    private void handleGuiClick(Player player, InventoryClickEvent event, String title) {
        Location hopperLocation = HopperGUI.openHopperLocations.get(player.getUniqueId());
        if (hopperLocation == null) return;

        HopperData data = plugin.getHopperManager().getData(hopperLocation);
        if (data == null) return;

        int slot = event.getRawSlot();

        if (title.contains("Hopper Upgrades")) {
            if (slot == 11) {
                if (plugin.getUpgradeManager().upgradeSpeed(player, data)) {
                    player.sendMessage("§aSpeed upgrade successful!");
                    plugin.getHopperManager().saveData();
                } else {
                    player.sendMessage("§cFailed to upgrade speed. Check cost or max level.");
                }
                plugin.getHopperGUI().openGUI(player, hopperLocation);
            } else if (slot == 13) {
                if (plugin.getUpgradeManager().upgradeRange(player, data)) {
                    player.sendMessage("§aRange upgrade successful!");
                    plugin.getHopperManager().saveData();
                } else {
                    player.sendMessage("§cFailed to upgrade range. Check cost or max level.");
                }
                plugin.getHopperGUI().openGUI(player, hopperLocation);
            } else if (slot == 15) {
                plugin.getHopperGUI().openFilterGUI(player, hopperLocation);
            }
        }
        else if (title.contains("Item Filter Selection")) {
            if (slot == 13 && !data.isFilterUnlocked()) {
                if (!data.isFilterUnlocked()) {
                    if (plugin.getUpgradeManager().unlockFilter(player, data)) {
                        player.sendMessage("§aFilter unlocked! You can now select 1 item to filter.");
                        plugin.getHopperManager().saveData();
                    } else {
                        player.sendMessage("§cFailed to unlock filter. You need $5,000,000.");
                    }
                }
                plugin.getHopperGUI().openFilterGUI(player, hopperLocation);
            } else if (slot == 26 && data.isFilterUnlocked()) {
                data.getFilteredMaterials().clear();
                data.setFilterEnabled(false);
                data.setSelectedFilterItem(null);
                plugin.getHopperManager().saveData();
                plugin.getHopperGUI().openFilterGUI(player, hopperLocation);
            }
            else if (data.isFilterUnlocked() && slot >= 0 && slot < Math.min(18, data.getUnlockedFilterSlots())) {
                ItemStack cursor = event.getCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    if (!data.getFilteredMaterials().contains(cursor.getType()) &&
                            data.getFilteredMaterials().size() >= data.getUnlockedFilterSlots()) {
                        player.sendMessage("§cUnlock another filter slot before adding more items.");
                        return;
                    }
                    data.addFilterMaterial(cursor.getType());
                    data.setFilterEnabled(true);
                    player.sendMessage("§aAdded " + cursor.getType().name() + " to the hopper filter.");
                    plugin.getHopperManager().saveData();
                    plugin.getHopperGUI().openFilterGUI(player, hopperLocation);
                } else if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
                    data.removeFilterMaterial(event.getCurrentItem().getType());
                    data.setFilterEnabled(!data.getFilteredMaterials().isEmpty());
                    plugin.getHopperManager().saveData();
                    plugin.getHopperGUI().openFilterGUI(player, hopperLocation);
                }
            }
            else if (data.isFilterUnlocked() && slot >= 18 && slot <= 25 && data.getUnlockedFilterSlots() < 18) {
                if (plugin.getUpgradeManager().unlockFilterSlot(player, data)) {
                    player.sendMessage("§aFilter slot unlocked! You now have " + data.getUnlockedFilterSlots() + " slots.");
                    plugin.getHopperManager().saveData();
                } else {
                    player.sendMessage("§cFailed to unlock slot. You need $250,000 or slots are maxed out.");
                }
                plugin.getHopperGUI().openFilterGUI(player, hopperLocation);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        
        if (title != null && title.contains("Item Filter Selection") &&
                event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize())) {
            event.setCancelled(true);
        }
    }
}
