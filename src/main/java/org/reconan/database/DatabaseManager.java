package org.reconan.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton manager to handle database initialization and schema validation.
 */
public class DatabaseManager {
    private static DatabaseManager instance;

    private DatabaseManager() {
        // Private constructor for Singleton
    }

    /**
     * Retrieves the thread-safe singleton instance of DatabaseManager.
     * @return The DatabaseManager instance
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initializes the database schema by creating required tables if they do not exist.
     * 
     * @throws SQLException if a database access error occurs
     */
    public void initialize() throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            
            System.out.println("SQL Server: Initializing database schema...");

            // 1. investigations
            stmt.execute(
                "IF OBJECT_ID('investigations', 'U') IS NULL\n" +
                "BEGIN\n" +
                "    CREATE TABLE investigations (\n" +
                "        id INT IDENTITY(1,1) PRIMARY KEY,\n" +
                "        name NVARCHAR(255) NOT NULL,\n" +
                "        description NVARCHAR(MAX),\n" +
                "        created_at DATETIME DEFAULT GETDATE(),\n" +
                "        updated_at DATETIME DEFAULT GETDATE()\n" +
                "    );\n" +
                "END;"
            );

            // 2. entities
            stmt.execute(
                "IF OBJECT_ID('entities', 'U') IS NULL\n" +
                "BEGIN\n" +
                "    CREATE TABLE entities (\n" +
                "        id INT IDENTITY(1,1) PRIMARY KEY,\n" +
                "        investigation_id INT NOT NULL FOREIGN KEY REFERENCES investigations(id),\n" +
                "        type VARCHAR(50) NOT NULL,\n" +
                "        value NVARCHAR(MAX) NOT NULL,\n" +
                "        created_at DATETIME DEFAULT GETDATE(),\n" +
                "        updated_at DATETIME DEFAULT GETDATE()\n" +
                "    );\n" +
                "END;"
            );

            // 3. entity_properties
            stmt.execute(
                "IF OBJECT_ID('entity_properties', 'U') IS NULL\n" +
                "BEGIN\n" +
                "    CREATE TABLE entity_properties (\n" +
                "        entity_id INT NOT NULL FOREIGN KEY REFERENCES entities(id) ON DELETE CASCADE,\n" +
                "        property_key VARCHAR(100) NOT NULL,\n" +
                "        property_value NVARCHAR(MAX),\n" +
                "        PRIMARY KEY (entity_id, property_key)\n" +
                "    );\n" +
                "END;"
            );

            // 4. relationships
            stmt.execute(
                "IF OBJECT_ID('relationships', 'U') IS NULL\n" +
                "BEGIN\n" +
                "    CREATE TABLE relationships (\n" +
                "        id INT IDENTITY(1,1) PRIMARY KEY,\n" +
                "        investigation_id INT NOT NULL FOREIGN KEY REFERENCES investigations(id),\n" +
                "        source_id INT NOT NULL FOREIGN KEY REFERENCES entities(id),\n" +
                "        target_id INT NOT NULL FOREIGN KEY REFERENCES entities(id),\n" +
                "        label VARCHAR(100) NOT NULL,\n" +
                "        created_at DATETIME DEFAULT GETDATE()\n" +
                "    );\n" +
                "END;"
            );

            // 5. relationship_properties
            stmt.execute(
                "IF OBJECT_ID('relationship_properties', 'U') IS NULL\n" +
                "BEGIN\n" +
                "    CREATE TABLE relationship_properties (\n" +
                "        relationship_id INT NOT NULL FOREIGN KEY REFERENCES relationships(id) ON DELETE CASCADE,\n" +
                "        property_key VARCHAR(100) NOT NULL,\n" +
                "        property_value NVARCHAR(MAX),\n" +
                "        PRIMARY KEY (relationship_id, property_key)\n" +
                "    );\n" +
                "END;"
            );

            // 6. transform_history
            stmt.execute(
                "IF OBJECT_ID('transform_history', 'U') IS NULL\n" +
                "BEGIN\n" +
                "    CREATE TABLE transform_history (\n" +
                "        id INT IDENTITY(1,1) PRIMARY KEY,\n" +
                "        investigation_id INT NOT NULL FOREIGN KEY REFERENCES investigations(id),\n" +
                "        entity_id INT NOT NULL FOREIGN KEY REFERENCES entities(id),\n" +
                "        transform_name VARCHAR(255) NOT NULL,\n" +
                "        status VARCHAR(50) NOT NULL,\n" +
                "        run_at DATETIME DEFAULT GETDATE()\n" +
                "    );\n" +
                "END;"
            );

            System.out.println("SQL Server: Database schema initialized successfully.");
        } finally {
            DatabaseConnection.close(stmt, conn);
        }
    }
}
