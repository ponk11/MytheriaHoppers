package me.mytheria.hoppers.listeners;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import me.mytheria.hoppers.MytheriaHoppers;
import me.mytheria.hoppers.hopper.HopperData;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class HopperConversionListener implements Listener {
    private static final Pattern REGION_NAME = Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca");

    private final MytheriaHoppers plugin;
    private final Queue<ChunkJob> chunks = new ArrayDeque<>();
    private final Queue<RegionJob> regions = new ConcurrentLinkedQueue<>();
    private final java.util.Set<String> queuedChunks = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<java.util.UUID, Set<Long>> scannedChunks = new ConcurrentHashMap<>();

    public HopperConversionListener(MytheriaHoppers plugin) {
        this.plugin = plugin;
    }

    public void start() {
        List<WorldFolder> worldFolders = new ArrayList<>();
        for (World world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) addChunk(world, chunk.getX(), chunk.getZ());
            worldFolders.add(new WorldFolder(world, world.getWorldFolder()));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (WorldFolder folder : worldFolders) findRegions(folder);
            }
        }.runTaskAsynchronously(plugin);

        int rate = Math.max(1, plugin.getConfig().getInt("conversion.chunks-per-second", 2));
        long interval = Math.max(1, 20 / rate);
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean roomForRegion;
                synchronized (chunks) {
                    roomForRegion = chunks.size() < 128;
                }
                if (roomForRegion) {
                    RegionJob region = regions.poll();
                    if (region != null) addRegionChunks(region);
                }
                ChunkJob job;
                synchronized (chunks) {
                    job = chunks.poll();
                }
                if (job != null) convert(job);
            }
        }.runTaskTimer(plugin, 1L, interval);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        addChunk(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    private void findRegions(WorldFolder worldFolder) {
        World world = worldFolder.world;
        Path folder = worldFolder.folder.toPath();
        if (!Files.isDirectory(folder)) return;
        try (Stream<Path> paths = Files.walk(folder)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                if (path.getParent() == null || !"region".equals(path.getParent().getFileName().toString())) return;
                Matcher matcher = REGION_NAME.matcher(path.getFileName().toString());
                if (!matcher.matches()) return;
                regions.add(new RegionJob(world, path.toFile(), Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
            });
        } catch (IOException e) {
            plugin.getLogger().warning("Could not scan saved chunks for " + world.getName() + ": " + e.getMessage());
        }
    }

    private void addRegionChunks(RegionJob region) {
        byte[] header = new byte[4096];
        try (RandomAccessFile file = new RandomAccessFile(region.file, "r")) {
            if (file.length() < header.length) return;
            file.readFully(header);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not read " + region.file.getName() + ": " + e.getMessage());
            return;
        }

        for (int index = 0; index < 1024; index++) {
            int offset = ((header[index * 4] & 0xff) << 16)
                    | ((header[index * 4 + 1] & 0xff) << 8)
                    | (header[index * 4 + 2] & 0xff);
            if (offset == 0) continue;
            int x = region.x * 32 + index % 32;
            int z = region.z * 32 + index / 32;
            addChunk(region.world, x, z);
        }
    }

    private void addChunk(World world, int x, int z) {
        String key = world.getUID() + ":" + x + ":" + z;
        Set<Long> scanned = scannedChunks.get(world.getUID());
        long coordinates = ((long) x << 32) | (z & 0xffffffffL);
        if (scanned != null && scanned.contains(coordinates)) return;
        if (!queuedChunks.add(key)) return;
        synchronized (chunks) {
            chunks.add(new ChunkJob(world, x, z, key));
        }
    }

    private void convert(ChunkJob job) {
        boolean wasLoaded = job.world.isChunkLoaded(job.x, job.z);
        try {
            Chunk chunk = job.world.getChunkAt(job.x, job.z);
            boolean changed = false;
            for (BlockState state : chunk.getTileEntities()) {
                if (state.getType() != Material.HOPPER) continue;
                if (!plugin.getHopperManager().getHoppers().containsKey(state.getLocation())) {
                    plugin.getHopperManager().getHoppers().put(state.getLocation(), new HopperData());
                    plugin.getHopperTask().indexHopper(state.getLocation());
                    changed = true;
                }
            }
            if (changed) plugin.getHopperManager().saveData();
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Could not check chunk " + job.x + ", " + job.z + " in " + job.world.getName() + ": " + e.getMessage());
        } finally {
            if (!wasLoaded) {
                try { job.world.unloadChunk(job.x, job.z, true); }
                catch (RuntimeException ignored) { }
            }
            queuedChunks.remove(job.key);
            scannedChunks.computeIfAbsent(job.world.getUID(), ignored -> ConcurrentHashMap.newKeySet())
                    .add(((long) job.x << 32) | (job.z & 0xffffffffL));
        }
    }

    private static class WorldFolder {
        private final World world;
        private final File folder;

        private WorldFolder(World world, File folder) {
            this.world = world;
            this.folder = folder;
        }
    }

    private static class ChunkJob {
        private final World world;
        private final int x;
        private final int z;
        private final String key;

        private ChunkJob(World world, int x, int z, String key) {
            this.world = world;
            this.x = x;
            this.z = z;
            this.key = key;
        }
    }

    private static class RegionJob {
        private final World world;
        private final File file;
        private final int x;
        private final int z;

        private RegionJob(World world, File file, int x, int z) {
            this.world = world;
            this.file = file;
            this.x = x;
            this.z = z;
        }
    }
}
