package me.mytheria.hoppers.managers;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import me.mytheria.hoppers.MytheriaHoppers;
import me.mytheria.hoppers.hopper.HopperData;

public class HopperManager {

    private final MytheriaHoppers plugin;
    private final Map<Location, HopperData> hopperDataMap = new HashMap<>();
    private final File dataFile;
    private final File backupFolder;
    private final Map<String, Map<String, HopperData>> pendingWorldData = new HashMap<>();
    private FileConfiguration dataConfig;
    private boolean dataLoadFailed;
    private RemovalSnapshot lastRemoval;

    public HopperManager(MytheriaHoppers plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.backupFolder = new File(plugin.getDataFolder(), "backups");
        loadData();
    }

    public HopperData getData(Location location) {
        return hopperDataMap.computeIfAbsent(location, loc -> new HopperData());
    }

    public Map<Location, HopperData> getHoppers() {
        return hopperDataMap;
    }

    public Map<Location, HopperData> getHopperDataMap() {
        return hopperDataMap;
    }

    public void loadWorldData(World world) {
        Map<String, HopperData> worldData = pendingWorldData.remove(world.getName());
        if (worldData == null) return;

        for (Map.Entry<String, HopperData> entry : worldData.entrySet()) {
            String[] coordinates = entry.getKey().split(",");
            if (coordinates.length != 3) continue;
            try {
                Location location = new Location(
                        world,
                        Integer.parseInt(coordinates[0]),
                        Integer.parseInt(coordinates[1]),
                        Integer.parseInt(coordinates[2])
                );
                hopperDataMap.put(location, entry.getValue());
                if (plugin.getHopperTask() != null) plugin.getHopperTask().indexHopper(location);
            } catch (NumberFormatException ignored) {
                plugin.getLogger().warning("Skipping invalid saved hopper location in world " + world.getName() + ".");
            }
        }
    }

    public void remove(Location location) {
        hopperDataMap.remove(location);
    }

    public void removeData(Location location) {
        remove(location);
    }

    public boolean canUndoLastRemoval() {
        return lastRemoval != null && !lastRemoval.hoppers.isEmpty();
    }

    public int removeWorld(String worldName) {
        RemovalSnapshot snapshot = new RemovalSnapshot();
        java.util.Iterator<Map.Entry<Location, HopperData>> iterator = hopperDataMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Location, HopperData> entry = iterator.next();
            Location location = entry.getKey();
            if (location.getWorld() == null || !location.getWorld().getName().equals(worldName)) continue;

            Block block = location.getBlock();
            BlockData blockData = block.getBlockData().clone();
            ItemStack[] contents = null;
            BlockState state = block.getState();
            if (state instanceof Hopper) {
                ItemStack[] original = ((Hopper) state).getInventory().getContents();
                contents = new ItemStack[original.length];
                for (int i = 0; i < original.length; i++) {
                    contents[i] = original[i] == null ? null : original[i].clone();
                }
            }
            snapshot.hoppers.add(new RemovedHopper(location, copyData(entry.getValue()), blockData, contents));
            iterator.remove();
            if (plugin.getHopperTask() != null) plugin.getHopperTask().unindexHopper(location);
            if (block.getType() == Material.HOPPER) block.setType(Material.AIR, false);
        }
        if (!snapshot.hoppers.isEmpty()) lastRemoval = snapshot;
        return snapshot.hoppers.size();
    }

    public int undoLastRemoval() {
        if (!canUndoLastRemoval()) return 0;
        int restored = 0;
        for (RemovedHopper removed : lastRemoval.hoppers) {
            Block block = removed.location.getBlock();
            if (block.getType() != Material.AIR && block.getType() != Material.HOPPER) continue;
            if (block.getType() == Material.AIR) block.setBlockData(removed.blockData, false);
            BlockState state = block.getState();
            if (state instanceof Hopper && removed.contents != null) {
                ((Hopper) state).getInventory().setContents(removed.contents);
                state.update(true, false);
            }
            hopperDataMap.put(removed.location, copyData(removed.data));
            if (plugin.getHopperTask() != null) plugin.getHopperTask().indexHopper(removed.location);
            restored++;
        }
        lastRemoval = null;
        return restored;
    }

    private HopperData copyData(HopperData source) {
        HopperData copy = new HopperData();
        copy.setSpeedLevel(source.getSpeedLevel());
        copy.setRangeLevel(source.getRangeLevel());
        copy.setLastTransferTick(source.getLastTransferTick());
        copy.setFilterUnlocked(source.isFilterUnlocked());
        copy.setUnlockedFilterSlots(source.getUnlockedFilterSlots());
        copy.setFilteredMaterials(new java.util.HashSet<>(source.getFilteredMaterials()));
        copy.setFilterEnabled(source.isFilterEnabled());
        return copy;
    }

    private static class RemovalSnapshot {
        private final java.util.List<RemovedHopper> hoppers = new java.util.ArrayList<>();
    }

    private static class RemovedHopper {
        private final Location location;
        private final HopperData data;
        private final BlockData blockData;
        private final ItemStack[] contents;

        private RemovedHopper(Location location, HopperData data, BlockData blockData, ItemStack[] contents) {
            this.location = location;
            this.data = data;
            this.blockData = blockData;
            this.contents = contents;
        }
    }

    public void saveData() {
        if (dataLoadFailed) {
            plugin.getLogger().severe("Skipping data save because the existing hopper file could not be recovered.");
            return;
        }
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().severe("Could not create the plugin data folder.");
            return;
        }
        dataConfig = new YamlConfiguration();

        for (Map.Entry<Location, HopperData> entry : hopperDataMap.entrySet()) {
            Location loc = entry.getKey();
            if (loc == null || loc.getWorld() == null) continue;
            String path = "hoppers." + loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            HopperData data = entry.getValue();
            dataConfig.set(path + ".speed", data.getSpeedLevel());
            dataConfig.set(path + ".range", data.getRangeLevel());
            dataConfig.set(path + ".filterEnabled", data.isFilterEnabled());
            dataConfig.set(path + ".filterUnlocked", data.isFilterUnlocked());
            dataConfig.set(path + ".unlockedFilterSlots", data.getUnlockedFilterSlots());
            if (data.getSelectedFilterItem() != null) {
                dataConfig.set(path + ".selectedFilterItem", data.getSelectedFilterItem().toString());
            }
            dataConfig.set(path + ".filteredMaterials", data.getFilteredMaterials().stream().map(Enum::name).collect(Collectors.toList()));
        }
        for (Map.Entry<String, Map<String, HopperData>> worldEntry : pendingWorldData.entrySet()) {
            for (Map.Entry<String, HopperData> hopperEntry : worldEntry.getValue().entrySet()) {
                String path = "hoppers." + worldEntry.getKey() + "," + hopperEntry.getKey();
                writeHopperData(path, hopperEntry.getValue());
            }
        }

        File temporaryFile = new File(plugin.getDataFolder(), "data.yml.tmp");
        try {
            cleanOldBackups();
            backupCurrentData();
        } catch (IOException e) {
            plugin.getLogger().warning("Could not create a data backup: " + e.getMessage());
        }
        try {
            dataConfig.save(temporaryFile);
            try {
                Files.move(temporaryFile.toPath(), dataFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporaryFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save hopper data: " + e.getMessage());
            try { Files.deleteIfExists(temporaryFile.toPath()); } catch (IOException ignored) { }
        }
    }

    public void loadData() {
        cleanOldBackups();
        if (!dataFile.exists()) {
            return;
        }
        try {
            dataConfig = readDataFile(dataFile);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().warning("data.yml could not be read. Trying the latest backup.");
            dataConfig = restoreLatestBackup();
            if (dataConfig == null) {
                dataLoadFailed = true;
                plugin.getLogger().severe("No readable hopper data backup was found; starting with empty hopper data.");
                return;
            }
            dataLoadFailed = false;
        }
        if (!dataConfig.contains("hoppers")) {
            return;
        }

        for (String key : dataConfig.getConfigurationSection("hoppers").getKeys(false)) {
            int zSeparator = key.lastIndexOf(',');
            int ySeparator = zSeparator < 0 ? -1 : key.lastIndexOf(',', zSeparator - 1);
            int xSeparator = ySeparator < 0 ? -1 : key.lastIndexOf(',', ySeparator - 1);
            if (xSeparator < 0 || ySeparator < 0 || zSeparator < 0) continue;

            String worldName = key.substring(0, xSeparator);
            int x;
            int y;
            int z;
            try {
                x = Integer.parseInt(key.substring(xSeparator + 1, ySeparator));
                y = Integer.parseInt(key.substring(ySeparator + 1, zSeparator));
                z = Integer.parseInt(key.substring(zSeparator + 1));
            } catch (NumberFormatException ignored) {
                plugin.getLogger().warning("Skipping invalid saved hopper location: " + key);
                continue;
            }

            HopperData data = readHopperData(key);
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                pendingWorldData.computeIfAbsent(worldName, ignored -> new HashMap<>())
                        .put(x + "," + y + "," + z, data);
                continue;
            }

            hopperDataMap.put(new Location(world, x, y, z), data);
        }
    }

    private HopperData readHopperData(String key) {
        HopperData data = new HopperData();
        data.setSpeedLevel(dataConfig.getInt("hoppers." + key + ".speed", 0));
        data.setRangeLevel(dataConfig.getInt("hoppers." + key + ".range", 0));
        data.setFilterEnabled(dataConfig.getBoolean("hoppers." + key + ".filterEnabled", false));
        data.setFilterUnlocked(dataConfig.getBoolean("hoppers." + key + ".filterUnlocked", false));
        data.setUnlockedFilterSlots(dataConfig.getInt("hoppers." + key + ".unlockedFilterSlots", 0));

        java.util.Set<Material> materials = new java.util.HashSet<>();
        for (String name : dataConfig.getStringList("hoppers." + key + ".filteredMaterials")) {
            try { materials.add(Material.valueOf(name)); } catch (IllegalArgumentException ignored) { }
        }
        String selectedItem = dataConfig.getString("hoppers." + key + ".selectedFilterItem");
        if (selectedItem != null && !selectedItem.isEmpty()) {
            try { materials.add(Material.valueOf(selectedItem)); } catch (IllegalArgumentException ignored) { }
        }
        data.setFilteredMaterials(materials);
        data.setFilterUnlocked(dataConfig.getBoolean("hoppers." + key + ".filterUnlocked", false));
        data.setUnlockedFilterSlots(Math.max(data.getUnlockedFilterSlots(),
                dataConfig.getInt("hoppers." + key + ".unlockedFilterSlots", materials.size())));
        return data;
    }

    private void writeHopperData(String path, HopperData data) {
        dataConfig.set(path + ".speed", data.getSpeedLevel());
        dataConfig.set(path + ".range", data.getRangeLevel());
        dataConfig.set(path + ".filterEnabled", data.isFilterEnabled());
        dataConfig.set(path + ".filterUnlocked", data.isFilterUnlocked());
        dataConfig.set(path + ".unlockedFilterSlots", data.getUnlockedFilterSlots());
        if (data.getSelectedFilterItem() != null) {
            dataConfig.set(path + ".selectedFilterItem", data.getSelectedFilterItem().toString());
        }
        dataConfig.set(path + ".filteredMaterials", data.getFilteredMaterials().stream()
                .map(Enum::name).collect(Collectors.toList()));
    }

    private FileConfiguration readDataFile(File file) throws IOException, InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.load(file);
        if (!configuration.getKeys(false).isEmpty() && !configuration.isConfigurationSection("hoppers")) {
            throw new IOException("Missing hoppers section");
        }
        return configuration;
    }

    private void backupCurrentData() throws IOException {
        if (!dataFile.isFile() || dataFile.length() == 0 || !isReadableData(dataFile)) return;
        if (!backupFolder.exists() && !backupFolder.mkdirs()) {
            throw new IOException("Could not create backups folder");
        }

        File backup = new File(backupFolder, "data-" + LocalDate.now() + ".yml");
        if (!backup.exists() || !isReadableData(backup)) {
            Files.copy(dataFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean isReadableData(File file) {
        try {
            readDataFile(file);
            return true;
        } catch (IOException | InvalidConfigurationException e) {
            return false;
        }
    }

    private FileConfiguration restoreLatestBackup() {
        File[] backups = backupFolder.listFiles((dir, name) -> name.matches("data-[0-9]{4}-[0-9]{2}-[0-9]{2}\\.yml"));
        if (backups == null) return null;
        Arrays.sort(backups, (first, second) -> Long.compare(second.lastModified(), first.lastModified()));

        for (File backup : backups) {
            try {
                FileConfiguration recovered = readDataFile(backup);
                Files.copy(backup.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().warning("Restored hopper data from " + backup.getName() + ".");
                return recovered;
            } catch (IOException | InvalidConfigurationException ignored) {
            }
        }
        return null;
    }

    private void cleanOldBackups() {
        File[] backups = backupFolder.listFiles((dir, name) -> name.matches("data-[0-9]{4}-[0-9]{2}-[0-9]{2}\\.yml"));
        if (backups == null) return;
        long cutoff = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L;
        for (File backup : backups) {
            if (backup.lastModified() < cutoff && !backup.delete()) {
                plugin.getLogger().warning("Could not delete expired data backup " + backup.getName() + ".");
            }
        }
    }
}
