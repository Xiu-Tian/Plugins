package org.FixSky.dataPlaceholder;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class DataCommand implements CommandExecutor {

    private final DataPlaceholder plugin;

    public DataCommand(DataPlaceholder plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 仅 OP 或控制台可用
        if (!(sender.isOp() || sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "help":
                sendHelp(sender);
                break;
            case "setdata":
                handleSetData(sender, args);
                break;
            case "removedata":
                handleRemoveData(sender, args);
                break;
            case "getdata":
                handleGetData(sender, args);
                break;
            case "test":
                handleTest(sender, args);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "未知子命令。使用 /dp help 查看帮助。");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== DataPlaceholder 命令帮助 ===");
        sender.sendMessage(ChatColor.YELLOW + "/dp help - 显示帮助");
        sender.sendMessage(ChatColor.YELLOW + "/dp setdata <键> <值> - 设置自己的数据");
        sender.sendMessage(ChatColor.YELLOW + "/dp setdata <玩家> <键> <值> - 设置其他玩家的数据");
        sender.sendMessage(ChatColor.YELLOW + "/dp removedata <键> - 删除自己的数据");
        sender.sendMessage(ChatColor.YELLOW + "/dp removedata <玩家> <键> - 删除其他玩家的数据");
        sender.sendMessage(ChatColor.YELLOW + "/dp getdata <键> - 查看自己的数据");
        sender.sendMessage(ChatColor.YELLOW + "/dp getdata <玩家> <键> - 查看其他玩家的数据");
        sender.sendMessage(ChatColor.YELLOW + "/dp test <键> <玩家> - 测试占位符解析");
    }

    private void handleSetData(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "用法: /dp setdata <键> <值> 或 /dp setdata <玩家> <键> <值>");
            return;
        }

        Player target;
        String key, value;

        Player possiblePlayer = Bukkit.getPlayer(args[1]);
        if (possiblePlayer != null && args.length == 4) {
            target = possiblePlayer;
            key = args[2];
            value = args[3];
        } else if (args.length == 3) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "控制台必须指定玩家名: /dp setdata <玩家> <键> <值>");
                return;
            }
            target = (Player) sender;
            key = args[1];
            value = args[2];
        } else {
            sender.sendMessage(ChatColor.RED + "参数错误。使用 /dp help 查看帮助。");
            return;
        }

        plugin.getDataManager().setData(target, key, value);
        sender.sendMessage(ChatColor.GREEN + "已为 " + target.getName() + " 设置 " + key + " = " + value);
    }

    private void handleRemoveData(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /dp removedata <键> 或 /dp removedata <玩家> <键>");
            return;
        }

        Player target;
        String key;

        Player possiblePlayer = Bukkit.getPlayer(args[1]);
        if (possiblePlayer != null && args.length == 3) {
            target = possiblePlayer;
            key = args[2];
        } else if (args.length == 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "控制台必须指定玩家名: /dp removedata <玩家> <键>");
                return;
            }
            target = (Player) sender;
            key = args[1];
        } else {
            sender.sendMessage(ChatColor.RED + "参数错误。使用 /dp help 查看帮助。");
            return;
        }

        plugin.getDataManager().removeData(target, key);
        sender.sendMessage(ChatColor.GREEN + "已删除 " + target.getName() + " 的 " + key);
    }

    private void handleGetData(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "控制台必须指定玩家名: /dp getdata <玩家> <键>");
                return;
            }
            Player self = (Player) sender;
            displayAllData(sender, self);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target != null) {
            if (args.length == 2) {
                displayAllData(sender, target);
            } else if (args.length == 3) {
                String key = args[2];
                String value = plugin.getDataManager().getData(target, key);
                if (value == null) {
                    sender.sendMessage(ChatColor.YELLOW + target.getName() + " 没有 " + key + " 的数据。");
                } else {
                    sender.sendMessage(ChatColor.GREEN + target.getName() + "." + key + " = " + value);
                }
            } else {
                sender.sendMessage(ChatColor.RED + "参数过多。使用 /dp help 查看帮助。");
            }
            return;
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "控制台必须指定玩家名: /dp getdata <玩家> <键>");
                return;
            }
            Player self = (Player) sender;
            String key = args[1];
            String value = plugin.getDataManager().getData(self, key);
            if (value == null) {
                sender.sendMessage(ChatColor.YELLOW + "你没有 " + key + " 的数据。");
            } else {
                sender.sendMessage(ChatColor.GREEN + "你的 " + key + " = " + value);
            }
        }
    }

    private void displayAllData(CommandSender sender, Player player) {
        Map<String, String> data = plugin.getDataManager().getPlayerData(player);
        if (data.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + player.getName() + " 没有任何数据。");
        } else {
            sender.sendMessage(ChatColor.AQUA + player.getName() + " 的数据:");
            for (Map.Entry<String, String> entry : data.entrySet()) {
                sender.sendMessage(ChatColor.WHITE + entry.getKey() + " = " + entry.getValue());
            }
        }
    }

    private void handleTest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /dp test <键> <玩家>");
            return;
        }

        String key = args[1];
        Player target;

        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "玩家 " + args[2] + " 不在线。");
                return;
            }
        } else {
            if (sender instanceof Player) {
                target = (Player) sender;
            } else {
                sender.sendMessage(ChatColor.RED + "控制台必须指定玩家名: /dp test <键> <玩家>");
                return;
            }
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            sender.sendMessage(ChatColor.RED + "PlaceholderAPI 未安装，无法测试占位符。");
            return;
        }

        String identifier = plugin.getExpansionIdentifier();
        String placeholder = "%" + identifier + "_" + key + "%";
        String result = PlaceholderAPI.setPlaceholders(target, placeholder);

        sender.sendMessage(ChatColor.GOLD + "玩家 " + target.getName() + " 的占位符 " + placeholder + " 解析为: " + ChatColor.GREEN + result);
    }
}