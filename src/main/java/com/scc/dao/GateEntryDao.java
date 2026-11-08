package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GateEntryDao {

    private static final String NOW = "strftime('%Y-%m-%d %H:%M', 'now', 'localtime')";

    public Optional<String> findActiveResidentName(int residentId) throws SQLException {
        String sql = "SELECT resident_name FROM Resident WHERE resident_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, residentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        }
    }

    public int addEntry(int guardUserId, int residentId, String visitorName,
                        String purpose, String plateNumber) throws SQLException {
        String sql = "INSERT INTO GateEntry (resident_id, visitor_name, purpose_of_visit, plate_number, time_in, entry_status) "
                + "VALUES (?, ?, ?, ?, " + NOW + ", 'INSIDE')";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, residentId);
                ps.setString(2, visitorName);
                ps.setString(3, purpose);
                ps.setString(4, plateNumber);
                ps.executeUpdate();

                int entryId;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    entryId = keys.getInt(1);
                }
                log(conn, guardUserId, "VISITOR_ENTRY",
                        "Visitor '" + visitorName + "' entered to visit resident ID " + residentId
                                + " (entry ID " + entryId + ")");
                conn.commit();
                return entryId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<String[]> getVisitorsInside() throws SQLException {
        String sql = """
            SELECT g.entry_id, g.visitor_name, r.resident_name, r.block_lot_address,
                   g.purpose_of_visit, g.plate_number, g.time_in
            FROM GateEntry g
            JOIN Resident r ON g.resident_id = r.resident_id
            WHERE g.entry_status = 'INSIDE'
            ORDER BY g.entry_id""";
        List<String[]> rows = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String[] row = new String[7];
                for (int i = 0; i < 7; i++) {
                    row[i] = rs.getString(i + 1);
                }
                rows.add(row);
            }
        }
        return rows;
    }

    public boolean recordTimeOut(int guardUserId, int entryId) throws SQLException {
        String sql = "UPDATE GateEntry SET time_out = " + NOW + ", entry_status = 'EXITED' "
                + "WHERE entry_id = ? AND entry_status = 'INSIDE'";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, entryId);
                boolean updated = ps.executeUpdate() > 0;
                if (updated) {
                    log(conn, guardUserId, "VISITOR_EXIT", "Time-out recorded for entry ID " + entryId);
                }
                conn.commit();
                return updated;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<String[]> searchResidents(String keyword) throws SQLException {
        String sql = """
            SELECT r.resident_id, r.resident_name, r.block_lot_address, r.status,
                   COALESCE(group_concat(v.plate_number, ', '), '-')
            FROM Resident r
            LEFT JOIN Vehicle v ON v.resident_id = r.resident_id
            WHERE r.resident_name LIKE ? OR r.block_lot_address LIKE ? OR v.plate_number LIKE ?
            GROUP BY r.resident_id
            ORDER BY r.resident_name""";
        String pattern = "%" + keyword + "%";
        List<String[]> rows = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[] {
                            rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5) });
                }
            }
        }
        return rows;
    }

    private void log(Connection conn, int userId, String action, String description) throws SQLException {
        String sql = "INSERT INTO Activity_Logs (user_id, action, description) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.setString(3, description);
            ps.executeUpdate();
        }
    }
}