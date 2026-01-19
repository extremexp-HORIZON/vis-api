package gr.imsi.athenarc.xtremexpvisapi.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.GeoPoint;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Zone;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class ZoneRepository {

    private final JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;

    public ZoneRepository(
            @Qualifier("postgresqlJdbcTemplate") JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    private final RowMapper<Zone> zoneRowMapper = (rs, rowNum) -> {
        Zone zone = new Zone();
        zone.setId(rs.getString("id"));
        zone.setFileName(rs.getString("file_name"));
        zone.setName(rs.getString("name"));
        zone.setType(rs.getString("type"));
        zone.setDescription(rs.getString("description"));
        zone.setStatus(rs.getString("status"));
        zone.setRadius(rs.getFloat("radius"));

        // Handle timestamp
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            zone.setCreatedAt(createdAt.toLocalDateTime().toString());
        }

        // Deserialize JSONB fields
        try {
            String heightsJson = rs.getString("heights");
            if (heightsJson != null) {
                zone.setHeights(objectMapper.readValue(heightsJson, Double[].class));
            }

            String geohashesJson = rs.getString("geohashes");
            if (geohashesJson != null) {
                zone.setGeohashes(objectMapper.readValue(geohashesJson, String[].class));
            }

           String coordinatesJson = rs.getString("coordinates");
           if (coordinatesJson != null) {
            zone.setCoordinates(objectMapper.readValue(coordinatesJson, new TypeReference<List<GeoPoint>>() {}));
           }

           String centerJson = rs.getString("center");
           if (centerJson != null) {
            zone.setCenter(objectMapper.readValue(centerJson, GeoPoint.class));
           }
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON fields", e);
        }

        return zone;
    };

    private void insert(Zone zone) throws DataAccessException, JsonProcessingException {
        String sql = """
            INSERT INTO zones (id, file_name, name, type, description, status, radius, created_at, heights, geohashes, coordinates, center)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?::jsonb)
            """;
        
        jdbcTemplate.update(sql,
            zone.getId(),
            zone.getFileName(),
            zone.getName(),
            zone.getType(),
            zone.getDescription(),
            zone.getStatus(),
            zone.getRadius() != null ? zone.getRadius() : null,
            // Handle createdAt - use current time if not provided
            zone.getCreatedAt() != null ? 
                Timestamp.valueOf(java.time.LocalDateTime.parse(zone.getCreatedAt())) : 
                Timestamp.valueOf(java.time.LocalDateTime.now()),
            // Serialize JSONB fields
            zone.getHeights() != null ? objectMapper.writeValueAsString(zone.getHeights()) : null,
            zone.getGeohashes() != null ? objectMapper.writeValueAsString(zone.getGeohashes()) : null,
            zone.getCoordinates() != null ? objectMapper.writeValueAsString(zone.getCoordinates()) : null,
            zone.getCenter() != null ? objectMapper.writeValueAsString(zone.getCenter()) : null
        );
    }

    private void update(Zone zone) throws DataAccessException, JsonProcessingException {
        String sql = """
            UPDATE zones 
            SET name = ?, type = ?, description = ?, status = ?, 
                heights = ?::jsonb, geohashes = ?::jsonb, shape = ?::jsonb
            WHERE id = ? AND file_name = ?
            """;
        
        jdbcTemplate.update(sql,
            zone.getName(),
            zone.getType(),
            zone.getDescription(),
            zone.getStatus(),
            zone.getRadius() != null ? zone.getRadius() : null,
            zone.getHeights() != null ? objectMapper.writeValueAsString(zone.getHeights()) : null,
            zone.getGeohashes() != null ? objectMapper.writeValueAsString(zone.getGeohashes()) : null,
            zone.getCoordinates() != null ? objectMapper.writeValueAsString(zone.getCoordinates()) : null,
            zone.getCenter() != null ? objectMapper.writeValueAsString(zone.getCenter()) : null,
            zone.getId(),
            zone.getFileName()
        );
    }

    /**
     * Find a Zone by its id within a specific fileName
     * 
     * @param fileName the fileName to search in
     * @param zoneId the zone id to search for
     * @return Optional containing the Zone if found, empty otherwise
     */
    public Optional<Zone> findByFileNameAndId(String fileName, String zoneId) {
        String sql = "SELECT * FROM zones WHERE file_name = ? AND id = ?";
        return jdbcTemplate.query(sql, zoneRowMapper, fileName, zoneId)
                .stream()
                .findFirst();
    }

    /**
     * Find all Zones for a specific fileName
     * 
     * @param fileName the fileName to search for
     * @return List of all Zones for the specified fileName
     */
    public List<Zone> findByFileName(String fileName) {
        String sql = "SELECT * FROM zones WHERE file_name = ?";
        return jdbcTemplate.query(sql, zoneRowMapper, fileName);
    }

    /**
     * Find all Zones across all files
     * 
     * @return List of all Zones from all files
     */
    public List<Zone> findAll() {
        String sql = "SELECT * FROM zones";
        return jdbcTemplate.query(sql, zoneRowMapper);
    }

    /**
     * Save a Zone to the appropriate fileName file
     * 
     * @param zone the Zone to save
     * @return the saved Zone
     * @throws JsonProcessingException 
     * @throws DataAccessException 
     */
    public Zone save(Zone zone) throws DataAccessException, JsonProcessingException {
        if (zone.getFileName() == null || zone.getFileName().trim().isEmpty()) {
            throw new IllegalArgumentException("Zone fileName cannot be null or empty");
        }

        // Generate ID if not provided
        if (zone.getId() == null || zone.getId().trim().isEmpty()) {
            zone.setId(java.util.UUID.randomUUID().toString());
        }

        // Check if zone already exists
        Optional<Zone> existing = findByFileNameAndId(zone.getFileName(), zone.getId());
        
        if (existing.isPresent()) {
            // Update existing zone
            update(zone);
        } else {
            // Insert new zone
            insert(zone);
        }

        return zone;
    }

    /**
     * Delete a Zone by its id within a specific fileName
     * 
     * @param fileName the fileName to search in
     * @param zoneId the zone id to delete
     * @return true if deletion was successful, false if the Zone doesn't exist
     */
    public boolean deleteByFileNameAndId(String fileName, String zoneId) {
        String sql = "DELETE FROM zones WHERE file_name = ? AND id = ?";
        return jdbcTemplate.update(sql, fileName, zoneId) > 0;
    }

    /**
     * Delete all zones for a specific fileName
     * 
     * @param fileName the fileName to delete
     * @return true if deletion was successful, false if the file doesn't exist
     */
    public boolean deleteByFileName(String fileName) {
        String sql = "DELETE FROM zones WHERE file_name = ?";
        return jdbcTemplate.update(sql, fileName) > 0;
    }

    /**
     * Check if zones exist for a specific fileName
     * 
     * @param fileName the fileName to check
     * @return true if zones exist for the fileName, false otherwise
     */
    public boolean existsByFileName(String fileName) {
        String sql = "SELECT COUNT(*) from zones WHERE file_name = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class) > 0;
    }

    /**
     * Find zones by type across all files
     * 
     * @param type the type to search for
     * @return List of zones with the specified type
     */
    public List<Zone> findByType(String type) {
        String sql = "SELECT * FROM zones WHERE type = ?";
        return jdbcTemplate.query(sql, zoneRowMapper, type);
    }

    /**
     * Find zones by status across all files
     * 
     * @param status the status to search for
     * @return List of zones with the specified status
     */
    public List<Zone> findByStatus(String status) {
        String sql = "SELECT * FROM zones WHERE status = ?";
        return jdbcTemplate.query(sql, zoneRowMapper, status);
    }

    /**
     * Get all unique fileNames that have zones
     * 
     * @return List of unique fileNames
     */
    public List<String> getAllFileNames() {
        String sql = "SELECT DISTINCT file_name FROM zones";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("file_name"));
    }
}
