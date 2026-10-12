package me.mytheria.hoppers.listeners;

import me.mytheria.hoppers.MytheriaHoppers;
import me.mytheria.hoppers.hopper.HopperData;
import org.bukkit.Location;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.InventoryHolder;

/** Enforces Mytheria item filters for Bukkit's native hopper pickup path. */
public class HopperFilterListener implements Listener {
    private final MytheriaHoppers plugin;

    public HopperFilterListener(MytheriaHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperPickup(InventoryPickupItemEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof Hopper)) return;

        Location location = ((Hopper) holder).getLocation();
        HopperData data = plugin.getHopperManager().getHoppers().get(location);
        if (data == null || data.isAllowedByFilter(event.getItem().getItemStack().getType())) return;

        event.setCancelled(true);
    }
}
