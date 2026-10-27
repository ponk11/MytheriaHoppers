package me.mytheria.hoppers.commands;

import me.mytheria.hoppers.MytheriaHoppers;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HopperCommand implements CommandExecutor {
    private final MytheriaHoppers plugin;

    public HopperCommand(MytheriaHoppers plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&dMytheria Hoppers &7- &f/mh reload &7or &f/mh admin"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mytheriahoppers.admin.reload")) {
                sender.sendMessage(color("&cYou do not have permission."));
                return true;
            }
            plugin.reloadConfig();
            plugin.refreshHopperTaskConfiguration();
            sender.sendMessage(color("&dMytheria Hoppers &8» &aConfig reloaded!"));
            return true;
        }
        if (args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission("mytheriahoppers.admin")) {
                sender.sendMessage(color("&cYou do not have permission."));
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(color("&cOnly players can open the admin GUI."));
                return true;
            }
            plugin.getHopperGUI().openAdminGUI((Player) sender, 0);
            return true;
        }
        sender.sendMessage(color("&cUnknown command."));
        return true;
    }

    @SuppressWarnings("deprecation")
    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
