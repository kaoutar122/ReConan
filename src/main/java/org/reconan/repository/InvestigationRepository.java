package org.reconan.repository;

import org.reconan.database.DatabaseConnection;
import org.reconan.model.Investigation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Investigations.
 */
public class InvestigationRepository {

    /**
     * Creates a new investigation in the database.
     *
     * @param investigation The investigation to create.
     * @return The created investigation with its generated ID, or null if creation failed.
     */
    public Investigation create(Investigation investigation) {
        String sql = "INSERT INTO investigations (name, description, created_at, updated_at) VALUES (?, ?, GETDATE(), GETDATE())";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            pstmt.setString(1, investigation.getName());
            pstmt.setString(2, investigation.getDescription());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    investigation.setId(rs.getInt(1));
                    return investigation;
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Server: Error creating investigation: " + e.getMessage());
        } finally {
            DatabaseConnection.close(rs, pstmt, conn);
        }
        return null;
    }

    /**
     * Retrieves all investigations from the database, ordered by newest first.
     *
     * @return A list of investigations.
     */
    public List<Investigation> findAll() {
        List<Investigation> investigations = new ArrayList<>();
        String sql = "SELECT id, name, description, created_at, updated_at FROM investigations ORDER BY created_at DESC";
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Investigation inv = new Investigation(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                );
                investigations.add(inv);
            }
        } catch (SQLException e) {
            System.err.println("SQL Server: Error retrieving investigations: " + e.getMessage());
        } finally {
            DatabaseConnection.close(rs, stmt, conn);
        }
        return investigations;
    }

    /**
     * Retrieves an investigation by its ID.
     *
     * @param id The ID of the investigation.
     * @return The investigation, or null if not found.
     */
    public Investigation findById(int id) {
        String sql = "SELECT id, name, description, created_at, updated_at FROM investigations WHERE id = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            
            pstmt.setInt(1, id);
            
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Investigation(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getTimestamp("updated_at").toLocalDateTime()
                );
            }
        } catch (SQLException e) {
            System.err.println("SQL Server: Error retrieving investigation by ID: " + e.getMessage());
        } finally {
            DatabaseConnection.close(rs, pstmt, conn);
        }
        return null;
    }
}
