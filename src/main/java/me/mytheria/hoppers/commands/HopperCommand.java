package me.mytheria.hoppers.commands;

import me.mytheria.hoppers.MytheriaHoppers;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HopperCommand implements CommandExecutor {
    private final MytheriaHoppers plugin;

    public HopperCommand(MytheriaHoppers plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(color("&dMytheria Hoppers &7- &f/mh reload"));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mytheriahoppers.admin.reload")) {
                sender.sendMessage(color("&cYou do not have permission."));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(color("&dMytheria Hoppers &8» &aConfig reloaded!"));
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
