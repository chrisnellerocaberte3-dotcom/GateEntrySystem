package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    private static final String SELECT_COLUMNS =
            "SELECT log_id, user_id, action, description, timestamp FROM Activity_Logs";

    // Local time (not UTC) so the log matches the clock on the screen.
    private static final String INSERT_SQL =
            "INSERT INTO Activity_Logs (user_id, action, description, timestamp) "
                    + "VALUES (?, ?, ?, strftime('%Y-%m-%d %H:%M:%S', 'now', 'localtime'))";


    public boolean logAction(int userId, String actionType, String description) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            logAction(conn, userId, actionType, description);
            return true;
        } catch (SQLException e) {
            System.err.println("WARNING: could not write audit log: " + e.getMessage());
            return false;
        }
    }

    public void logAction(Connection conn, int userId, String actionType, String description) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            ps.setInt(1, userId);
            ps.setString(2, actionType);
            ps.setString(3, description);
            ps.executeUpdate();
        }
    }


    public List<ActivityLog> getAllLogs() throws SQLException {
        String sql = SELECT_COLUMNS + " ORDER BY timestamp DESC, log_id DESC";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                logs.add(mapRow(rs));
            }
        }
        return logs;
    }


    public List<ActivityLog> getLogsByUserId(int userId) throws SQLException {
        String sql = SELECT_COLUMNS + " WHERE user_id = ? ORDER BY timestamp DESC, log_id DESC";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        }
        return logs;
    }

    public int clearLogs() throws SQLException {
        String sql = "DELETE FROM Activity_Logs";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    private ActivityLog mapRow(ResultSet rs) throws SQLException {
        return new ActivityLog(
                rs.getInt("log_id"),
                rs.getInt("user_id"),
                rs.getString("action"),
                rs.getString("description"),
                rs.getString("timestamp"));
    }
}