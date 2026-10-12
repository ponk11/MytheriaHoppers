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
import me.mytheria.hoppers.managers.HopperManager;
import me.mytheria.hoppers.commands.HopperCommand;
import net.milkbowl.vault.economy.Economy;

public class MytheriaHoppers extends JavaPlugin {

    private static MytheriaHoppers instance;
    private Economy economy;
    private EconomyManager economyManager;
    private HopperManager hopperManager;
    private UpgradeManager upgradeManager;
    private HopperGUI hopperGUI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().warning("Vault economy not found! Economy-based upgrades will be disabled.");
        }

        this.hopperManager = new HopperManager(this);
        this.upgradeManager = new UpgradeManager(this);
        this.hopperGUI = new HopperGUI(this);

        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new HopperInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new HopperBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new HopperBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new HopperPlaceListener(this), this);

        if (getCommand("mytheriahoppers") != null) {
            getCommand("mytheriahoppers").setExecutor(new HopperCommand(this));
        }

        // Schedule the hopper task to run every tick
        new HopperTask(this).runTaskTimer(this, 0L, 1L);

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
}
