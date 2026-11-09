package com.scc.ui;

import com.scc.dao.ActivityLogDao;
import com.scc.dao.UserDao;
import com.scc.model.ActivityLog;
import com.scc.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private static final List<String> VALID_ROLES = List.of("ADMIN", "GUARD", "RESIDENT");
    private static final List<String> VALID_STATUSES = List.of("APPROVED", "PENDING");

    private static final String ROW_FORMAT = "| %-5s | %-15s | %-12s | %-10s |%n";
    private static final String LOG_ROW_FORMAT = "| %-5s | %-8s | %-18s | %-35s | %-20s |%n";
    private static final String LOG_BORDER = "+" + "-".repeat(7) + "+" + "-".repeat(10) + "+"
            + "-".repeat(20) + "+" + "-".repeat(37) + "+" + "-".repeat(22) + "+";

    private static final String BORDER =
            "+" + "-".repeat(7) + "+" + "-".repeat(17) + "+" + "-".repeat(14) + "+" + "-".repeat(12) + "+";

    private final Scanner scanner;
    private final UserDao userDao;
    private final User admin;
    private final ActivityLogDao logDao = new ActivityLogDao();

    public AdminMenu(Scanner scanner, UserDao userDao, User admin) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.admin = admin;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== ADMIN MENU (" + admin.fullName() + ") ===");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User Account");
            System.out.println("4. Update User Role & Status");
            System.out.println("5. View System Audit Logs");
            System.out.println("6. Delete User Account");
            System.out.println("0. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingUsers();
                case "3" -> approveUser();
                case "4" -> updateUser();
                case "5" -> auditLogMenu();
                case "6" -> deleteUser();
                case "0" -> running = false;
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    // ---------- menu actions ----------

    private void viewAllUsers() {
        try {
            System.out.println("\n--- All System Users ---");
            printUserTable(userDao.getAllUsers());
        } catch (SQLException e) {
            System.out.println("Error loading users: " + e.getMessage());
        }
    }

    private void viewPendingUsers() {
        try {
            System.out.println("\n--- Pending Registrations ---");
            printUserTable(userDao.getPendingUsers());
        } catch (SQLException e) {
            System.out.println("Error loading pending users: " + e.getMessage());
        }
    }

    private void approveUser() {
        try {
            List<User> pending = userDao.getPendingUsers();
            System.out.println("\n--- Approve User Account ---");
            printUserTable(pending);
            if (pending.isEmpty()) {
                return;
            }

            while (true) {
                int targetId = readIntInput("Enter Target User ID to approve (0 to cancel): ");
                if (targetId == 0) {
                    System.out.println("Approval cancelled.");
                    return;
                }

                User target = pending.stream().filter(u -> u.id() == targetId).findFirst().orElse(null);
                if (target == null) {
                    System.out.println("ID " + targetId + " is not in the pending list. Try again.");
                    continue;
                }

                if (userDao.approveUser(targetId)) {
                    System.out.printf("SUCCESS: User '%s' (ID %d, role %s) has been APPROVED.%n",
                            target.username(), target.id(), target.role());
                    logDao.logAction(admin.id(), "USER_APPROVED",
                            "Admin [" + admin.id() + "] approved account for User ID " + target.id());
                } else {
                    System.out.println("Approval failed: the account may have been changed already.");
                }
                return;
            }
        } catch (SQLException e) {
            System.out.println("Error approving user: " + e.getMessage());
        }
    }

    private void updateUser() {
        try {
            List<User> users = userDao.getAllUsers();
            System.out.println("\n--- Update User Role & Status ---");
            printUserTable(users);
            if (users.isEmpty()) {
                return;
            }

            // Step 1: target user ID
            User target;
            while (true) {
                int targetId = readIntInput("Enter Target User ID to update (0 to cancel): ");
                if (targetId == 0) {
                    System.out.println("Update cancelled.");
                    return;
                }
                if (targetId == admin.id()) {
                    System.out.println("WARNING: Modifying the currently active admin account is restricted.");
                    continue;
                }
                target = users.stream().filter(u -> u.id() == targetId).findFirst().orElse(null);
                if (target == null) {
                    System.out.println("No user found with ID " + targetId + ". Try again.");
                    continue;
                }
                break;
            }

            // Step 2: new role
            String newRole = readValidRole();
            if (newRole == null) {
                System.out.println("Update cancelled.");
                return;
            }

            // Step 3: new status
            String newStatus = readValidStatus();
            if (newStatus == null) {
                System.out.println("Update cancelled.");
                return;
            }

            if (userDao.updateUserRoleAndStatus(target.id(), newRole, newStatus)) {
                System.out.printf("SUCCESS: User '%s' (ID %d) updated: role %s -> %s, status %s -> %s.%n",
                        target.username(), target.id(),
                        target.role(), newRole, target.status(), newStatus);
                logDao.logAction(admin.id(), "USER_UPDATED",
                        "Admin [" + admin.id() + "] changed User " + target.id()
                                + " to role " + newRole + ", status " + newStatus);
            } else {
                System.out.println("Update failed: user not found.");
            }
        } catch (SQLException e) {
            System.out.println("Error updating user: " + e.getMessage());
        }
    }

    private void deleteUser() {
        try {
            List<User> users = userDao.getAllUsers();
            System.out.println("\n--- Delete User Account ---");
            printUserTable(users);
            if (users.isEmpty()) {
                return;
            }

            User target;
            while (true) {
                int targetId = readIntInput("Enter Target User ID to delete (0 to cancel): ");
                if (targetId == 0) {
                    System.out.println("Delete cancelled.");
                    return;
                }
                if (targetId == admin.id()) {
                    System.out.println("WARNING: You cannot delete the currently active admin account.");
                    continue;
                }
                target = users.stream().filter(u -> u.id() == targetId).findFirst().orElse(null);
                if (target == null) {
                    System.out.println("No user found with ID " + targetId + ". Try again.");
                    continue;
                }
                break;
            }

            System.out.printf("%nWARNING: This permanently deletes user '%s' (ID %d, role %s).%n",
                    target.username(), target.id(), target.role());
            System.out.print("Type CONFIRM to proceed (anything else cancels): ");
            if (!scanner.nextLine().trim().equals("CONFIRM")) {
                System.out.println("Delete cancelled. No user was deleted.");
                return;
            }

            if (userDao.deleteUser(target.id())) {
                System.out.printf("SUCCESS: User '%s' (ID %d) has been DELETED.%n",
                        target.username(), target.id());
                logDao.logAction(admin.id(), "USER_DELETED",
                        "Admin [" + admin.id() + "] deleted User " + target.id()
                                + " (" + target.username() + ")");
            } else {
                System.out.println("Delete failed: user not found.");
            }
        } catch (SQLException e) {
            System.out.println("Error deleting user: " + e.getMessage());
            System.out.println("Hint: this user may still have related records (e.g. logs or visitors). "
                    + "Consider setting their status to a non-approved value instead.");
        }
    }

    // ---------- audit logs ----------

    private void auditLogMenu() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- SYSTEM AUDIT LOGS ---");
            System.out.println("1. View All System Logs");
            System.out.println("2. Filter Logs by User ID");
            System.out.println("3. Clear Historical Logs");
            System.out.println("0. Back");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewAllLogs();
                case "2" -> filterLogsByUser();
                case "3" -> clearLogs();
                case "0" -> back = true;
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private void viewAllLogs() {
        try {
            System.out.println("\n--- All System Logs (newest first) ---");
            printLogTable(logDao.getAllLogs());
        } catch (SQLException e) {
            System.out.println("Error loading logs: " + e.getMessage());
        }
    }

    private void filterLogsByUser() {
        try {
            int userId = readIntInput("Enter User ID to filter by (0 to cancel): ");
            if (userId == 0) {
                System.out.println("Filter cancelled.");
                return;
            }
            System.out.println("\n--- Logs for User ID " + userId + " ---");
            printLogTable(logDao.getLogsByUserId(userId));
        } catch (SQLException e) {
            System.out.println("Error loading logs: " + e.getMessage());
        }
    }

    private void clearLogs() {
        try {
            System.out.println("\nWARNING: This permanently deletes ALL audit log entries.");
            System.out.print("Type CONFIRM to proceed (anything else cancels): ");
            if (!scanner.nextLine().trim().equals("CONFIRM")) {
                System.out.println("Clear cancelled. No logs were deleted.");
                return;
            }
            int removed = logDao.clearLogs();
            logDao.logAction(admin.id(), "LOGS_CLEARED",
                    "Admin [" + admin.id() + "] cleared " + removed + " audit log entries");
            System.out.println("SUCCESS: " + removed + " log entries cleared.");
        } catch (SQLException e) {
            System.out.println("Error clearing logs: " + e.getMessage());
        }
    }

    private void printLogTable(List<ActivityLog> logs) {
        if (logs.isEmpty()) {
            System.out.println("(no log entries found)");
            return;
        }
        System.out.println(LOG_BORDER);
        System.out.printf(LOG_ROW_FORMAT, "ID", "User ID", "Action", "Description", "Timestamp");
        System.out.println(LOG_BORDER);
        for (ActivityLog l : logs) {
            System.out.printf(LOG_ROW_FORMAT,
                    l.logId(), l.userId(), fit(l.action(), 18), fit(l.description(), 35), fit(l.timestamp(), 20));
        }
        System.out.println(LOG_BORDER);
        System.out.println(logs.size() + " record(s)");
    }

    // ---------- input validation wrappers ----------

    private int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a whole number.");
            }
        }
    }

    private String readValidRole() {
        while (true) {
            System.out.print("Enter new role " + VALID_ROLES + " (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (VALID_ROLES.contains(input)) {
                return input;
            }
            System.out.println("Invalid role. Choose one of: " + String.join(", ", VALID_ROLES));
        }
    }

    private String readValidStatus() {
        while (true) {
            System.out.print("Enter new status " + VALID_STATUSES + " (0 to cancel): ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("0")) {
                return null;
            }
            if (VALID_STATUSES.contains(input)) {
                return input;
            }
            System.out.println("Invalid status. Choose one of: " + String.join(", ", VALID_STATUSES));
        }
    }

    // ---------- table helper ----------

    private void printUserTable(List<User> users) {
        if (users.isEmpty()) {
            System.out.println("(no records found)");
            return;
        }
        System.out.println(BORDER);
        System.out.printf(ROW_FORMAT, "ID", "Username", "Role", "Status");
        System.out.println(BORDER);
        for (User u : users) {
            System.out.printf(ROW_FORMAT,
                    u.id(), fit(u.username(), 15), fit(u.role(), 12), fit(u.status(), 10));
        }
        System.out.println(BORDER);
        System.out.println(users.size() + " record(s)");
    }

    private static String fit(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }
}