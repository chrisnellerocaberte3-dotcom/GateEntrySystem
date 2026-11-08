package com.scc.ui;

import com.scc.dao.GateEntryDao;
import com.scc.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class GuardMenu {

    private final Scanner scanner;
    private final User user;
    private final GateEntryDao gateDao = new GateEntryDao();

    public GuardMenu(Scanner scanner, User user) {
        this.scanner = scanner;
        this.user = user;
    }

    public void show() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== GUARD MENU (" + user.fullName() + ") ===");
            System.out.println("1. Record Visitor Entry");
            System.out.println("2. View Visitors Currently Inside");
            System.out.println("3. Record Visitor Time-Out");
            System.out.println("4. Search Resident / Vehicle");
            System.out.println("0. Logout");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> recordEntry();
                case "2" -> viewInside();
                case "3" -> recordTimeOut();
                case "4" -> searchResidents();
                case "0" -> running = false;
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private void recordEntry() {
        try {
            System.out.println("\n--- Record Visitor Entry ---");
            int residentId;
            String residentName;
            while (true) {
                System.out.print("Resident ID to visit (0 to cancel, use option 4 to look up): ");
                Integer id = parseInt(scanner.nextLine());
                if (id == null) {
                    System.out.println("Please enter a valid number.");
                    continue;
                }
                if (id == 0) {
                    System.out.println("Entry cancelled.");
                    return;
                }
                Optional<String> name = gateDao.findActiveResidentName(id);
                if (name.isEmpty()) {
                    System.out.println("No active resident with ID " + id + ".");
                    continue;
                }
                residentId = id;
                residentName = name.get();
                break;
            }

            String visitor = promptRequired("Visitor name: ");
            String purpose = promptRequired("Purpose of visit: ");
            System.out.print("Vehicle plate number (leave blank if none): ");
            String plate = scanner.nextLine().trim();

            int entryId = gateDao.addEntry(user.id(), residentId, visitor, purpose, plate.isEmpty() ? null : plate);
            System.out.println("\nSUCCESS: Gate pass created.");
            System.out.println("  Pass / Entry ID : " + entryId);
            System.out.println("  Visitor         : " + visitor);
            System.out.println("  Visiting        : " + residentName);
            System.out.println("  Status          : INSIDE");
        } catch (SQLException e) {
            System.out.println("Error recording entry: " + e.getMessage());
        }
    }

    private void viewInside() {
        try {
            System.out.println("\n--- Visitors Currently Inside ---");
            printInsideTable(gateDao.getVisitorsInside());
        } catch (SQLException e) {
            System.out.println("Error loading visitors: " + e.getMessage());
        }
    }

    private void recordTimeOut() {
        try {
            List<String[]> inside = gateDao.getVisitorsInside();
            System.out.println("\n--- Record Visitor Time-Out ---");
            printInsideTable(inside);
            if (inside.isEmpty()) {
                return;
            }
            while (true) {
                System.out.print("Entry ID to time-out (0 to cancel): ");
                Integer id = parseInt(scanner.nextLine());
                if (id == null) {
                    System.out.println("Please enter a valid number.");
                    continue;
                }
                if (id == 0) {
                    System.out.println("Time-out cancelled.");
                    return;
                }
                boolean listed = inside.stream().anyMatch(r -> r[0].equals(String.valueOf(id)));
                if (!listed) {
                    System.out.println("Entry ID " + id + " is not in the list above. Try again.");
                    continue;
                }
                if (gateDao.recordTimeOut(user.id(), id)) {
                    System.out.println("SUCCESS: Entry " + id + " marked EXITED.");
                } else {
                    System.out.println("Update failed: the entry may already be closed.");
                }
                return;
            }
        } catch (SQLException e) {
            System.out.println("Error recording time-out: " + e.getMessage());
        }
    }

    private void searchResidents() {
        try {
            System.out.print("\nSearch by resident name, address or plate number: ");
            String keyword = scanner.nextLine().trim();
            if (keyword.isEmpty()) {
                System.out.println("Search cancelled.");
                return;
            }
            List<String[]> rows = gateDao.searchResidents(keyword);
            System.out.println("\n--- Search Results ---");
            if (rows.isEmpty()) {
                System.out.println("(no matches found)");
                return;
            }
            printTable(new String[] {"ID", "Resident", "Address", "Status", "Plate No(s)."},
                    new int[] {5, 20, 18, 10, 20}, rows);
        } catch (SQLException e) {
            System.out.println("Error searching: " + e.getMessage());
        }
    }

    // ---------- helpers ----------

    private void printInsideTable(List<String[]> rows) {
        if (rows.isEmpty()) {
            System.out.println("(no visitors currently inside)");
            return;
        }
        printTable(new String[] {"ID", "Visitor", "Resident", "Address", "Purpose", "Plate No.", "Time In"},
                new int[] {5, 14, 14, 14, 12, 10, 16}, rows);
    }

    private void printTable(String[] headers, int[] widths, List<String[]> rows) {
        StringBuilder border = new StringBuilder("+");
        StringBuilder fmt = new StringBuilder("|");
        for (int w : widths) {
            border.append("-".repeat(w + 2)).append("+");
            fmt.append(" %-").append(w).append("s |");
        }
        fmt.append("%n");

        System.out.println(border);
        System.out.printf(fmt.toString(), (Object[]) headers);
        System.out.println(border);
        for (String[] row : rows) {
            Object[] cells = new Object[widths.length];
            for (int i = 0; i < widths.length; i++) {
                cells[i] = fit(row[i], widths[i]);
            }
            System.out.printf(fmt.toString(), cells);
        }
        System.out.println(border);
        System.out.println(rows.size() + " record(s)");
    }

    private String promptRequired(String label) {
        while (true) {
            System.out.print(label);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This field is required.");
        }
    }

    private static Integer parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String fit(String s, int max) {
        if (s == null || s.isBlank()) {
            return "-";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }
}