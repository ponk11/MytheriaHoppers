package me.mytheria.hoppers.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

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

        plugin.getHopperManager().remove(block.getLocation());
        plugin.getHopperManager().saveData();

        event.setDropItems(false);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType() == Material.HOPPER) {
                block.setType(Material.AIR);
            }
        });

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType() == Material.AIR) {
                block.getWorld().dropItemNaturally(
                        block.getLocation(),
                        HopperItem.create(speedLevel, rangeLevel)
                );
            }
        });
    }
}
