package org.FixSky.dataPlaceholder;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DataTabCompleter implements TabCompleter {

    private final DataPlaceholder plugin;

    public DataTabCompleter(DataPlaceholder plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // 仅对有权限的玩家补全
        if (!(sender.isOp() || sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            return new ArrayList<>();
        }

        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("help", "setdata", "removedata", "getdata", "test");
            suggestions = subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length >= 2) {
            String sub = args[0].toLowerCase();
            // 为需要玩家名的子命令提供在线玩家列表
            if (sub.equals("setdata") || sub.equals("removedata") || sub.equals("getdata") || sub.equals("test")) {
                String arg = args[1].toLowerCase();
                suggestions = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(arg))
                        .collect(Collectors.toList());
            }
        }

        return suggestions;
    }
}