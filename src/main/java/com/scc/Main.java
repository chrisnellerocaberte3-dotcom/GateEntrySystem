package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.ui.Authentication;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        if (!DatabaseInitializer.initialize()) {
            System.err.println("Cannot start: database setup failed.");
            System.exit(1);
        }
        try (Scanner scanner = new Scanner(System.in)) {
            new Authentication(scanner).start();
        }
    }
}