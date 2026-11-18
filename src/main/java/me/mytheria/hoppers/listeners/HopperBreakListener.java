package me.mytheria.hoppers.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import me.mytheria.hoppers.MytheriaHoppers;
import me.mytheria.hoppers.hopper.HopperData;
import me.mytheria.hoppers.hopper.HopperItem;

public class HopperBreakListener implements Listener {

    private final MytheriaHoppers plugin;

    public HopperBreakListener(MytheriaHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {

        Block block = event.getBlock();

        if (block.getType() != Material.HOPPER) {
            return;
        }

        HopperData data = plugin.getHopperManager().getHoppers().get(block.getLocation());

        if (data == null) {
            return;
        }

        int speedLevel = data.getSpeedLevel();
        int rangeLevel = data.getRangeLevel();
        ItemStack[] contents = ((org.bukkit.block.Hopper) block.getState()).getInventory().getContents();

        event.setDropItems(false);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType() != Material.AIR) return;

            plugin.getHopperManager().remove(block.getLocation());
            plugin.getHopperTask().unindexHopper(block.getLocation());
            plugin.getHopperManager().saveData();

            for (ItemStack content : contents) {
                if (content != null && content.getType() != Material.AIR && content.getAmount() > 0) {
                    block.getWorld().dropItemNaturally(block.getLocation(), content);
                }
            }
            block.getWorld().dropItemNaturally(
                    block.getLocation(),
                    HopperItem.create(data.getType(), speedLevel, rangeLevel)
            );
        });
    }
}
