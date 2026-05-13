package org.reconan.repository;

import org.reconan.database.DatabaseConnection;
import org.reconan.model.Entity;
import org.reconan.model.EntityType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Repository for managing Entity persistence.
 */
public class EntityRepository {

    /**
     * Saves all entities for an investigation.
     * Simplistic approach: Delete existing and re-insert (or use MERGE if SQL Server).
     * For this OSINT tool, we'll clear and re-save for the specific investigation to ensure sync.
     */
    public void saveAll(int investigationId, List<Entity> entities) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            try {
                // 1. Delete existing properties
                String deletePropsSql = "DELETE FROM entity_properties WHERE entity_id IN (SELECT id FROM entities WHERE investigation_id = ?)";
                PreparedStatement pstmtDelProps = null;
                try {
                    pstmtDelProps = conn.prepareStatement(deletePropsSql);
                    pstmtDelProps.setInt(1, investigationId);
                    pstmtDelProps.executeUpdate();
                } finally {
                    DatabaseConnection.close(pstmtDelProps);
                }

                // 2. Delete existing entities
                String deleteEntitiesSql = "DELETE FROM entities WHERE investigation_id = ?";
                PreparedStatement pstmtDelEntities = null;
                try {
                    pstmtDelEntities = conn.prepareStatement(deleteEntitiesSql);
                    pstmtDelEntities.setInt(1, investigationId);
                    pstmtDelEntities.executeUpdate();
                } finally {
                    DatabaseConnection.close(pstmtDelEntities);
                }

                // 3. Insert new entities and their properties
                String insertEntitySql = "INSERT INTO entities (investigation_id, type, value, created_at, updated_at) VALUES (?, ?, ?, GETDATE(), GETDATE())";
                String insertPropSql = "INSERT INTO entity_properties (entity_id, property_key, property_value) VALUES (?, ?, ?)";

                for (Entity entity : entities) {
                    try (PreparedStatement pstmt = conn.prepareStatement(insertEntitySql, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt.setInt(1, investigationId);
                        pstmt.setString(2, entity.getType().name());
                        pstmt.setString(3, entity.getValue());
                        pstmt.executeUpdate();

                        try (ResultSet rs = pstmt.getGeneratedKeys()) {
                            if (rs.next()) {
                                int newId = rs.getInt(1);
                                entity.setId(newId);
                                
                                // Insert properties
                                if (!entity.getProperties().isEmpty()) {
                                    try (PreparedStatement propPstmt = conn.prepareStatement(insertPropSql)) {
                                        for (Map.Entry<String, String> entry : entity.getProperties().entrySet()) {
                                            propPstmt.setInt(1, newId);
                                            propPstmt.setString(2, entry.getKey());
                                            propPstmt.setString(3, entry.getValue());
                                            propPstmt.addBatch();
                                        }
                                        propPstmt.executeBatch();
                                    }
                                }
                            }
                        }
                    }
                }

                conn.commit();
                System.out.println("SQL Server: Successfully saved " + entities.size() + " entities.");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("SQL Server: Error saving entities: " + e.getMessage());
        }
    }

    /**
     * Finds all entities for a given investigation.
     */
    public List<Entity> findByInvestigationId(int investigationId) {
        List<Entity> entities = new ArrayList<>();
        String sql = "SELECT id, type, value FROM entities WHERE investigation_id = ?";
        
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            
            pstmt.setInt(1, investigationId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Entity entity = new Entity(
                    EntityType.valueOf(rs.getString("type")),
                    rs.getString("value")
                );
                entity.setId(rs.getInt("id"));
                entity.setInvestigationId(investigationId);
                
                // Load properties
                loadProperties(entity, conn);
                
                entities.add(entity);
            }
        } catch (SQLException e) {
            System.err.println("SQL Server: Error loading entities: " + e.getMessage());
        } finally {
            DatabaseConnection.close(rs, pstmt, conn);
        }
        return entities;
    }

    private void loadProperties(Entity entity, Connection conn) throws SQLException {
        String sql = "SELECT property_key, property_value FROM entity_properties WHERE entity_id = ?";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, entity.getId());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                entity.addProperty(rs.getString("property_key"), rs.getString("property_value"));
            }
        } finally {
            DatabaseConnection.close(rs, pstmt);
        }
    }
}