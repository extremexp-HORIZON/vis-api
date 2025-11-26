package gr.imsi.athenarc.xtremexpvisapi.repository.drone;

import gr.imsi.athenarc.xtremexpvisapi.domain.drone.DroneData;
import gr.imsi.athenarc.xtremexpvisapi.domain.drone.DroneTelemetryRequest;
import gr.imsi.athenarc.xtremexpvisapi.domain.sync.SyncResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
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
    @Qualifier("droneDuckDBDataSource")
    private DataSource dataSource;

    @Value("${duckdb.drone.database.path:}")
    private String droneDatabasePath;

    @Value("${duckdb.drone.table.name:drone_telemetry}")
    private String tableName;

    @Value("${duckdb.drone.jsonl.path:}")
    private String jsonlPath; // Only used for sync operations

    /**
     * Initialize the DuckDB connection and create table if needed.
     * If jsonlPath is configured, uses direct JSONL reading.
     * Otherwise, uses a persistent DuckDB table.
     */
    @PostConstruct
    public void init() {
        try (Connection connection = dataSource.getConnection()) {
            if (droneDatabasePath != null && !droneDatabasePath.isBlank()) {
                createTableInPersistentDB();
                log.info("DroneDataRepository initialized with persistent database: " + droneDatabasePath);
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
     * Create the drone_telemetry table in persistent DuckDB database.
     * This is called during initialization if persistent DB is configured.
     */
    public synchronized void createTableInPersistentDB() throws SQLException {
        if (droneDatabasePath == null || droneDatabasePath.isBlank()) {
            throw new IllegalStateException("DuckDB database path not configured");
        }

        try (Connection connection = dataSource.getConnection();
            Statement stmt = connection.createStatement()) {

            // Create table with same schema as in-memory version
            String createTableSql = String.format("""
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
                    earfcn_5g INTEGER,
                    rsrq_db_5g DOUBLE,
                    rsrp_dbm_5g DOUBLE,
                    rssnr_db_5g DOUBLE,
                    PRIMARY KEY (timestamp, drone_id)
                )
                """, tableName);

            Boolean created = stmt.execute(createTableSql);

            if (created) {               
                // Create indexes
                String index1 = String.format("CREATE INDEX IF NOT EXISTS idx_drone_telemetry_drone_id ON %s(drone_id)", tableName);
                String index2 = String.format("CREATE INDEX IF NOT EXISTS idx_drone_telemetry_timestamp ON %s(timestamp)", tableName);
                String index3 = String.format("CREATE INDEX IF NOT EXISTS idx_drone_telemetry_location ON %s(lat, lon)", tableName);
                            
                stmt.execute(index1);
                stmt.execute(index2);
                stmt.execute(index3);
                            
                log.info("Created drone_telemetry table in persistent DuckDB: " + droneDatabasePath);
            } else {
                log.info("Table already exists in persistent DuckDB: " + droneDatabasePath);
            }
        }
    }
    
    /**
     * Perform incremental sync from JSONL to DuckDB.
     * Only inserts records newer than the maximum timestamp in the table.
     *
     * @return SyncResult containing statistics about the sync operation
     */
    public SyncResult syncJsonlToDuckDB() throws SQLException {
        if (jsonlPath == null || jsonlPath.isBlank()) {
            throw new IllegalStateException("JSONL path not configured");
        }
        if (droneDatabasePath == null || droneDatabasePath.isBlank()) {
            throw new IllegalStateException("DuckDB database path not configured");
        }
    
        SyncResult result = new SyncResult();
        result.setStartTime(System.currentTimeMillis());
    
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
    
            // Get max timestamp from table to find new records
            Optional<Timestamp> maxTimestamp = getMaxTimestampFromPersistentDB(connection);
    
            // Build INSERT query with WHERE clause to filter new records
            String insertSql = buildIncrementalInsertQuery(maxTimestamp);
    
            try (PreparedStatement pstmt = connection.prepareStatement(insertSql)) {
                pstmt.setString(1, jsonlPath);

                if (maxTimestamp.isPresent()) {
                    pstmt.setTimestamp(2, maxTimestamp.get());
                }
    
                int rowsInserted = pstmt.executeUpdate();
                result.setRowsInserted(rowsInserted);
                result.setSuccess(true);
    
                log.info("Synced " + rowsInserted + " new records from JSONL to DuckDB");
            }
    
        } catch (SQLException e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.warning("Error syncing JSONL to DuckDB: " + e.getMessage());
            throw e;
        } finally {
            result.setEndTime(System.currentTimeMillis());
            result.setDurationMs(result.getEndTime() - result.getStartTime());
        }
    
        return result;
    }
    
    /**
     * Get max timestamp from persistent DuckDB table.
     */
    private Optional<Timestamp> getMaxTimestampFromPersistentDB(Connection connection) throws SQLException {
        String sql = String.format("SELECT MAX(timestamp) FROM %s", tableName);
    
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
    
            if (rs.next() && rs.getTimestamp(1) != null) {
                return Optional.of(rs.getTimestamp(1));
            }
        }
    
        return Optional.empty();
    }
    
    /**
     * Build INSERT query for incremental sync.
     * If maxTimestamp is present, only inserts records newer than that.
     */
    private String buildIncrementalInsertQuery(Optional<Timestamp> maxTimestamp) {
        StringBuilder sql = new StringBuilder();

        sql.append(String.format("""
            INSERT OR IGNORE INTO %s
            WITH raw_json_data AS (
                SELECT
                    to_timestamp(timestamp) AS timestamp,
                    tags.drone_id AS drone_id,
                    fields.gpsFix AS gps_fix,
                    fields.lat AS lat,
                    fields.lon AS lon,
                    fields.alt AS alt,
                    fields.speedKmh AS speed_kmh,
                    fields.satellites AS satellites,
                    fields.ratLte AS rat_lte,
                    fields.opModeLte AS op_mode_lte,
                    fields.mccmncLte AS mccmnc_lte,
                    fields.tacLte AS tac_lte,
                    fields.scellIdLte AS scell_id_lte,
                    fields.pcellIdLte AS pcell_id_lte,
                    fields.bandLte AS band_lte,
                    fields.earfcnLte AS earfcn_lte,
                    fields.dlbwMhzLte AS dlbw_mhz_lte,
                    fields.ulbwMhzLte AS ulbw_mhz_lte,
                    fields.rsrqDbLte AS rsrq_db_lte,
                    fields.rsrpDbmLte AS rsrp_dbm_lte,
                    fields.rssiDbmLte AS rssi_dbm_lte,
                    fields.rssnrDbLte AS rssnr_db_lte,
                    fields.rat5g AS rat_5g,
                    fields.pcellId5g AS pcell_id_5g,
                    fields.band5g AS band_5g,
                    fields.earfcn5g AS earfcn_5g,
                    fields.rsrqDb5g AS rsrq_db_5g,
                    fields.rsrpDbm5g AS rsrp_dbm_5g,
                    fields.rssnrDb5g AS rssnr_db_5g
                FROM read_json_auto(?)
                WHERE 1=1
        """, tableName));

        // Add WHERE clause to filter only new records on the raw data (efficient)
        if (maxTimestamp.isPresent()) {
            sql.append(" AND to_timestamp(timestamp) > ?");
        }
        
        // Close the CTE
        sql.append(")\n");
        
        // Select from CTE and use QUALIFY for de-duplication
        sql.append("""
                SELECT 
                    * FROM raw_json_data
                QUALIFY
                    ROW_NUMBER() OVER (
                        PARTITION BY drone_id, timestamp
                        ORDER BY timestamp DESC -- If two records have the same time, this ensures one is chosen deterministically
                    ) = 1
                """);

        // Important: REMOVE the old 'GROUP BY' line here!

        return sql.toString();
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
                earfcn_5g INTEGER,
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
        sql = String.format(
                "SELECT * FROM %s WHERE drone_id = ? ORDER BY timestamp DESC LIMIT 1", tableName);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setString(1, droneId);
            
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
        
        sql = String.format("SELECT DISTINCT drone_id FROM %s ORDER BY drone_id", tableName);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
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
        
        sql.append("SELECT * FROM ").append(tableName);
        
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
            conditions.add("timestamp >= ?");
        }
        if (request.getEndTime() != null) {
            conditions.add("timestamp <= ?");
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
        if (sortOrder.equals("timestamp_asc")) {
            sql.append(" ORDER BY timestamp ASC");
        } else {
            sql.append(" ORDER BY timestamp DESC");
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
        
        sql.append("SELECT COUNT(*) FROM ").append(tableName);
        
        List<String> conditions = new ArrayList<>();
        
        // Same conditions as buildQuery
        if (request.getDroneId() != null && !request.getDroneId().isBlank()) {
            conditions.add("drone_id = ?");
        } else if (request.getDroneIds() != null && !request.getDroneIds().isEmpty()) {
            String placeholders = "?,".repeat(request.getDroneIds().size());
            conditions.add("drone_id IN (" + placeholders.substring(0, placeholders.length() - 1) + ")");
        }
        
        if (request.getStartTime() != null) {
            conditions.add("timestamp >= ?");
        }
        if (request.getEndTime() != null) {
            conditions.add("timestamp <= ?");
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

