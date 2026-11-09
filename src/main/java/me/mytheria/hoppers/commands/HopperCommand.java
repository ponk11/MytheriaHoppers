package me.mytheria.hoppers.commands;

import me.mytheria.hoppers.MytheriaHoppers;
import me.mytheria.hoppers.hopper.HopperItem;
import me.mytheria.hoppers.hopper.HopperType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HopperCommand implements CommandExecutor, TabCompleter {
    private final MytheriaHoppers plugin;

    public HopperCommand(MytheriaHoppers plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                plugin.getHopperGUI().openHelpGUI((Player) sender);
            } else {
                sender.sendMessage(color("&dMytheria Hoppers &7- &f/mh reload &7| &f/mh admin &7| &f/mh give"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mytheriahoppers.admin.reload")) {
                sender.sendMessage(color("&cYou do not have permission."));
                return true;
            }
            plugin.reloadConfig();
            plugin.refreshHopperTaskConfiguration();
            if (!plugin.getConfig().getBoolean("hopper-types.enabled", true)) {
                plugin.disableTypedHoppers();
            }
            sender.sendMessage(color("&dMytheria Hoppers &8» &aConfig reloaded!"));
            return true;
        }
        if (args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("mytheriahoppers.admin")) {
                sender.sendMessage(color("&cYou do not have permission."));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(color("&cUsage: /mh give <player> <all|crop|mob|ore> [amount]"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            HopperType type = HopperType.fromName(args[2]);
            if (!type.name().equalsIgnoreCase(args[2])) {
                sender.sendMessage(color("&cUnknown hopper type. Use all, crop, mob, or ore."));
                return true;
            }
            if (!plugin.getConfig().getBoolean("hopper-types.enabled", true)) {
                type = HopperType.ALL;
            }
            if (target == null) {
                sender.sendMessage(color("&cThat player is not online."));
                return true;
            }
            int amount = 1;
            if (args.length > 3) {
                try {
                    amount = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
                } catch (NumberFormatException exception) {
                    sender.sendMessage(color("&cAmount must be a whole number."));
                    return true;
                }
            }
            org.bukkit.inventory.ItemStack item = HopperItem.create(type, 0, 0);
            item.setAmount(amount);
            target.getInventory().addItem(item);
            sender.sendMessage(color("&aGave " + amount + " " + type.getDisplayName() + " hopper(s) to " + target.getName() + "."));
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            if (sender.hasPermission("mytheriahoppers.admin.reload")) {
                commands.add("reload");
            }
            if (sender.hasPermission("mytheriahoppers.admin")) {
                commands.add("admin");
                commands.add("give");
            }
            return partial(args[0], commands.toArray(new String[0]));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("mytheriahoppers.admin")) {
                return Collections.emptyList();
            }
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return partial(args[1], players.toArray(new String[0]));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("mytheriahoppers.admin")) {
                return Collections.emptyList();
            }
            return partial(args[2], "all", "crop", "mob", "ore");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("mytheriahoppers.admin")) {
                return Collections.emptyList();
            }
            return partial(args[3], "1", "16", "32", "64");
        }
        return Collections.emptyList();
    }

    private List<String> partial(String input, String... values) {
        List<String> matches = new ArrayList<>();
        for (String value : Arrays.asList(values)) {
            if (value.toLowerCase().startsWith(input.toLowerCase())) {
                matches.add(value);
            }
        }
        return matches;
    }

    @SuppressWarnings("deprecation")
    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
