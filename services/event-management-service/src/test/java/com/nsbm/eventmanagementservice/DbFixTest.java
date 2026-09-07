package com.nsbm.eventmanagementservice;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DbFixTest {
    @Test
    public void fixDb() {
        String url = "jdbc:postgresql://localhost:5432/event_management_db";
        String user = "user";
        String password = "root";
        
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Executing DROP COLUMN...");
            stmt.execute("ALTER TABLE venues DROP COLUMN IF EXISTS venue_type;");
            System.out.println("Column dropped successfully.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
