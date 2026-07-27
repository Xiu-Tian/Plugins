package org.FixSky.dataPlaceholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataPlaceholderExpansion extends PlaceholderExpansion {

    private final DataPlaceholder plugin;

    public DataPlaceholderExpansion(DataPlaceholder plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return plugin.getExpansionIdentifier();
    }

    @Override
    public @NotNull String getAuthor() {
        return "FixSky";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return null;
        return plugin.getDataManager().getData(player.getUniqueId(), params);
    }
}