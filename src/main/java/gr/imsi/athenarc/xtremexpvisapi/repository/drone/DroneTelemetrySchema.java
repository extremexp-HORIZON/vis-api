package gr.imsi.athenarc.xtremexpvisapi.repository.drone;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Centralized schema definition for drone_telemetry table.
 * Automatically handles schema migrations when new fields are added.
 * Supports NOT NULL fields with default values for existing rows.
 */
public class DroneTelemetrySchema {
    
    // Field definitions: columnName -> FieldDefinition
    private static final Map<String, FieldDefinition> FIELDS = new LinkedHashMap<>();
    
    // Primary key columns (must be first in the map)
    private static final String[] PRIMARY_KEY_COLUMNS = {"id", "session_id", "timestamp", "drone_id"};
    
    static {
        // UUID ID from Telegraf tags.id field (VARCHAR to store UUID string)
        // Part of PRIMARY KEY, so must be NOT NULL
        addField("id", "VARCHAR(36) NOT NULL", "tags.id", "id", null);
        
        // Primary keys (always NOT NULL, no defaults needed for new tables)
        addField("session_id", "VARCHAR(100) NOT NULL", "fields.sessionId", "sessionId", null);
        addField("timestamp", "TIMESTAMP NOT NULL", "timestamp", "timestamp", null);
        addField("drone_id", "VARCHAR(50) NOT NULL", "tags.drone_id", "droneId", null);
        
        // GPS Data (nullable fields)
        addField("gps_fix", "BOOLEAN", "fields.gpsFix", "gpsFix", null);
        addField("lat", "DOUBLE", "fields.lat", "lat", null);
        addField("lon", "DOUBLE", "fields.lon", "lon", null);
        addField("alt", "DOUBLE", "fields.alt", "alt", null);
        addField("speed_kmh", "DOUBLE", "fields.speedKmh", "speedKmh", null);
        addField("satellites", "INTEGER", "fields.satellites", "satellites", null);
        
        // LTE Data (nullable fields)
        addField("rat_lte", "VARCHAR(50)", "fields.ratLte", "ratLte", null);
        addField("op_mode_lte", "VARCHAR(50)", "fields.opModeLte", "opModeLte", null);
        addField("mccmnc_lte", "VARCHAR(50)", "fields.mccmncLte", "mccmncLte", null);
        addField("tac_lte", "VARCHAR(50)", "fields.tacLte", "tacLte", null);
        addField("scell_id_lte", "VARCHAR(50)", "fields.scellIdLte", "scellIdLte", null);
        addField("pcell_id_lte", "VARCHAR(50)", "fields.pcellIdLte", "pcellIdLte", null);
        addField("band_lte", "VARCHAR(50)", "fields.bandLte", "bandLte", null);
        addField("earfcn_lte", "INTEGER", "fields.earfcnLte", "earfcnLte", null);
        addField("dlbw_mhz_lte", "INTEGER", "fields.dlbwMhzLte", "dlbwMhzLte", null);
        addField("ulbw_mhz_lte", "INTEGER", "fields.ulbwMhzLte", "ulbwMhzLte", null);
        addField("rsrq_db_lte", "DOUBLE", "fields.rsrqDbLte", "rsrqDbLte", null);
        addField("rsrp_dbm_lte", "DOUBLE", "fields.rsrpDbmLte", "rsrpDbmLte", null);
        addField("rssi_dbm_lte", "DOUBLE", "fields.rssiDbmLte", "rssiDbmLte", null);
        addField("rssnr_db_lte", "DOUBLE", "fields.rssnrDbLte", "rssnrDbLte", null);
        
        // 5G Data (nullable fields)
        addField("rat_5g", "VARCHAR(50)", "fields.rat5g", "rat5g", null);
        addField("pcell_id_5g", "VARCHAR(50)", "fields.pcellId5g", "pcellId5g", null);
        addField("band_5g", "VARCHAR(50)", "fields.band5g", "band5g", null);
        addField("earfcn_5g", "INTEGER", "fields.earfcn5g", "earfcn5g", null);
        addField("rsrq_db_5g", "DOUBLE", "fields.rsrqDb5g", "rsrqDb5g", null);
        addField("rsrp_dbm_5g", "DOUBLE", "fields.rsrpDbm5g", "rsrpDbm5g", null);
        addField("rssnr_db_5g", "DOUBLE", "fields.rssnrDb5g", "rssnrDb5g", null);
    }
    
    /**
     * Add a field definition.
     * 
     * @param columnName Database column name (snake_case)
     * @param sqlType SQL type definition (e.g., "DOUBLE", "VARCHAR(50)", "INTEGER NOT NULL")
     * @param jsonPath JSON path in source data (e.g., "fields.lat", "tags.drone_id")
     * @param javaFieldName Java field name (camelCase) for mapping
     * @param defaultValue Default value for existing rows when adding NOT NULL columns.
     *                     Use null for nullable columns.
     *                     For NOT NULL columns, provide appropriate default:
     *                     - Numbers: "0", "1", "-1"
     *                     - Booleans: "true", "false"
     *                     - Strings: "'default_value'" (with quotes)
     *                     - Timestamps: "CURRENT_TIMESTAMP" or specific timestamp
     */
    private static void addField(String columnName, String sqlType, String jsonPath, 
                                 String javaFieldName, String defaultValue) {
        FIELDS.put(columnName, new FieldDefinition(sqlType, jsonPath, javaFieldName, defaultValue));
    }
    
    /**
     * Generate CREATE TABLE SQL with all fields.
     */
    public static String generateCreateTableSql(String tableName) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (\n");
        
        boolean first = true;
        for (Map.Entry<String, FieldDefinition> entry : FIELDS.entrySet()) {
            if (!first) sql.append(",\n");
            FieldDefinition field = entry.getValue();
            sql.append("    ").append(entry.getKey()).append(" ").append(field.sqlType);
            
            // Add DEFAULT for NOT NULL fields in CREATE TABLE
            if (field.defaultValue != null && field.sqlType.contains("NOT NULL")) {
                sql.append(" DEFAULT ").append(field.defaultValue);
            }
            first = false;
        }
        
        // Generate PRIMARY KEY constraint
        sql.append(",\n    PRIMARY KEY (");
        sql.append(String.join(", ", PRIMARY_KEY_COLUMNS));
        sql.append(")\n)");
        
        return sql.toString();
    }
    
    /**
     * Get the expected primary key columns.
     */
    public static String[] getPrimaryKeyColumns() {
        return PRIMARY_KEY_COLUMNS.clone();
    }
    
    /**
     * Check if the table's primary key matches the expected primary key.
     * Returns true if they match, false otherwise.
     */
    public static boolean checkPrimaryKeyMatches(Connection connection, String tableName) throws SQLException {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getPrimaryKeys(null, null, tableName)) {
                java.util.List<String> existingPkColumns = new java.util.ArrayList<>();
                while (rs.next()) {
                    existingPkColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
                
                // Check if primary keys match
                if (existingPkColumns.size() != PRIMARY_KEY_COLUMNS.length) {
                    return false;
                }
                
                for (String expectedPk : PRIMARY_KEY_COLUMNS) {
                    if (!existingPkColumns.contains(expectedPk.toLowerCase())) {
                        return false;
                    }
                }
                
                return true;
            }
        } catch (SQLException e) {
            // Table might not exist
            if (e.getMessage().contains("does not exist") || 
                e.getMessage().contains("not found")) {
                return false;
            }
            throw e;
        }
    }
    
    /**
     * Migrate existing table by adding any missing columns.
     * Handles both nullable and NOT NULL columns with defaults.
     * 
     * @param connection Database connection
     * @param tableName Table name
     * @return Number of columns added
     * @throws SQLException If migration fails
     */
    public static int migrateTableSchema(Connection connection, String tableName) throws SQLException {
        // Get existing columns from the table
        Set<String> existingColumns = getExistingColumns(connection, tableName);
        
        int columnsAdded = 0;
        
        try (Statement stmt = connection.createStatement()) {
            for (Map.Entry<String, FieldDefinition> entry : FIELDS.entrySet()) {
                String columnName = entry.getKey();
                FieldDefinition field = entry.getValue();
                
                // Skip primary key columns (they should already exist, or table needs recreation)
                if (isPrimaryKeyColumn(columnName)) {
                    continue;
                }
                
                // If column doesn't exist, add it
                if (!existingColumns.contains(columnName.toLowerCase())) {
                    String alterSql = buildAlterTableAddColumnSql(tableName, columnName, field);
                    
                    try {
                        stmt.execute(alterSql);
                        columnsAdded++;
                        System.out.println("Added column: " + columnName + " (" + field.sqlType + 
                                         (field.defaultValue != null ? " DEFAULT " + field.defaultValue : "") + ")");
                    } catch (SQLException e) {
                        // Column might have been added by another process, or there's a real error
                        if (!e.getMessage().contains("already exists") && 
                            !e.getMessage().contains("duplicate column")) {
                            throw e;
                        }
                    }
                }
            }
        }
        
        return columnsAdded;
    }
    
    /**
     * Check if a column is part of the primary key.
     */
    private static boolean isPrimaryKeyColumn(String columnName) {
        for (String pkColumn : PRIMARY_KEY_COLUMNS) {
            if (pkColumn.equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Build ALTER TABLE ADD COLUMN SQL statement.
     * Handles NOT NULL columns with defaults for existing rows.
     */
    private static String buildAlterTableAddColumnSql(String tableName, String columnName, 
                                                      FieldDefinition field) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE ").append(tableName)
           .append(" ADD COLUMN ").append(columnName).append(" ").append(field.sqlType);
        
        // For NOT NULL columns, add DEFAULT value for existing rows
        if (field.sqlType.contains("NOT NULL")) {
            if (field.defaultValue != null) {
                sql.append(" DEFAULT ").append(field.defaultValue);
            } else {
                // Warn if NOT NULL but no default provided
                throw new IllegalStateException(
                    "Column " + columnName + " is NOT NULL but no default value specified. " +
                    "Provide a default value for existing rows when adding NOT NULL columns."
                );
            }
        }
        
        return sql.toString();
    }
    
    /**
     * Get set of existing column names from the table (case-insensitive).
     */
    private static Set<String> getExistingColumns(Connection connection, String tableName) throws SQLException {
        Set<String> columns = new java.util.HashSet<>();
        
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    columns.add(columnName.toLowerCase());
                }
            }
        } catch (SQLException e) {
            // If table doesn't exist yet, return empty set
            if (e.getMessage().contains("does not exist") || 
                e.getMessage().contains("not found")) {
                return columns;
            }
            throw e;
        }
        
        return columns;
    }
    
    /**
     * Generate column list for SELECT and INSERT statements.
     * Includes all columns.
     */
    public static String generateColumnList() {
        return String.join(", ", FIELDS.keySet());
    }
    
    /**
     * Generate JSON field mapping for INSERT query (fields.xxx AS column_name).
     * Includes all fields that have a JSON path defined.
     */
    public static String generateJsonFieldMapping() {
        StringBuilder mapping = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, FieldDefinition> entry : FIELDS.entrySet()) {
            FieldDefinition field = entry.getValue();
            
            // Skip fields without JSON path
            if (field.jsonPath == null) {
                continue;
            }
            
            if (!first) mapping.append(",\n                    ");
            
            // Handle special case for timestamp
            if ("timestamp".equals(entry.getKey())) {
                mapping.append("to_timestamp(").append(field.jsonPath).append(") AS ").append(entry.getKey());
            } else {
                mapping.append(field.jsonPath).append(" AS ").append(entry.getKey());
            }
            first = false;
        }
        return mapping.toString();
    }
    
    /**
     * Get all field definitions for ResultSet mapping.
     */
    public static Map<String, FieldDefinition> getFields() {
        return new LinkedHashMap<>(FIELDS);
    }
    
    /**
     * Field definition containing SQL type, JSON path, Java field name, and default value.
     */
    public static class FieldDefinition {
        final String sqlType;
        final String jsonPath;
        final String javaFieldName;
        final String defaultValue;  // null for nullable columns, or default value for NOT NULL columns
        
        FieldDefinition(String sqlType, String jsonPath, String javaFieldName, String defaultValue) {
            this.sqlType = sqlType;
            this.jsonPath = jsonPath;
            this.javaFieldName = javaFieldName;
            this.defaultValue = defaultValue;
        }
    }
}

