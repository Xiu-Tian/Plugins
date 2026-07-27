package org.FixSky.dataPlaceholder.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MySqlDataStorage implements DataStorage {

    private final HikariDataSource dataSource;
    private final String table;

    public MySqlDataStorage(String host, int port, String database, String username, String password, String table) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(10);
        this.dataSource = new HikariDataSource(config);
        this.table = table;
    }

    @Override
    public void initialize() {
        String sql = "CREATE TABLE IF NOT EXISTS " + table + " (" +
                "uuid CHAR(36) NOT NULL, " +
                "data_key VARCHAR(64) NOT NULL, " +
                "data_value VARCHAR(255) NOT NULL, " +
                "PRIMARY KEY (uuid, data_key))";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public Map<String, String> getPlayerData(UUID uuid) {
        Map<String, String> result = new HashMap<>();
        String sql = "SELECT data_key, data_value FROM " + table + " WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.put(rs.getString("data_key"), rs.getString("data_value"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public String getData(UUID uuid, String key) {
        String sql = "SELECT data_value FROM " + table + " WHERE uuid = ? AND data_key = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, key);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("data_value");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void setData(UUID uuid, String key, String value) {
        String sql = "INSERT INTO " + table + " (uuid, data_key, data_value) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE data_value = VALUES(data_value)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, key);
            stmt.setString(3, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void removeData(UUID uuid, String key) {
        String sql = "DELETE FROM " + table + " WHERE uuid = ? AND data_key = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, key);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}