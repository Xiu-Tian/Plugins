package org.FixSky.dataPlaceholder.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class YamlDataStorage implements DataStorage {

    private final File dataFile;
    private Map<UUID, Map<String, String>> cache = new HashMap<>();

    public YamlDataStorage(File dataFile) {
        this.dataFile = dataFile;
    }

    @Override
    public void initialize() {
        if (dataFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            for (String uuidStr : config.getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, String> data = new HashMap<>();
                for (String key : config.getConfigurationSection(uuidStr).getKeys(false)) {
                    data.put(key, config.getString(uuidStr + "." + key));
                }
                cache.put(uuid, data);
            }
        }
    }

    @Override
    public void shutdown() {
        saveAll();
    }

    private void saveAll() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, String>> entry : cache.entrySet()) {
            String uuidStr = entry.getKey().toString();
            for (Map.Entry<String, String> dataEntry : entry.getValue().entrySet()) {
                config.set(uuidStr + "." + dataEntry.getKey(), dataEntry.getValue());
            }
        }
        try {
            config.save(dataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, String> getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    @Override
    public String getData(UUID uuid, String key) {
        Map<String, String> data = cache.get(uuid);
        return data == null ? null : data.get(key);
    }

    @Override
    public void setData(UUID uuid, String key, String value) {
        Map<String, String> data = getPlayerData(uuid);
        data.put(key, value);
        saveAll();
    }

    @Override
    public void removeData(UUID uuid, String key) {
        Map<String, String> data = cache.get(uuid);
        if (data != null) {
            data.remove(key);
            if (data.isEmpty()) {
                cache.remove(uuid);
            }
            saveAll();
        }
    }
}