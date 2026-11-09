package me.mytheria.hoppers;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import me.mytheria.hoppers.economy.EconomyManager;
import me.mytheria.hoppers.economy.VaultEconomyProvider;
import me.mytheria.hoppers.gui.HopperGUI;
import me.mytheria.hoppers.gui.UpgradeManager;
import me.mytheria.hoppers.hopper.HopperTask;
import me.mytheria.hoppers.listeners.GUIListener;
import me.mytheria.hoppers.listeners.HopperBlockListener;
import me.mytheria.hoppers.listeners.HopperBreakListener;
import me.mytheria.hoppers.listeners.HopperInteractListener;
import me.mytheria.hoppers.listeners.HopperPlaceListener;
import me.mytheria.hoppers.listeners.HopperFilterListener;
import me.mytheria.hoppers.listeners.HopperConversionListener;
import me.mytheria.hoppers.managers.HopperManager;
import me.mytheria.hoppers.commands.HopperCommand;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import java.util.Iterator;
import me.mytheria.hoppers.hopper.HopperData;
import me.mytheria.hoppers.hopper.HopperType;

public class MytheriaHoppers extends JavaPlugin {

    private static MytheriaHoppers instance;
    private Economy economy;
    private EconomyManager economyManager;
    private HopperManager hopperManager;
    private UpgradeManager upgradeManager;
    private HopperGUI hopperGUI;
    private HopperTask hopperTask;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        updateHopperRecipe();

        if (!setupEconomy()) {
            getLogger().warning("Vault economy not found! Economy-based upgrades will be disabled.");
        }

        this.hopperManager = new HopperManager(this);
        if (!getConfig().getBoolean("hopper-types.enabled", true)) {
            disableTypedHoppers();
        }
        this.upgradeManager = new UpgradeManager(this);
        this.hopperGUI = new HopperGUI(this);

        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new HopperInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new HopperBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new HopperBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new HopperPlaceListener(this), this);
        getServer().getPluginManager().registerEvents(new HopperFilterListener(this), this);
        HopperConversionListener conversionListener = new HopperConversionListener(this);
        getServer().getPluginManager().registerEvents(conversionListener, this);
        conversionListener.start();

        if (getCommand("mytheriahoppers") != null) {
            HopperCommand hopperCommand = new HopperCommand(this);
            getCommand("mytheriahoppers").setExecutor(hopperCommand);
            getCommand("mytheriahoppers").setTabCompleter(hopperCommand);
        }

        hopperTask = new HopperTask(this);
        getServer().getPluginManager().registerEvents(hopperTask, this);
        hopperTask.runTaskTimer(this, 0L, 2L);

        getLogger().info("MytheriaHoppers enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (hopperManager != null) {
            hopperManager.saveData();
        }
        getLogger().info("MytheriaHoppers data saved successfully!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        
        this.economyManager = new EconomyManager(new VaultEconomyProvider(economy));

        return economy != null;
    }

    public static MytheriaHoppers getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public HopperManager getHopperManager() {
        return hopperManager;
    }

    public UpgradeManager getUpgradeManager() {
        return upgradeManager;
    }

    public HopperGUI getHopperGUI() {
        return hopperGUI;
    }

    public void refreshHopperTaskConfiguration() {
        if (hopperTask != null) hopperTask.refreshConfiguration();
    }

    public int disableTypedHoppers() {
        int converted = 0;
        for (HopperData data : hopperManager.getHoppers().values()) {
            if (data.getType() != HopperType.ALL) {
                data.setType(HopperType.ALL);
                converted++;
            }
        }

        for (World world : getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                converted += registerUniversalHoppers(chunk);
            }
        }
        hopperManager.saveData();
        return converted;
    }

    public int registerUniversalHoppers(Chunk chunk) {
        int registered = 0;
        for (BlockState state : chunk.getTileEntities()) {
            if (state.getType() != Material.HOPPER) continue;
            HopperData existing = hopperManager.getHoppers().get(state.getLocation());
            if (existing == null) {
                hopperManager.getHoppers().put(state.getLocation(), new HopperData());
                if (hopperTask != null) hopperTask.indexHopper(state.getLocation());
                registered++;
            }
        }
        return registered;
    }

    public void updateHopperRecipe() {
        Iterator<Recipe> recipes = getServer().recipeIterator();
        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            if (recipe.getResult().getType() == Material.HOPPER) {
                recipes.remove();
            }
        }
        if (!getConfig().getBoolean("crafting-recipes.enabled", true)) return;

        ShapedRecipe recipe = new ShapedRecipe(
                new NamespacedKey(this, "hopper_recipe"),
                new org.bukkit.inventory.ItemStack(Material.HOPPER));
        recipe.shape("I I", "ICI", " I ");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('C', Material.CHEST);
        getServer().addRecipe(recipe);
    }

    public HopperTask getHopperTask() {
        return hopperTask;
    }
}
