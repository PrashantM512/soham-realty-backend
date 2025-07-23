package com.soham.realty.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestDatabaseConnection {
    public static void main(String[] args) {
        String url = "jdbc:mysql://sohamrealty108-soham-realty108.f.aivencloud.com:20154/defaultdb?useSSL=true&requireSSL=true&serverTimezone=UTC";
        String username = "avnadmin";
        String password = "AVNS_0Szw5tfwfCaTs-IXbcJ";
        
        System.out.println("Testing database connection...");
        System.out.println("URL: " + url);
        System.out.println("Username: " + username);
        
        try {
            // Load MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Attempt connection
            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                System.out.println("✅ Connection successful!");
                
                // Test query
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT VERSION()")) {
                    if (rs.next()) {
                        System.out.println("MySQL Version: " + rs.getString(1));
                    }
                }
                
                // List databases
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
                    System.out.println("\nAvailable databases:");
                    while (rs.next()) {
                        System.out.println("  - " + rs.getString(1));
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL driver not found! Add mysql-connector-java to your dependencies.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Connection failed!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
