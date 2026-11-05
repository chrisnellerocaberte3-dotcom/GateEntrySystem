package com.yourdomain.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private DatabaseInitializer() { }

    public static boolean initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS Resident (
                    resident_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    resident_name     TEXT NOT NULL,
                    block_lot_address TEXT NOT NULL,
                    contact_number    TEXT,
                    status            TEXT NOT NULL DEFAULT 'ACTIVE'
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS Vehicle (
                    vehicle_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    resident_id   INTEGER NOT NULL,
                    plate_number  TEXT NOT NULL UNIQUE,
                    vehicle_type  TEXT,
                    vehicle_color TEXT,
                    FOREIGN KEY (resident_id) REFERENCES Resident(resident_id)
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS GateEntry (
                    entry_id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    resident_id      INTEGER NOT NULL,
                    visitor_name     TEXT NOT NULL,
                    purpose_of_visit TEXT,
                    plate_number     TEXT,
                    time_in          TEXT,
                    time_out         TEXT,
                    entry_status     TEXT NOT NULL DEFAULT 'INSIDE',
                    FOREIGN KEY (resident_id) REFERENCES Resident(resident_id)
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS Users (
                    user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    username      TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    full_name     TEXT NOT NULL,
                    role          TEXT NOT NULL CHECK (role IN ('ADMIN','GUARD','RESIDENT')),
                    status        TEXT NOT NULL DEFAULT 'PENDING'
                                  CHECK (status IN ('PENDING','APPROVED','REJECTED','INACTIVE'))
                )""");

            st.execute("""
                CREATE TABLE IF NOT EXISTS Activity_Logs (
                    log_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER NOT NULL,
                    action      TEXT NOT NULL,
                    description TEXT,
                    timestamp   TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES Users(user_id)
                )""");

            seedUsersIfEmpty(conn);
            seedResidentDataIfEmpty(conn);
            return true;

        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            return false;
        }
    }

    private static void seedUsersIfEmpty(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Users")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        String sql = "INSERT INTO Users (username, password_hash, full_name, role, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            insert(ps, "admin",     "admin123", "System Administrator", "ADMIN",    "APPROVED");
            insert(ps, "guard1",    "guard123", "Juan Dela Cruz",       "GUARD",    "PENDING");
            insert(ps, "resident1", "res123",   "Maria Santos",         "RESIDENT", "PENDING");
        }
        System.out.println("First run: default admin created (admin / admin123).");
    }

    private static void insert(PreparedStatement ps, String username, String plainPassword,
                               String fullName, String role, String status) throws SQLException {
        ps.setString(1, username);
        ps.setString(2, UserDao.hashPassword(plainPassword));
        ps.setString(3, fullName);
        ps.setString(4, role);
        ps.setString(5, status);
        ps.executeUpdate();
    }

    private static void seedResidentDataIfEmpty(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Resident")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Resident (resident_name, block_lot_address, contact_number, status) VALUES (?, ?, ?, 'ACTIVE')")) {
            ps.setString(1, "Maria Santos");
            ps.setString(2, "Block 3 Lot 12");
            ps.setString(3, "09171234567");
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO Vehicle (resident_id, plate_number, vehicle_type, vehicle_color) VALUES (1, ?, ?, ?)")) {
            ps.setString(1, "ABC 1234");
            ps.setString(2, "Sedan");
            ps.setString(3, "White");
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO GateEntry (resident_id, visitor_name, purpose_of_visit, plate_number, time_in, time_out, entry_status) "
                        + "VALUES (1, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, "Pedro Reyes");
            ps.setString(2, "Delivery");
            ps.setString(3, "XYZ 5678");
            ps.setString(4, "2026-11-03 09:15");
            ps.setString(5, "2026-11-03 09:40");
            ps.setString(6, "EXITED");
            ps.executeUpdate();
        }
    }
}