package org.FixSky.dataPlaceholder;

import org.FixSky.dataPlaceholder.storage.DataStorage;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final DataStorage storage;

    public PlayerDataManager(DataStorage storage) {
        this.storage = storage;
        storage.initialize();
    }

    public void shutdown() {
        storage.shutdown();
    }

    public Map<String, String> getPlayerData(Player player) {
        return storage.getPlayerData(player.getUniqueId());
    }

    public String getData(Player player, String key) {
        return storage.getData(player.getUniqueId(), key);
    }

    public String getData(UUID uuid, String key) {
        return storage.getData(uuid, key);
    }

    public void setData(Player player, String key, String value) {
        storage.setData(player.getUniqueId(), key, value);
    }

    public void setData(UUID uuid, String key, String value) {
        storage.setData(uuid, key, value);
    }

    public void removeData(Player player, String key) {
        storage.removeData(player.getUniqueId(), key);
    }

    public void removeData(UUID uuid, String key) {
        storage.removeData(uuid, key);
    }
}