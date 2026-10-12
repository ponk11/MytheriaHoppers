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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;

import me.mytheria.hoppers.MytheriaHoppers;
import me.mytheria.hoppers.hopper.HopperData;

public class HopperManager {

    private final MytheriaHoppers plugin;
    private final Map<Location, HopperData> hopperDataMap = new HashMap<>();
    private final File dataFile;
    private final File backupFolder;
    private FileConfiguration dataConfig;
    private boolean dataLoadFailed;

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

    public void remove(Location location) {
        hopperDataMap.remove(location);
    }

    public void removeData(Location location) {
        remove(location);
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
            String[] parts = key.split(",");
            if (parts.length != 4) continue;

            String worldName = parts[0];
            int x;
            int y;
            int z;
            try {
                x = Integer.parseInt(parts[1]);
                y = Integer.parseInt(parts[2]);
                z = Integer.parseInt(parts[3]);
            } catch (NumberFormatException ignored) {
                plugin.getLogger().warning("Skipping invalid saved hopper location: " + key);
                continue;
            }

            if (Bukkit.getWorld(worldName) == null) continue;

            Location loc = new Location(Bukkit.getWorld(worldName), x, y, z);
            HopperData data = new HopperData();
            data.setSpeedLevel(dataConfig.getInt("hoppers." + key + ".speed", 0));
            data.setRangeLevel(dataConfig.getInt("hoppers." + key + ".range", 0));
            data.setFilterEnabled(dataConfig.getBoolean("hoppers." + key + ".filterEnabled", false));
            data.setFilterUnlocked(dataConfig.getBoolean("hoppers." + key + ".filterUnlocked", false));
            data.setUnlockedFilterSlots(dataConfig.getInt("hoppers." + key + ".unlockedFilterSlots", 0));
            
            String selectedItem = dataConfig.getString("hoppers." + key + ".selectedFilterItem");
            java.util.Set<org.bukkit.Material> materials = new java.util.HashSet<>();
            for (String name : dataConfig.getStringList("hoppers." + key + ".filteredMaterials")) {
                try { materials.add(org.bukkit.Material.valueOf(name)); } catch (IllegalArgumentException ignored) { }
            }
            if (selectedItem != null && !selectedItem.isEmpty()) {
                try {
                    materials.add(org.bukkit.Material.valueOf(selectedItem));
                } catch (IllegalArgumentException e) {
                    // Invalid material, skip
                }
            }
            data.setFilteredMaterials(materials);
            data.setFilterUnlocked(dataConfig.getBoolean("hoppers." + key + ".filterUnlocked", false));
            data.setUnlockedFilterSlots(Math.max(data.getUnlockedFilterSlots(), dataConfig.getInt("hoppers." + key + ".unlockedFilterSlots", materials.size())));

            hopperDataMap.put(loc, data);
        }
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
                // Try the next most recent backup.
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
