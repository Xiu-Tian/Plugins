package org.FixSky.dataPlaceholder.storage;

import java.util.Map;
import java.util.UUID;

public interface DataStorage {
    void initialize();
    void shutdown();
    Map<String, String> getPlayerData(UUID uuid);
    String getData(UUID uuid, String key);
    void setData(UUID uuid, String key, String value);
    void removeData(UUID uuid, String key);
}