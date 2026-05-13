package org.reconan.repository;

import org.reconan.database.DatabaseConnection;
import org.reconan.model.Relationship;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing Relationship persistence.
 */
public class RelationshipRepository {

    public void saveAll(int investigationId, List<Relationship> relationships) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            try {
                // 1. Delete existing relationships for this investigation
                String deleteSql = "DELETE FROM relationships WHERE investigation_id = ?";
                PreparedStatement pstmtDel = null;
                try {
                    pstmtDel = conn.prepareStatement(deleteSql);
                    pstmtDel.setInt(1, investigationId);
                    pstmtDel.executeUpdate();
                } finally {
                    DatabaseConnection.close(pstmtDel);
                }

                // 2. Insert new ones
                String insertSql = "INSERT INTO relationships (investigation_id, source_id, target_id, label, created_at) VALUES (?, ?, ?, ?, GETDATE())";
                PreparedStatement pstmtIns = null;
                try {
                    pstmtIns = conn.prepareStatement(insertSql);
                    for (Relationship rel : relationships) {
                        pstmtIns.setInt(1, investigationId);
                        pstmtIns.setInt(2, rel.getSourceId());
                        pstmtIns.setInt(3, rel.getTargetId());
                        pstmtIns.setString(4, rel.getLabel());
                        pstmtIns.addBatch();
                    }
                    pstmtIns.executeBatch();
                } finally {
                    DatabaseConnection.close(pstmtIns);
                }

                conn.commit();
                System.out.println("SQL Server: Successfully saved " + relationships.size() + " relationships.");
            } catch (SQLException e) {
                if (conn != null) conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("SQL Server: Error saving relationships: " + e.getMessage());
        } finally {
            DatabaseConnection.close(conn);
        }
    }

    public List<Relationship> findByInvestigationId(int investigationId) {
        List<Relationship> relationships = new ArrayList<>();
        String sql = "SELECT id, source_id, target_id, label FROM relationships WHERE investigation_id = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pstmt = conn.prepareStatement(sql);

            pstmt.setInt(1, investigationId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Relationship rel = new Relationship(
                        rs.getInt("source_id"),
                        rs.getInt("target_id"),
                        rs.getString("label")
                );
                rel.setId(rs.getInt("id"));
                rel.setInvestigationId(investigationId);
                relationships.add(rel);
            }
        } catch (SQLException e) {
            System.err.println("SQL Server: Error loading relationships: " + e.getMessage());
        } finally {
            DatabaseConnection.close(rs, pstmt, conn);
        }
        return relationships;
    }
}