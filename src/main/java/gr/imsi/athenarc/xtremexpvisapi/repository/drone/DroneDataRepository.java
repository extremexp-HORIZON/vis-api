package gr.imsi.athenarc.xtremexpvisapi.repository.drone;

import gr.imsi.athenarc.xtremexpvisapi.domain.drone.DroneData;
import gr.imsi.athenarc.xtremexpvisapi.domain.drone.DroneTelemetryRequest;
import jakarta.annotation.PostConstruct;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for accessing drone telemetry data from DuckDB.
 * Handles all database operations for drone telemetry queries.
 */
@Repository
@Log
public class DroneDataRepository {

    @Autowired
    private DataSource dataSource;

    @Value("${duckdb.drone.database.path:}")
    private String droneDatabasePath;

    @Value("${duckdb.drone.table.name:drone_telemetry}")
    private String tableName;

    @Value("${duckdb.drone.jsonl.path:}")
    private String jsonlPath;

    /**
     * Initialize the DuckDB connection and create table if needed.
     * If jsonlPath is configured, uses direct JSONL reading.
     * Otherwise, uses a persistent DuckDB table.
     */
    @PostConstruct
    public void init() {
        try (Connection connection = dataSource.getConnection()) {
            if (jsonlPath != null && !jsonlPath.isBlank()) {
                log.info("DroneDataRepository initialized with JSONL path: " + jsonlPath);
            } else if (droneDatabasePath != null && !droneDatabasePath.isBlank()) {
                // Create persistent database connection
                try (Statement stmt = connection.createStatement()) {
                    // Attach or create persistent database
                    String attachSql = String.format("ATTACH '%s' AS drone_db (READ_ONLY)", droneDatabasePath);
                    stmt.execute(attachSql);
                    log.info("DroneDataRepository initialized with persistent database: " + droneDatabasePath);
                }
            } else {
                // Use in-memory table - create schema
                createTableIfNotExists(connection);
                log.info("DroneDataRepository initialized with in-memory table");
            }
        } catch (SQLException e) {
            log.warning("Failed to initialize DroneDataRepository: " + e.getMessage());
            // Don't throw - allow application to start even if table doesn't exist yet
        }
    }

    /**
     * Create the drone_telemetry table if it doesn't exist.
     */
    private void createTableIfNotExists(Connection connection) throws SQLException {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS %s (
                timestamp TIMESTAMP NOT NULL,
                drone_id VARCHAR(50) NOT NULL,
                gps_fix BOOLEAN,
                lat DOUBLE,
                lon DOUBLE,
                alt DOUBLE,
                speed_kmh DOUBLE,
                satellites INTEGER,
                rat_lte VARCHAR(50),
                op_mode_lte VARCHAR(50),
                mccmnc_lte VARCHAR(50),
                tac_lte VARCHAR(50),
                scell_id_lte VARCHAR(50),
                pcell_id_lte VARCHAR(50),
                band_lte VARCHAR(50),
                earfcn_lte INTEGER,
                dlbw_mhz_lte INTEGER,
                ulbw_mhz_lte INTEGER,
                rsrq_db_lte DOUBLE,
                rsrp_dbm_lte DOUBLE,
                rssi_dbm_lte DOUBLE,
                rssnr_db_lte DOUBLE,
                rat_5g VARCHAR(50),
                pcell_id_5g VARCHAR(50),
                band_5g VARCHAR(50),
                earfcn_5g INTEGER,pom
                rsrq_db_5g DOUBLE,
                rsrp_dbm_5g DOUBLE,
                rssnr_db_5g DOUBLE,
                PRIMARY KEY (timestamp, drone_id)
            )
            """.formatted(tableName);

        String createIndex1 = String.format(
            "CREATE INDEX IF NOT EXISTS idx_drone_telemetry_drone_id ON %s(drone_id)", tableName);
        String createIndex2 = String.format(
            "CREATE INDEX IF NOT EXISTS idx_drone_telemetry_timestamp ON %s(timestamp)", tableName);
        String createIndex3 = String.format(
            "CREATE INDEX IF NOT EXISTS idx_drone_telemetry_location ON %s(lat, lon)", tableName);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSql);
            stmt.execute(createIndex1);
            stmt.execute(createIndex2);
            stmt.execute(createIndex3);
            log.info("Created drone_telemetry table and indexes");
        }
    }

    /**
     * Find telemetry data based on the provided request filters.
     *
     * @param request The telemetry request with filters
     * @return List of DroneData matching the criteria
     */
    public List<DroneData> findByRequest(DroneTelemetryRequest request) throws SQLException {
        String sql = buildQuery(request);
        List<DroneData> results = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            setQueryParameters(pstmt, request, true);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToDroneData(rs));
                }
            }
        }

        return results;
    }

    /**
     * Count total records matching the request criteria (before pagination).
     *
     * @param request The telemetry request with filters
     * @return Total count of matching records
     */
    public int countByRequest(DroneTelemetryRequest request) throws SQLException {
        String countSql = buildCountQuery(request);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(countSql)) {
            
            setQueryParameters(pstmt, request, false);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    /**
     * Find the latest telemetry record for a specific drone.
     *
     * @param droneId The drone ID
     * @return Optional containing the latest DroneData, or empty if not found
     */
    public Optional<DroneData> findLatestByDroneId(String droneId) throws SQLException {
        String sql;
        if (jsonlPath != null && !jsonlPath.isBlank()) {
            sql = """
            SELECT 
                tags.drone_id as drone_id,
                to_timestamp(timestamp) as timestamp,
                fields.gpsFix as gps_fix,
                fields.lat as lat,
                fields.lon as lon,
                fields.alt as alt,
                fields.speedKmh as speed_kmh,
                fields.satellites as satellites,
                fields.ratLte as rat_lte,
                fields.opModeLte as op_mode_lte,
                fields.mccmncLte as mccmnc_lte,
                fields.tacLte as tac_lte,
                fields.scellIdLte as scell_id_lte,
                fields.pcellIdLte as pcell_id_lte,
                fields.bandLte as band_lte,
                fields.earfcnLte as earfcn_lte,
                fields.dlbwMhzLte as dlbw_mhz_lte,
                fields.ulbwMhzLte as ulbw_mhz_lte,
                fields.rsrqDbLte as rsrq_db_lte,
                fields.rsrpDbmLte as rsrp_dbm_lte,
                fields.rssiDbmLte as rssi_dbm_lte,
                fields.rssnrDbLte as rssnr_db_lte,
                fields.rat5g as rat_5g,
                fields.pcellId5g as pcell_id_5g,
                fields.band5g as band_5g,
                fields.earfcn5g as earfcn_5g,
                fields.rsrqDb5g as rsrq_db_5g,
                fields.rsrpDbm5g as rsrp_dbm_5g,
                fields.rssnrDb5g as rssnr_db_5g
            FROM read_json_auto(?)
            WHERE tags.drone_id = ?
            ORDER BY to_timestamp(timestamp) DESC
            LIMIT 1
            """;
        } else {
            sql = String.format(
                "SELECT * FROM %s WHERE drone_id = ? ORDER BY timestamp DESC LIMIT 1", tableName);
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            if (jsonlPath != null && !jsonlPath.isBlank()) {
                pstmt.setString(1, jsonlPath);
                pstmt.setString(2, droneId);
            } else {
                pstmt.setString(1, droneId);
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDroneData(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Find all unique drone IDs in the database.
     *
     * @return List of unique drone IDs
     */
    public List<String> findAllDroneIds() throws SQLException {
        List<String> droneIds = new ArrayList<>();
        String sql;
        
        if (jsonlPath != null && !jsonlPath.isBlank()) {
            sql = "SELECT DISTINCT tags.drone_id as drone_id FROM read_json_auto(?) ORDER BY drone_id";
        } else {
            sql = String.format("SELECT DISTINCT drone_id FROM %s ORDER BY drone_id", tableName);
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            if (jsonlPath != null && !jsonlPath.isBlank()) {
                pstmt.setString(1, jsonlPath);
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    droneIds.add(rs.getString("drone_id"));
                }
            }
        }

        return droneIds;
    }

    /**
     * Build the SQL query based on request filters.
     */
    private String buildQuery(DroneTelemetryRequest request) {
        StringBuilder sql = new StringBuilder();
        
        if (jsonlPath != null && !jsonlPath.isBlank()) {
            sql.append("""
        SELECT 
            tags.drone_id as drone_id,
            to_timestamp(timestamp) as timestamp,
            fields.gpsFix as gps_fix,
            fields.lat as lat,
            fields.lon as lon,
            fields.alt as alt,
            fields.speedKmh as speed_kmh,
            fields.satellites as satellites,
            fields.ratLte as rat_lte,
            fields.opModeLte as op_mode_lte,
            fields.mccmncLte as mccmnc_lte,
            fields.tacLte as tac_lte,
            fields.scellIdLte as scell_id_lte,
            fields.pcellIdLte as pcell_id_lte,
            fields.bandLte as band_lte,
            fields.earfcnLte as earfcn_lte,
            fields.dlbwMhzLte as dlbw_mhz_lte,
            fields.ulbwMhzLte as ulbw_mhz_lte,
            fields.rsrqDbLte as rsrq_db_lte,
            fields.rsrpDbmLte as rsrp_dbm_lte,
            fields.rssiDbmLte as rssi_dbm_lte,
            fields.rssnrDbLte as rssnr_db_lte,
            fields.rat5g as rat_5g,
            fields.pcellId5g as pcell_id_5g,
            fields.band5g as band_5g,
            fields.earfcn5g as earfcn_5g,
            fields.rsrqDb5g as rsrq_db_5g,
            fields.rsrpDbm5g as rsrp_dbm_5g,
            fields.rssnrDb5g as rssnr_db_5g
        FROM read_json_auto(?)
        """);
        } else {
            sql.append("SELECT * FROM ").append(tableName);
        }
        
        List<String> conditions = new ArrayList<>();
        
        // Drone ID filter
        if (request.getDroneId() != null && !request.getDroneId().isBlank()) {
            conditions.add("drone_id = ?");
        } else if (request.getDroneIds() != null && !request.getDroneIds().isEmpty()) {
            String placeholders = "?,".repeat(request.getDroneIds().size());
            conditions.add("drone_id IN (" + placeholders.substring(0, placeholders.length() - 1) + ")");
        }
        
        // Time range filters
        if (request.getStartTime() != null) {
            if (jsonlPath != null && !jsonlPath.isBlank()) {
                conditions.add("to_timestamp(timestamp) >= ?");
            } else {
                conditions.add("timestamp >= ?");
            }
        }
        if (request.getEndTime() != null) {
            if (jsonlPath != null && !jsonlPath.isBlank()) {
                conditions.add("to_timestamp(timestamp) <= ?");
            } else {
                conditions.add("timestamp <= ?");
            }
        }
        
        // Geographic bounding box filters
        if (request.getMinLat() != null) {
            conditions.add("lat >= ?");
        }
        if (request.getMaxLat() != null) {
            conditions.add("lat <= ?");
        }
        if (request.getMinLon() != null) {
            conditions.add("lon >= ?");
        }
        if (request.getMaxLon() != null) {
            conditions.add("lon <= ?");
        }
        
        // Add WHERE clause if there are conditions
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        
        // Sorting
        String sortOrder = request.getSortOrder() != null ? request.getSortOrder() : "timestamp_desc";
        if (jsonlPath != null && !jsonlPath.isBlank()) {
            if (sortOrder.equals("timestamp_asc")) {
                sql.append(" ORDER BY to_timestamp(timestamp) ASC");
            } else {
                sql.append(" ORDER BY to_timestamp(timestamp) DESC");
            }
        } else {
            if (sortOrder.equals("timestamp_asc")) {
                sql.append(" ORDER BY timestamp ASC");
            } else {
                sql.append(" ORDER BY timestamp DESC");
            }
        }
        
        // Pagination
        if (request.getLimit() != null && request.getLimit() > 0) {
            sql.append(" LIMIT ?");
        }
        if (request.getOffset() != null && request.getOffset() > 0) {
            sql.append(" OFFSET ?");
        }
        
        return sql.toString();
    }

    /**
     * Build the count query (same filters, no pagination).
     */
    private String buildCountQuery(DroneTelemetryRequest request) {
        StringBuilder sql = new StringBuilder();
        
        if (jsonlPath != null && !jsonlPath.isBlank()) {
            sql.append("SELECT COUNT(*) FROM read_json_auto(?)");
        } else {
            sql.append("SELECT COUNT(*) FROM ").append(tableName);
        }
        
        List<String> conditions = new ArrayList<>();
        
        // Same conditions as buildQuery
        if (request.getDroneId() != null && !request.getDroneId().isBlank()) {
            conditions.add("drone_id = ?");
        } else if (request.getDroneIds() != null && !request.getDroneIds().isEmpty()) {
            String placeholders = "?,".repeat(request.getDroneIds().size());
            conditions.add("drone_id IN (" + placeholders.substring(0, placeholders.length() - 1) + ")");
        }
        
        if (request.getStartTime() != null) {
            if (jsonlPath != null && !jsonlPath.isBlank()) {
                conditions.add("to_timestamp(timestamp) >= ?");
            } else {
                conditions.add("timestamp >= ?");
            }
        }
        if (request.getEndTime() != null) {
            if (jsonlPath != null && !jsonlPath.isBlank()) {
                conditions.add("to_timestamp(timestamp) <= ?");
            } else {
                conditions.add("timestamp <= ?");
            }
        }
        if (request.getMinLat() != null) {
            conditions.add("lat >= ?");
        }
        if (request.getMaxLat() != null) {
            conditions.add("lat <= ?");
        }
        if (request.getMinLon() != null) {
            conditions.add("lon >= ?");
        }
        if (request.getMaxLon() != null) {
            conditions.add("lon <= ?");
        }
        
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        
        return sql.toString();
    }

    /**
     * Set query parameters on the prepared statement.
     * @param pstmt The prepared statement
     * @param request The telemetry request with filters
     * @param includePagination Whether to include LIMIT and OFFSET parameters (false for count queries)
     */
    private void setQueryParameters(PreparedStatement pstmt, DroneTelemetryRequest request, boolean includePagination) throws SQLException {
        int paramIndex = 1;
        
        // JSONL path parameter (if using JSONL)
        if (jsonlPath != null && !jsonlPath.isBlank()) {
            pstmt.setString(paramIndex++, jsonlPath);
        }
        
        // Drone ID filter
        if (request.getDroneId() != null && !request.getDroneId().isBlank()) {
            pstmt.setString(paramIndex++, request.getDroneId());
        } else if (request.getDroneIds() != null && !request.getDroneIds().isEmpty()) {
            for (String droneId : request.getDroneIds()) {
                pstmt.setString(paramIndex++, droneId);
            }
        }
        
        // Time range filters
        if (request.getStartTime() != null) {
            pstmt.setTimestamp(paramIndex++, Timestamp.from(request.getStartTime()));
        }
        if (request.getEndTime() != null) {
            pstmt.setTimestamp(paramIndex++, Timestamp.from(request.getEndTime()));
        }
        
        // Geographic filters
        if (request.getMinLat() != null) {
            pstmt.setDouble(paramIndex++, request.getMinLat());
        }
        if (request.getMaxLat() != null) {
            pstmt.setDouble(paramIndex++, request.getMaxLat());
        }
        if (request.getMinLon() != null) {
            pstmt.setDouble(paramIndex++, request.getMinLon());
        }
        if (request.getMaxLon() != null) {
            pstmt.setDouble(paramIndex++, request.getMaxLon());
        }
        
        // Pagination (only for main query, not count query)
        if (includePagination) {
            if (request.getLimit() != null && request.getLimit() > 0) {
                pstmt.setInt(paramIndex++, request.getLimit());
            }
            if (request.getOffset() != null && request.getOffset() > 0) {
                pstmt.setInt(paramIndex++, request.getOffset());
            }
        }
    }

    /**
     * Map a ResultSet row to a DroneData object.
     */
    private DroneData mapResultSetToDroneData(ResultSet rs) throws SQLException {
        DroneData data = new DroneData();
        
        // Identifiers
        data.setDroneId(rs.getString("drone_id"));
        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            data.setTimestamp(timestamp.toInstant());
        }
        
        // GPS Data
        data.setGpsFix(getBoolean(rs, "gps_fix"));
        data.setLat(getDouble(rs, "lat"));
        data.setLon(getDouble(rs, "lon"));
        data.setAlt(getDouble(rs, "alt"));
        data.setSpeedKmh(getDouble(rs, "speed_kmh"));
        data.setSatellites(getInteger(rs, "satellites"));
        
        // LTE Data
        data.setRatLte(rs.getString("rat_lte"));
        data.setOpModeLte(rs.getString("op_mode_lte"));
        data.setMccmncLte(rs.getString("mccmnc_lte"));
        data.setTacLte(rs.getString("tac_lte"));
        data.setScellIdLte(rs.getString("scell_id_lte"));
        data.setPcellIdLte(rs.getString("pcell_id_lte"));
        data.setBandLte(rs.getString("band_lte"));
        data.setEarfcnLte(getInteger(rs, "earfcn_lte"));
        data.setDlbwMhzLte(getInteger(rs, "dlbw_mhz_lte"));
        data.setUlbwMhzLte(getInteger(rs, "ulbw_mhz_lte"));
        data.setRsrqDbLte(getDouble(rs, "rsrq_db_lte"));
        data.setRsrpDbmLte(getDouble(rs, "rsrp_dbm_lte"));
        data.setRssiDbmLte(getDouble(rs, "rssi_dbm_lte"));
        data.setRssnrDbLte(getDouble(rs, "rssnr_db_lte"));
        
        // 5G Data
        data.setRat5g(rs.getString("rat_5g"));
        data.setPcellId5g(rs.getString("pcell_id_5g"));
        data.setBand5g(rs.getString("band_5g"));
        data.setEarfcn5g(getInteger(rs, "earfcn_5g"));
        data.setRsrqDb5g(getDouble(rs, "rsrq_db_5g"));
        data.setRsrpDbm5g(getDouble(rs, "rsrp_dbm_5g"));
        data.setRssnrDb5g(getDouble(rs, "rssnr_db_5g"));
        
        return data;
    }

    /**
     * Helper method to safely get Boolean from ResultSet.
     */
    private Boolean getBoolean(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value == null || rs.wasNull()) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Helper method to safely get Double from ResultSet.
     */
    private Double getDouble(ResultSet rs, String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }

    /**
     * Helper method to safely get Integer from ResultSet.
     */
    private Integer getInteger(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : value;
    }
}

