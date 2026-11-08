package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Scanner;

public class Authentication {

    private final Scanner scanner;
    private final UserDao userDao = new UserDao();

    public Authentication(Scanner scanner) {
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n=== SUBDIVISION GATE ENTRY SYSTEM ===");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("0. Exit");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> login();
                case "2" -> register();
                case "0" -> running = false;
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
        System.out.println("Goodbye!");
    }

    private void login() {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        try {
            Optional<User> result = userDao.authenticate(username, password);
            if (result.isEmpty()) {
                System.out.println("Invalid username or password.");
                return;
            }
            User user = result.get();
            if (!"APPROVED".equals(user.status())) {
                System.out.println("Your account is " + user.status() + ". Please wait for admin approval.");
                return;
            }
            routeToMenu(user);
        } catch (SQLException e) {
            System.out.println("Login error: " + e.getMessage());
        }
    }

    private void routeToMenu(User user) {
        switch (user.role()) {
            case "ADMIN"    -> new AdminMenu(scanner, userDao, user).show();
            case "GUARD"    -> new GuardMenu(scanner, user).show();
            case "RESIDENT" -> new ResidentMenu(scanner, user).show();
            default -> System.out.println("Unknown role: " + user.role());
        }
    }

    private void register() {
        try {
            System.out.print("Choose a username: ");
            String username = scanner.nextLine().trim();
            if (username.isEmpty()) {
                System.out.println("Username cannot be empty.");
                return;
            }
            if (userDao.usernameExists(username)) {
                System.out.println("That username is already taken.");
                return;
            }
            System.out.print("Full name: ");
            String fullName = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine();
            if (fullName.isEmpty() || password.isEmpty()) {
                System.out.println("Full name and password are required.");
                return;
            }

            System.out.print("Role (1 = Guard, 2 = Resident): ");
            String role = switch (scanner.nextLine().trim()) {
                case "1" -> "GUARD";
                case "2" -> "RESIDENT";
                default -> null;
            };
            if (role == null) {
                System.out.println("Invalid role selection.");
                return;
            }

            userDao.registerUser(username, password, fullName, role);
            System.out.println("Registration submitted. Your account is PENDING admin approval.");
        } catch (SQLException e) {
            System.out.println("Registration error: " + e.getMessage());
        }
    }
}