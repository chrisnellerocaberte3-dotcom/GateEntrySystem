package com.yourdomain.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ResidentDao {

    public List<String[]> getVehiclesByResidentName(String residentName) throws SQLException {
        String sql = """
            SELECT v.plate_number, v.vehicle_type, v.vehicle_color
            FROM Vehicle v
            JOIN Resident r ON v.resident_id = r.resident_id
            WHERE r.resident_name = ? AND r.status = 'ACTIVE'
            ORDER BY v.vehicle_id""";
        List<String[]> rows = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, residentName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[] {
                            rs.getString(1), rs.getString(2), rs.getString(3) });
                }
            }
        }
        return rows;
    }

    public List<String[]> getGateEntriesByResidentName(String residentName) throws SQLException {
        String sql = """
            SELECT g.visitor_name, g.purpose_of_visit, g.plate_number,
                   g.time_in, g.time_out, g.entry_status
            FROM GateEntry g
            JOIN Resident r ON g.resident_id = r.resident_id
            WHERE r.resident_name = ?
            ORDER BY g.entry_id DESC""";
        List<String[]> rows = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, residentName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[] {
                            rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5), rs.getString(6) });
                }
            }
        }
        return rows;
    }
}