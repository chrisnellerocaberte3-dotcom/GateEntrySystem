package com.scc.ui;

import com.scc.dao.ResidentDao;
import com.scc.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;


public class ResidentMenu {

    private final Scanner scanner;
    private final User user;
    private final ResidentDao residentDao = new ResidentDao();

    public ResidentMenu(Scanner scanner, User user) {
        this.scanner = scanner;
        this.user = user;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== RESIDENT MENU (" + user.fullName() + ") ===");
            System.out.println("1. View My Account");
            System.out.println("2. View My Vehicles");
            System.out.println("3. View My Visitor Entries");
            System.out.println("0. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> viewAccount();
                case "2" -> viewVehicles();
                case "3" -> viewVisitorEntries();
                case "0" -> running = false;
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private void viewAccount() {
        System.out.println("\n--- My Account ---");
        System.out.printf("%-12s: %d%n", "User ID", user.id());
        System.out.printf("%-12s: %s%n", "Username", user.username());
        System.out.printf("%-12s: %s%n", "Full Name", user.fullName());
        System.out.printf("%-12s: %s%n", "Role", user.role());
        System.out.printf("%-12s: %s%n", "Status", user.status());
    }

    private void viewVehicles() {
        try {
            List<String[]> rows = residentDao.getVehiclesByResidentName(user.fullName());
            System.out.println("\n--- My Vehicles ---");
            if (rows.isEmpty()) {
                System.out.println("(no vehicles registered under your name)");
                return;
            }
            String fmt = "| %-12s | %-12s | %-10s |%n";
            String border = "+" + "-".repeat(14) + "+" + "-".repeat(14) + "+" + "-".repeat(12) + "+";
            System.out.println(border);
            System.out.printf(fmt, "Plate No.", "Type", "Color");
            System.out.println(border);
            for (String[] r : rows) {
                System.out.printf(fmt, dash(r[0]), dash(r[1]), dash(r[2]));
            }
            System.out.println(border);
        } catch (SQLException e) {
            System.out.println("Error loading vehicles: " + e.getMessage());
        }
    }

    private void viewVisitorEntries() {
        try {
            List<String[]> rows = residentDao.getGateEntriesByResidentName(user.fullName());
            System.out.println("\n--- My Visitor Entries ---");
            if (rows.isEmpty()) {
                System.out.println("(no visitor entries found)");
                return;
            }
            String fmt = "| %-15s | %-15s | %-10s | %-16s | %-16s | %-8s |%n";
            String border = "+" + "-".repeat(17) + "+" + "-".repeat(17) + "+" + "-".repeat(12) + "+"
                    + "-".repeat(18) + "+" + "-".repeat(18) + "+" + "-".repeat(10) + "+";
            System.out.println(border);
            System.out.printf(fmt, "Visitor", "Purpose", "Plate No.", "Time In", "Time Out", "Status");
            System.out.println(border);
            for (String[] r : rows) {
                System.out.printf(fmt, fit(dash(r[0]), 15), fit(dash(r[1]), 15), fit(dash(r[2]), 10),
                        fit(dash(r[3]), 16), fit(dash(r[4]), 16), fit(dash(r[5]), 8));
            }
            System.out.println(border);
            System.out.println(rows.size() + " record(s)");
        } catch (SQLException e) {
            System.out.println("Error loading visitor entries: " + e.getMessage());
        }
    }

    private static String dash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private static String fit(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }
}