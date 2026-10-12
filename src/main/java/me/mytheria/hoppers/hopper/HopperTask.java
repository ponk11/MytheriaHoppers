package me.mytheria.hoppers.hopper;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import me.mytheria.hoppers.MytheriaHoppers;

public class HopperTask extends BukkitRunnable {

    private final MytheriaHoppers plugin;
    private int currentTick;

    public HopperTask(MytheriaHoppers plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        currentTick++;

        for (Map.Entry<Location, HopperData> entry : plugin.getHopperManager().getHoppers().entrySet()) {
            Location location = entry.getKey();
            HopperData data = entry.getValue();

            if (!location.getChunk().isLoaded()) {
                continue;
            }

            Block block = location.getBlock();
            if (block.getType() != Material.HOPPER) {
                continue;
            }

            if (block.isBlockPowered() || block.isBlockIndirectlyPowered()) {
                continue;
            }

            int speedLevel = data.getSpeedLevel();
            int transferTicks = plugin.getConfig().getInt(
                    "speed-upgrades." + speedLevel + ".transfer-ticks",
                    plugin.getConfig().getInt("settings.default-transfer-ticks", 8)
            );

            if (currentTick - data.getLastTransferTick() < transferTicks) {
                continue;
            }

            data.setLastTransferTick(currentTick);

            int rangeLevel = data.getRangeLevel();
            int range = 1;
            if (rangeLevel > 0) {
                range = plugin.getConfig().getInt(
                        "range-upgrades." + rangeLevel + ".range",
                        1
                );
            }

            int amountToTransfer = plugin.getConfig().getInt(
                    "speed-upgrades." + speedLevel + ".transfer-amount",
                    plugin.getConfig().getInt("settings.default-transfer-amount", 1)
            );

            org.bukkit.block.Hopper hopper = (org.bukkit.block.Hopper) block.getState();
            // Paper exposes this on newer API versions; reflection keeps the plugin
            // loadable on older Bukkit/Paper servers where the method is absent.
            boolean customCooldown = false;
            try {
                hopper.getClass().getMethod("setTransferCooldown", int.class)
                        .invoke(hopper, Math.max(1, transferTicks));
                customCooldown = true;
            } catch (ReflectiveOperationException ignored) {
                // Older APIs need the compatibility transfer below.
            }
            hopper.update(true, false);

            collectItems(location, data, range, amountToTransfer);
            if (!customCooldown) {
                transferOut(block, Math.max(1, amountToTransfer));
            }
        }
    }

    private void transferOut(Block block, int amount) {
        org.bukkit.block.data.BlockData blockData = block.getBlockData();
        if (!(blockData instanceof org.bukkit.block.data.type.Hopper)) return;

        BlockFace facing = ((org.bukkit.block.data.type.Hopper) blockData).getFacing();
        Block destination = block.getRelative(facing);
        if (!(destination.getState() instanceof InventoryHolder)) return;

        Inventory source = ((org.bukkit.block.Hopper) block.getState()).getInventory();
        Inventory target = ((InventoryHolder) destination.getState()).getInventory();
        for (int slot = 0; slot < source.getSize(); slot++) {
            ItemStack stack = source.getItem(slot);
            if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) continue;

            int moving = Math.min(stack.getAmount(), amount);
            ItemStack transfer = stack.clone();
            transfer.setAmount(moving);
            Map<Integer, ItemStack> leftovers = target.addItem(transfer);
            int remaining = 0;
            for (ItemStack leftover : leftovers.values()) remaining += leftover.getAmount();
            int moved = moving - remaining;
            if (moved <= 0) continue;

            if (moved >= stack.getAmount()) source.setItem(slot, null);
            else {
                stack.setAmount(stack.getAmount() - moved);
                source.setItem(slot, stack);
            }
            return;
        }
    }

    private void collectItems(Location location, HopperData data, int range, int maxAmount) {
        if (location.getWorld() == null) {
            return;
        }

        Inventory inventory = ((org.bukkit.block.Hopper) location.getBlock().getState()).getInventory();

        for (Entity entity : location.getWorld().getNearbyEntities(location, range, range, range)) {
            if (!(entity instanceof Item)) {
                continue;
            }
            Item item = (Item) entity;

            if (!item.isValid() || item.isDead()) {
                continue;
            }

            ItemStack stack = item.getItemStack();
            if (stack.getType() == Material.AIR || stack.getAmount() <= 0) {
                continue;
            }

            if (!data.isAllowedByFilter(stack.getType())) {
                continue;
            }

            int transferCount = Math.min(stack.getAmount(), maxAmount);
            ItemStack stackToInsert = stack.clone();
            stackToInsert.setAmount(transferCount);

            Map<Integer, ItemStack> leftover = inventory.addItem(stackToInsert);

            int successfullyAdded = transferCount - (leftover.isEmpty() ? 0 : leftover.values().iterator().next().getAmount());

            if (successfullyAdded > 0) {
                if (stack.getAmount() <= successfullyAdded) {
                    item.remove();
                } else {
                    stack.setAmount(stack.getAmount() - successfullyAdded);
                    item.setItemStack(stack);
                }
                break;
            }
        }
    }
}
