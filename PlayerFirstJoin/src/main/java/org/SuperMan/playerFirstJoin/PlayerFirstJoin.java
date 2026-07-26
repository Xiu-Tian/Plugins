package org.SuperMan.playerFirstJoin;

import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerFirstJoin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getLogger().info("PlayerFirstJoin 已启用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlayerFirstJoin 已禁用。");
    }
}