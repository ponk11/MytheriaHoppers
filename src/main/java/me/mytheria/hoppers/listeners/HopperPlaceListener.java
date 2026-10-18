package me.mytheria.hoppers.listeners;

import me.mytheria.hoppers.MytheriaHoppers;
import me.mytheria.hoppers.hopper.HopperData;
import me.mytheria.hoppers.hopper.HopperKeys;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class HopperPlaceListener implements Listener {

    private final MytheriaHoppers plugin;

    public HopperPlaceListener(MytheriaHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {

        if (event.getBlock().getType() != Material.HOPPER) {
            return;
        }

        ItemStack item =
                event.getItemInHand();

        int speedLevel = 0;
        int rangeLevel = 0;

        if (item.hasItemMeta()) {

            if (item.getItemMeta()
                    .getPersistentDataContainer()
                    .has(
                            HopperKeys.speedLevel(),
                            PersistentDataType.INTEGER
                    )) {

                speedLevel =
                        item.getItemMeta()
                                .getPersistentDataContainer()
                                .get(
                                        HopperKeys.speedLevel(),
                                        PersistentDataType.INTEGER
                                );
            }

            if (item.getItemMeta()
                    .getPersistentDataContainer()
                    .has(
                            HopperKeys.rangeLevel(),
                            PersistentDataType.INTEGER
                    )) {

                rangeLevel =
                        item.getItemMeta()
                                .getPersistentDataContainer()
                                .get(
                                        HopperKeys.rangeLevel(),
                                        PersistentDataType.INTEGER
                                );
            }
        }

        HopperData data =
                new HopperData();

        data.setSpeedLevel(speedLevel);
        data.setRangeLevel(rangeLevel);

        plugin.getHopperManager().getHoppers().put(event.getBlock().getLocation(), data);
        plugin.getHopperTask().indexHopper(event.getBlock().getLocation());

        plugin.getHopperManager().saveData();
    }
}
