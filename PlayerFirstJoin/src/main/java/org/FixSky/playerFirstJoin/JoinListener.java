package org.FixSky.playerFirstJoin;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Collections;
import java.util.List;

public class JoinListener implements Listener {

    private final PlayerFirstJoin plugin;

    public JoinListener(PlayerFirstJoin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            // 获取命令列表（支持新格式）
            List<String> commands = plugin.getConfig().getStringList("first-join-commands");
            // 若列表为空，尝试兼容旧格式（单条字符串命令）
            if (commands.isEmpty()) {
                String single = plugin.getConfig().getString("first-join-command");
                if (single != null && !single.isEmpty()) {
                    commands = Collections.singletonList(single);
                } else {
                    return; // 无命令可执行
                }
            }

            // 逐条执行
            for (String rawCommand : commands) {
                executeCommand(player, rawCommand);
            }
        }
    }

    /**
     * 执行单条命令，包含前缀解析、占位符替换和命令分发。
     */
    private void executeCommand(Player player, String rawCommand) {
        // 替换自定义占位符 {player}
        rawCommand = rawCommand.replace("{player}", player.getName());

        // 解析执行者类型
        String executorType = "console";
        String commandBody = rawCommand;

        if (rawCommand.startsWith("console:")) {
            executorType = "console";
            commandBody = rawCommand.substring("console:".length());
        } else if (rawCommand.startsWith("op:")) {
            executorType = "op";
            commandBody = rawCommand.substring("op:".length());
        } else if (rawCommand.startsWith("player:")) {
            executorType = "player";
            commandBody = rawCommand.substring("player:".length());
        }

        // 解析 PlaceholderAPI 占位符
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                commandBody = PlaceholderAPI.setPlaceholders(player, commandBody);
            } catch (Exception e) {
                plugin.getLogger().warning("解析 PlaceholderAPI 占位符失败: " + e.getMessage());
            }
        }

        // 执行命令
        boolean success = false;
        switch (executorType.toLowerCase()) {
            case "console":
                success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandBody);
                break;
            case "op":
                try {
                    boolean wasOp = player.isOp();
                    player.setOp(true);
                    success = Bukkit.dispatchCommand(player, commandBody);
                    player.setOp(wasOp);
                } catch (Exception e) {
                    plugin.getLogger().warning("OP 执行命令时出错: " + e.getMessage());
                }
                break;
            case "player":
                success = Bukkit.dispatchCommand(player, commandBody);
                break;
            default:
                success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandBody);
                break;
        }

        if (success) {
            plugin.getLogger().info("首次加入玩家 " + player.getName() +
                    "，执行者类型: " + executorType + "，命令: " + commandBody);
        } else {
            plugin.getLogger().warning("首次加入命令执行失败: " + commandBody);
        }
    }
}