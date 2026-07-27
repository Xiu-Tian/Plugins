package org.FixSky.dataPlaceholder;

import org.FixSky.dataPlaceholder.storage.DataStorage;
import org.FixSky.dataPlaceholder.storage.MySqlDataStorage;
import org.FixSky.dataPlaceholder.storage.YamlDataStorage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class DataPlaceholder extends JavaPlugin {

    private static DataPlaceholder instance;
    private PlayerDataManager dataManager;
    private String expansionIdentifier;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();

        expansionIdentifier = getConfig().getString("identifier", "dp");
        getLogger().info("Placeholder identifier: " + expansionIdentifier);

        DataStorage storage = createStorage();
        dataManager = new PlayerDataManager(storage);

        // 注册命令
        DataCommand commandExecutor = new DataCommand(this);
        getCommand("dp").setExecutor(commandExecutor);
        getCommand("dp").setTabCompleter(new DataTabCompleter(this));

        // 注册 PlaceholderAPI 扩展
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DataPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered.");
        } else {
            getLogger().warning("PlaceholderAPI not found - placeholders won't work.");
        }
    }

    private DataStorage createStorage() {
        String type = getConfig().getString("storage-type", "yaml").toLowerCase();
        if (type.equals("mysql")) {
            String host = getConfig().getString("mysql.host", "localhost");
            int port = getConfig().getInt("mysql.port", 3306);
            String database = getConfig().getString("mysql.database", "minecraft");
            String username = getConfig().getString("mysql.username", "root");
            String password = getConfig().getString("mysql.password", "");
            String table = getConfig().getString("mysql.table", "player_data");
            getLogger().info("Using MySQL storage.");
            return new MySqlDataStorage(host, port, database, username, password, table);
        } else {
            getLogger().info("Using YAML storage.");
            return new YamlDataStorage(new File(getDataFolder(), "data.yml"));
        }
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.shutdown();
        }
    }

    public static DataPlaceholder getInstance() {
        return instance;
    }

    public PlayerDataManager getDataManager() {
        return dataManager;
    }

    public String getExpansionIdentifier() {
        return expansionIdentifier;
    }
}