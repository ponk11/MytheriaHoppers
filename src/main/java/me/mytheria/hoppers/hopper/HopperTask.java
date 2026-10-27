package me.mytheria.hoppers.hopper;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import me.mytheria.hoppers.MytheriaHoppers;

public class HopperTask extends BukkitRunnable implements Listener {

    private final MytheriaHoppers plugin;
    private int currentTick;
    private final int[] transferTicks = new int[5];
    private final int[] transferAmounts = new int[5];
    private final int[] ranges = new int[5];
    private Method setTransferCooldown;
    private Class<?> cooldownClass;
    private int tickStep = 2;
    private final Map<String, Set<Location>> hoppersByChunk = new ConcurrentHashMap<>();
    private final Set<String> loadedChunks = ConcurrentHashMap.newKeySet();
    private final PriorityQueue<ScheduledHopper> scheduledHoppers =
            new PriorityQueue<>(Comparator.comparingInt(entry -> entry.dueTick));

    public HopperTask(MytheriaHoppers plugin) {
        this.plugin = plugin;
        refreshConfiguration();
        for (Location location : plugin.getHopperManager().getHoppers().keySet()) {
            indexHopper(location);
        }
    }

    public void refreshConfiguration() {
        int defaultTicks = Math.max(1, plugin.getConfig().getInt("settings.default-transfer-ticks", 8));
        int defaultAmount = Math.max(1, plugin.getConfig().getInt("settings.default-transfer-amount", 1));
        for (int level = 0; level < transferTicks.length; level++) {
            transferTicks[level] = Math.max(1, plugin.getConfig().getInt("speed-upgrades." + level + ".transfer-ticks", defaultTicks));
            transferAmounts[level] = Math.max(1, plugin.getConfig().getInt("speed-upgrades." + level + ".transfer-amount", defaultAmount));
            ranges[level] = Math.max(1, plugin.getConfig().getInt("range-upgrades." + level + ".range", 1));
        }
        tickStep = 2;
        scheduledHoppers.clear();
        for (Map.Entry<String, Set<Location>> entry : hoppersByChunk.entrySet()) {
            for (Location location : entry.getValue()) schedule(location, currentTick);
        }
    }

    @Override
    public void run() {
        currentTick += tickStep;

        while (!scheduledHoppers.isEmpty() && scheduledHoppers.peek().dueTick <= currentTick) {
            ScheduledHopper scheduled = scheduledHoppers.poll();
            Location location = scheduled.location;
            String chunkKey = chunkKey(location);
            Set<Location> locations = hoppersByChunk.get(chunkKey);
            if (locations == null || !locations.contains(location)) continue;

            HopperData data = plugin.getHopperManager().getHoppers().get(location);
            if (data == null) continue;

            schedule(location, currentTick);
            if (!loadedChunks.contains(chunkKey)) continue;

            Block block = location.getBlock();
            if (block.getType() != Material.HOPPER) continue;
            if (block.isBlockPowered() || block.isBlockIndirectlyPowered()) continue;

            int speedLevel = clampLevel(data.getSpeedLevel());
            int configuredTransferTicks = transferTicks[speedLevel];
            data.setLastTransferTick(currentTick);

            int range = ranges[clampLevel(data.getRangeLevel())];
            int amountToTransfer = transferAmounts[speedLevel];

            org.bukkit.block.Hopper hopper = (org.bukkit.block.Hopper) block.getState();
            if (cooldownClass != hopper.getClass()) {
                cooldownClass = hopper.getClass();
                try {
                    setTransferCooldown = cooldownClass.getMethod("setTransferCooldown", int.class);
                } catch (ReflectiveOperationException ignored) {
                    setTransferCooldown = null;
                }
            }
            boolean customCooldown = setTransferCooldown != null;
            if (customCooldown) {
                try {
                    setTransferCooldown.invoke(hopper, configuredTransferTicks);
                } catch (ReflectiveOperationException ignored) {
                    customCooldown = false;
                }
            }
            hopper.update(true, false);

            collectItems(location, data, hopper.getInventory(), range, amountToTransfer);
            if (!customCooldown) transferOut(block, Math.max(1, amountToTransfer));
        }
    }

    public void indexHopper(Location location) {
        if (location == null || location.getWorld() == null) return;
        String key = chunkKey(location);
        if (hoppersByChunk.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(location)) {
            schedule(location, currentTick);
        }
        if (location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) loadedChunks.add(key);
    }

    public void unindexHopper(Location location) {
        if (location == null || location.getWorld() == null) return;
        Set<Location> locations = hoppersByChunk.get(chunkKey(location));
        if (locations != null && locations.remove(location) && locations.isEmpty()) {
            hoppersByChunk.remove(chunkKey(location), locations);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        String key = chunkKey(event.getWorld().getUID().toString(), event.getChunk().getX(), event.getChunk().getZ());
        loadedChunks.add(key);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        loadedChunks.remove(chunkKey(event.getWorld().getUID().toString(), event.getChunk().getX(), event.getChunk().getZ()));
    }

    private String chunkKey(Location location) {
        return chunkKey(location.getWorld().getUID().toString(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private String chunkKey(String worldId, int x, int z) {
        return worldId + ':' + x + ':' + z;
    }

    private int clampLevel(int level) {
        return Math.max(0, Math.min(level, transferTicks.length - 1));
    }

    private void schedule(Location location, int fromTick) {
        HopperData data = plugin.getHopperManager().getHoppers().get(location);
        if (data == null) return;
        int interval = transferTicks[clampLevel(data.getSpeedLevel())];
        int dueTick = Math.max(fromTick + tickStep, data.getLastTransferTick() + interval);
        scheduledHoppers.add(new ScheduledHopper(location, dueTick));
    }

    private static final class ScheduledHopper {
        private final Location location;
        private final int dueTick;

        private ScheduledHopper(Location location, int dueTick) {
            this.location = location;
            this.dueTick = dueTick;
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

    private void collectItems(Location location, HopperData data, Inventory inventory, int range, int maxAmount) {
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
