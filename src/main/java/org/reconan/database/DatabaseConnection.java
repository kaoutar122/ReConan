package org.reconan.database;

import org.reconan.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Handles database connection operations.
 */
public class DatabaseConnection {
    // Create and return a database connection
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.URL,
                DatabaseConfig.USER,
                DatabaseConfig.PASSWORD
        );
    }

    // Check if the database connection is successful
    public static void checkConnection() {
        Connection connection = null;
        try {
            connection = getConnection();
            if (connection != null) {
                System.out.println("SQL Server: Connected to Database Successfully!");
            } else {
                System.out.println("SQL Server: Failed to connect to Database.");
            }
        } catch (SQLException e) {
            // Print connection error
            System.err.println("SQL Server: Database Connection Error: " + e.getMessage());
        } finally {
            close(connection);
        }
    }

    /**
     * Safely closes the given database resources.
     *
     * @param resources The resources to close (Connection, Statement, ResultSet, etc.)
     */
    public static void close(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    System.err.println("SQL Server: Error closing resource: " + e.getMessage());
                }
            }
        }
    }
}
