package org.FixSky.randomTeleport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RandomTeleport extends JavaPlugin implements TabCompleter {

    private Map<String, Integer> worldRadiusMap = new HashMap<>();
    private final Random random = new Random();
    private String prefix = "";
    private int maxRetries = 10;
    private int retryDelayTicks = 20;

    private String retrySoundName = "ENTITY_EXPERIENCE_ORB_PICKUP";
    private float retrySoundVolume = 1.0f;
    private float retrySoundPitch = 1.0f;

    private Sound cachedSound = null;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getCommand("rtp").setExecutor(this);
        getCommand("rtp").setTabCompleter(this);
        getLogger().info("RandomTeleport 已启用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("RandomTeleport 已禁用。");
    }

    private void loadConfig() {
        reloadConfig();
        worldRadiusMap.clear();

        ConfigurationSection msgSection = getConfig().getConfigurationSection("messages");
        if (msgSection != null) {
            prefix = msgSection.getString("prefix", "&6[随机传送] &r");
        } else {
            prefix = "&6[随机传送] &r";
        }

        maxRetries = getConfig().getInt("max-retries", 10);
        retryDelayTicks = getConfig().getInt("retry-delay-ticks", 20);
        if (retryDelayTicks < 0) retryDelayTicks = 0;

        retrySoundName = getConfig().getString("retry-sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        retrySoundVolume = (float) getConfig().getDouble("retry-sound-volume", 1.0);
        retrySoundPitch = (float) getConfig().getDouble("retry-sound-pitch", 1.0);

        // 使用反射获取音效枚举常量（无编译期弃用警告）
        cachedSound = getSoundByName(retrySoundName);
        if (cachedSound == null) {
            getLogger().warning("无效的音效名称: " + retrySoundName + "，将使用默认音效 ENTITY_EXPERIENCE_ORB_PICKUP");
            retrySoundName = "ENTITY_EXPERIENCE_ORB_PICKUP";
            cachedSound = getSoundByName(retrySoundName);
        }

        // 迁移旧配置
        if (getConfig().contains("target-world") && getConfig().contains("radius")) {
            String oldWorld = getConfig().getString("target-world");
            int oldRadius = getConfig().getInt("radius");
            if (oldWorld != null && !oldWorld.isEmpty()) {
                worldRadiusMap.put(oldWorld, oldRadius);
                getLogger().info("已迁移旧配置：世界=" + oldWorld + "，半径=" + oldRadius);
            }
        }

        ConfigurationSection worldsSection = getConfig().getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldName : worldsSection.getKeys(false)) {
                int radius = worldsSection.getInt(worldName + ".radius", 1000);
                if (radius < 1) radius = 1;
                worldRadiusMap.put(worldName, radius);
            }
            getLogger().info("从 worlds 节点加载了 " + worldRadiusMap.size() + " 个世界。");
        }

        if (worldRadiusMap.isEmpty()) {
            worldRadiusMap.put("world", 1000);
            getLogger().warning("未找到任何世界配置，已添加默认世界 'world'，半径 1000。");
        }

        saveConfig();
    }

    /**
     * 通过反射获取 Sound 枚举常量，完全避免编译期弃用警告
     */
    private Sound getSoundByName(String name) {
        try {
            Method method = Sound.class.getDeclaredMethod("valueOf", String.class);
            return (Sound) method.invoke(null, name.toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    private String getMessage(String key, Object... placeholders) {
        String msg = getConfig().getString("messages." + key, "");
        if (msg.isEmpty()) {
            msg = getDefaultMessage(key);
        }
        msg = msg.replace("{prefix}", prefix);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            String placeholder = placeholders[i].toString();
            String value = placeholders[i + 1] != null ? placeholders[i + 1].toString() : "?";
            msg = msg.replace("{" + placeholder + "}", value);
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private String getDefaultMessage(String key) {
        switch (key) {
            case "prefix": return "&6[随机传送] &r";
            case "teleport-success": return "{prefix}&a你已传送至世界 {world} 的 ({x}, {y}, {z})";
            case "teleport-fail": return "{prefix}&c在 {world} 找不到安全的地面位置（已尝试 {attempts} 次）。";
            case "no-permission": return "{prefix}&c你没有权限执行此操作。";
            case "player-only": return "{prefix}&c此命令只能由玩家执行！";
            case "world-not-exist": return "{prefix}&c世界 '{world}' 未加载或不存在！";
            case "world-not-configured": return "{prefix}&c未知的世界 '{world}'。可用世界请使用 /rtp list 查看。";
            case "no-worlds-configured": return "{prefix}&c没有配置任何世界，请管理员检查配置。";
            case "reload-success": return "{prefix}&a配置已重新加载！当前可用世界数：{count}";
            case "list-header": return "{prefix}&6=== 可用世界列表 ===";
            case "list-entry": return "&7- {world} &8(半径: {radius})";
            case "error-occurred": return "{prefix}&c传送过程中发生错误：{error}";
            case "retry-attempt": return "{prefix}&e正在尝试第 {attempt}/{max} 次...";
            case "help":
                return "{prefix}&6=== 随机传送命令帮助 ===\n" +
                        "&e/rtp &7- 随机传送至已配置的某个世界\n" +
                        "&e/rtp <世界名> &7- 传送到指定的配置世界\n" +
                        "&e/rtp list &7- 查看所有可用世界及其半径\n" +
                        "&e/rtp reload &7- 重新加载配置文件 (需权限)\n" +
                        "&e/rtp help &7- 显示此帮助信息";
            default: return "";
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "reload":
                    if (!player.hasPermission("randomteleport.reload")) {
                        player.sendMessage(getMessage("no-permission"));
                        return true;
                    }
                    loadConfig();
                    player.sendMessage(getMessage("reload-success", "count", worldRadiusMap.size()));
                    return true;

                case "list":
                    if (worldRadiusMap.isEmpty()) {
                        player.sendMessage(getMessage("no-worlds-configured"));
                        return true;
                    }
                    player.sendMessage(getMessage("list-header"));
                    for (Map.Entry<String, Integer> entry : worldRadiusMap.entrySet()) {
                        player.sendMessage(getMessage("list-entry", "world", entry.getKey(), "radius", entry.getValue()));
                    }
                    return true;

                case "help":
                    String helpMsg = getMessage("help");
                    for (String line : helpMsg.split("\n")) {
                        player.sendMessage(line);
                    }
                    return true;

                default:
                    String targetWorldName = args[0];
                    if (worldRadiusMap.containsKey(targetWorldName)) {
                        performTeleport(player, targetWorldName);
                        return true;
                    } else {
                        player.sendMessage(getMessage("world-not-configured", "world", targetWorldName));
                        return true;
                    }
            }
        }

        if (worldRadiusMap.isEmpty()) {
            player.sendMessage(getMessage("no-worlds-configured"));
            return true;
        }

        List<String> worldNames = new ArrayList<>(worldRadiusMap.keySet());
        String randomWorld = worldNames.get(random.nextInt(worldNames.size()));
        performTeleport(player, randomWorld);
        return true;
    }

    private void performTeleport(Player player, String worldName) {
        if (!player.hasPermission("randomteleport.use")) {
            player.sendMessage(getMessage("no-permission"));
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(getMessage("world-not-exist", "world", worldName));
            return;
        }

        int radius = worldRadiusMap.get(worldName);

        // 异步生成位置
        CompletableFuture.supplyAsync(() -> {
            int attempts = 0;
            Location location = null;
            while (attempts < maxRetries) {
                attempts++;
                final int currentAttempt = attempts;
                // 发送提示和音效（主线程）
                Bukkit.getScheduler().runTask(this, () -> {
                    player.sendMessage(getMessage("retry-attempt", "attempt", currentAttempt, "max", maxRetries));
                    if (cachedSound != null) {
                        player.playSound(player.getLocation(), cachedSound, retrySoundVolume, retrySoundPitch);
                    }
                });

                location = generateRandomLocation(world, radius);
                if (location != null) break;

                if (attempts < maxRetries && retryDelayTicks > 0) {
                    try {
                        Thread.sleep(retryDelayTicks * 50L);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            return location;
        }).thenAccept(location -> {
            if (location == null) {
                player.sendMessage(getMessage("teleport-fail", "world", worldName, "attempts", maxRetries));
                return;
            }
            // 主线程传送
            Bukkit.getScheduler().runTask(this, () -> {
                player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
                player.sendMessage(getMessage("teleport-success",
                        "world", worldName,
                        "x", location.getBlockX(),
                        "y", location.getBlockY(),
                        "z", location.getBlockZ()));
            });
        }).exceptionally(ex -> {
            player.sendMessage(getMessage("error-occurred", "error", ex.getMessage()));
            return null;
        });
    }

    private Location generateRandomLocation(World world, int radius) {
        int x = random.nextInt(2 * radius + 1) - radius;
        int z = random.nextInt(2 * radius + 1) - radius;

        int maxY = world.getMaxHeight() - 1;
        for (int y = maxY; y > 0; y--) {
            Material blockType = world.getBlockAt(x, y, z).getType();
            Material aboveType = world.getBlockAt(x, y + 1, z).getType();
            Material belowType = (y > 0) ? world.getBlockAt(x, y - 1, z).getType() : Material.AIR;

            if (blockType.isSolid() && blockType != Material.BEDROCK &&
                    aboveType.isAir() && belowType != Material.BEDROCK) {
                return new Location(world, x + 0.5, y + 1, z + 0.5);
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            completions.addAll(worldRadiusMap.keySet());
            completions.add("list");
            completions.add("reload");
            completions.add("help");

            List<String> filtered = new ArrayList<>();
            for (String s : completions) {
                if (s.toLowerCase().startsWith(partial)) {
                    filtered.add(s);
                }
            }
            Collections.sort(filtered);
            return filtered;
        }
        return Collections.emptyList();
    }
}