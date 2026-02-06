package gr.imsi.athenarc.xtremexpvisapi.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.geo.Coverage;
import com.github.davidmoten.geo.GeoHash;
import com.github.davidmoten.geo.LatLong;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Zone;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
public class ZoneRepository {

    private final JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;

    @Value("${eusome.geohash.precision}")
    private int geohashPrecision;

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

            String featureJson = rs.getString("feature");
            if (featureJson != null) {
                zone.setFeature(objectMapper.readValue(featureJson, Feature.class));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON fields", e);
        }

        return zone;
    };

    private void insert(Zone zone) throws DataAccessException, JsonProcessingException, ParseException {
        String sql = """
            INSERT INTO zones (id, file_name, name, type, description, status, created_at, heights, geohashes, feature)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb)
            """;
        
        jdbcTemplate.update(sql,
            zone.getId(),
            zone.getFileName(),
            zone.getName(),
            zone.getType(),
            zone.getDescription(),
            zone.getStatus(),
            // Handle createdAt - use current time if not provided
            parseCreatedAt(zone.getCreatedAt(), true),
            // Serialize JSONB fields
            zone.getHeights() != null ? objectMapper.writeValueAsString(zone.getHeights()) : null,
            zone.getGeohashes() != null
                ? objectMapper.writeValueAsString(zone.getGeohashes())
                : (zone.getFeature() != null
                    ? objectMapper.writeValueAsString(calculateIntersectingGeohashes(zone.getFeature()).toArray(String[]::new))
                    : null),
            zone.getFeature() != null ? objectMapper.writeValueAsString(zone.getFeature()) : null
        );
    }

    /**
     * Partial update: only updates columns that are non-null on the passed Zone.
     *
     * Special-case: if a new feature is provided and geohashes are not,
     * geohashes are re-calculated from the feature so they don't become stale.
     */
    private void update(Zone zone) throws DataAccessException, JsonProcessingException, ParseException {
        List<String> sets = new ArrayList<>();
        List<Object> args = new ArrayList<>();

        if (zone.getName() != null) {
            sets.add("name = ?");
            args.add(zone.getName());
        }
        if (zone.getType() != null) {
            sets.add("type = ?");
            args.add(zone.getType());
        }
        if (zone.getDescription() != null) {
            sets.add("description = ?");
            args.add(zone.getDescription());
        }
        if (zone.getStatus() != null) {
            sets.add("status = ?");
            args.add(zone.getStatus());
        }
        if (zone.getHeights() != null) {
            sets.add("heights = ?::jsonb");
            args.add(objectMapper.writeValueAsString(zone.getHeights()));
        }

        // If feature changes, geohashes should either be explicitly provided or re-computed.
        String computedGeohashesJson = null;
        if (zone.getFeature() != null && zone.getGeohashes() == null) {
            computedGeohashesJson = objectMapper.writeValueAsString(
                    calculateIntersectingGeohashes(zone.getFeature()).toArray(String[]::new));
        }

        if (zone.getGeohashes() != null) {
            sets.add("geohashes = ?::jsonb");
            args.add(objectMapper.writeValueAsString(zone.getGeohashes()));
        } else if (computedGeohashesJson != null) {
            sets.add("geohashes = ?::jsonb");
            args.add(computedGeohashesJson);
        }

        if (zone.getFeature() != null) {
            sets.add("feature = ?::jsonb");
            args.add(objectMapper.writeValueAsString(zone.getFeature()));
        }

        // Nothing to update (but caller treated it as an update).
        if (sets.isEmpty()) {
            return;
        }

        String sql = "UPDATE zones SET " + String.join(", ", sets) + " WHERE id = ? AND file_name = ?";
        args.add(zone.getId());
        args.add(zone.getFileName());

        jdbcTemplate.update(sql, args.toArray());
    }

    private void insertAll(List<Zone> zones) throws DataAccessException, JsonProcessingException {
        String sql = """
            INSERT INTO zones (id, file_name, name, type, description, status, created_at, heights, geohashes, feature)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb)
            """;
        jdbcTemplate.batchUpdate(sql, zones, zones.size(), (ps, zone) -> {
            try {
                ps.setString(1, zone.getId());
                ps.setString(2, zone.getFileName());
                ps.setString(3, zone.getName());
                ps.setString(4, zone.getType());
                ps.setString(5, zone.getDescription());
                ps.setString(6, zone.getStatus());
                ps.setTimestamp(7, parseCreatedAt(zone.getCreatedAt(), false));
                ps.setString(8, zone.getHeights() != null ? objectMapper.writeValueAsString(zone.getHeights()) : null);
                ps.setString(9, zone.getGeohashes() != null ? objectMapper.writeValueAsString(zone.getGeohashes()) : null);
                ps.setString(10, zone.getFeature() != null ? objectMapper.writeValueAsString(zone.getFeature()) : null);
                System.out.println("Prepared statement: " + ps.toString());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize JSON fields", e);
            }
        });
    }

    private Timestamp parseCreatedAt(String createdAt, boolean defaultToNow) {
        if (createdAt == null || createdAt.trim().isEmpty()) {
            return defaultToNow ? Timestamp.valueOf(LocalDateTime.now()) : null;
        }

        String trimmed = createdAt.trim();
        // Timestamp.valueOf accepts "yyyy-MM-dd HH:mm:ss[.fraction]"
        if (trimmed.indexOf(' ') >= 0) {
            return Timestamp.valueOf(trimmed);
        }

        // Fallback to ISO-8601 "yyyy-MM-ddTHH:mm:ss[.fraction]"
        return Timestamp.valueOf(LocalDateTime.parse(trimmed));
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
    public Zone save(Zone zone) throws DataAccessException, JsonProcessingException, ParseException {
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

    static Polygon circleToPolygonWgs84(GeometryFactory gf, double centerLon, double centerLat, double radiusMeters, int steps) {
        final double R = 6371008.8; // mean Earth radius (m)
        double lat1 = Math.toRadians(centerLat);
        double lon1 = Math.toRadians(centerLon);
        double angDist = radiusMeters / R;

        Coordinate[] coords = new Coordinate[steps + 1];
        for (int i = 0; i < steps; i++) {
        double bearing = 2.0 * Math.PI * i / steps;

        double sinLat1 = Math.sin(lat1), cosLat1 = Math.cos(lat1);
        double sinAng = Math.sin(angDist), cosAng = Math.cos(angDist);

        double lat2 = Math.asin(sinLat1 * cosAng + cosLat1 * sinAng * Math.cos(bearing));
        double lon2 = lon1 + Math.atan2(
        Math.sin(bearing) * sinAng * cosLat1,
        cosAng - sinLat1 * Math.sin(lat2)
        );

        double latDeg = Math.toDegrees(lat2);
        double lonDeg = Math.toDegrees(lon2);
        coords[i] = new Coordinate(lonDeg, latDeg);
        }
        coords[steps] = coords[0]; // close ring

        LinearRing ring = gf.createLinearRing(coords);
        return gf.createPolygon(ring, null);
    }

    /**
     * Calculate the geohashes that intersect with a feature
     * 
     * @param feature the feature to calculate the intersecting geohashes for
     * @return List of geohashes that intersect with the feature
     * @throws JsonProcessingException 
     * @throws ParseException 
     */
    private List<String> calculateIntersectingGeohashes(Feature feature) throws JsonProcessingException, ParseException {
        Set<String> intersectingGeohashes = new HashSet<>();
        GeometryFactory geometryFactory = new GeometryFactory();

        if (feature == null || feature.getGeometry() == null) {
            return new ArrayList<>(intersectingGeohashes);
        }

        if (feature.getGeometry() != null) {
            if (feature.getGeometry() instanceof org.geojson.Polygon) {
                String geometryGeoJson = objectMapper.writeValueAsString(feature.getGeometry());
                Geometry zoneGeometry = new GeoJsonReader().read(geometryGeoJson);
                intersectingGeohashes.addAll(calculateIntersectingGeohashesForGeometry(zoneGeometry, geometryFactory));
            } else if (feature.getGeometry() instanceof org.geojson.Point
                    && feature.getProperties() != null
                    && feature.getProperties().get("radius") != null) {
                // It's a circle
                Object r = feature.getProperties().get("radius");
                double radiusMeters = (r instanceof Number) ? ((Number) r).doubleValue() : Double.parseDouble(r.toString());

                org.geojson.Point point = (org.geojson.Point) feature.getGeometry();
                double centerLon = point.getCoordinates().getLongitude();
                double centerLat = point.getCoordinates().getLatitude();

                Geometry zoneGeometry = circleToPolygonWgs84(geometryFactory, centerLon, centerLat, radiusMeters, 64);
                intersectingGeohashes.addAll(calculateIntersectingGeohashesForGeometry(zoneGeometry, geometryFactory));
            }
        }
        return new ArrayList<>(intersectingGeohashes);
    }

    private Set<String> calculateIntersectingGeohashesForGeometry(Geometry zoneGeometry, GeometryFactory geometryFactory) {
        Set<String> intersectingGeohashes = new HashSet<>();

        Envelope e = zoneGeometry.getEnvelopeInternal();
        Coverage coverage = GeoHash.coverBoundingBox(e.getMaxY(), e.getMinX(), e.getMinY(), e.getMaxX(), geohashPrecision);
        Set<String> coveredGeohashes = coverage.getHashes();

        PreparedGeometry preparedZone = PreparedGeometryFactory.prepare(zoneGeometry);

        double halfWidthDegrees = GeoHash.widthDegrees(geohashPrecision) / 2;
        double halfHeightDegrees = GeoHash.heightDegrees(geohashPrecision) / 2;

        for (String coveredGeohash : coveredGeohashes) {
            LatLong center = GeoHash.decodeHash(coveredGeohash);
            double minLat = center.getLat() - halfHeightDegrees;
            double maxLat = center.getLat() + halfHeightDegrees;
            double minLon = center.getLon() - halfWidthDegrees;
            double maxLon = center.getLon() + halfWidthDegrees;

            Coordinate[] rectangle = new Coordinate[] {
                    new Coordinate(minLon, minLat),
                    new Coordinate(maxLon, minLat),
                    new Coordinate(maxLon, maxLat),
                    new Coordinate(minLon, maxLat),
                    new Coordinate(minLon, minLat)
            };
            Polygon cellPolygon = geometryFactory.createPolygon(rectangle);
            if (preparedZone.intersects(cellPolygon)) {
                intersectingGeohashes.add(coveredGeohash);
            }
        }

        return intersectingGeohashes;
    }

    /**
     * Create a Zone from a Feature
     * 
     * @param feature the Feature to create the Zone from
     * @param fileName the fileName of the file that the Feature is from
     * @param importFileName the fileName of the file that the Feature is imported from
     * @param zoneIndex the one-based index of the zone in the file
     * @return the created Zone
     * @throws ParseException 
     */
    private Zone createZoneFromFeature(Feature feature, String fileName, String importFileName, Integer zoneIndex) throws JsonProcessingException, ParseException {
        Zone zone = new Zone();
        Map<String, Object> properties = feature.getProperties() != null ? feature.getProperties() : Collections.emptyMap();

        String id = properties.get("id") != null ? properties.get("id").toString() : null;
        if (id != null && !id.isEmpty()) {
            zone.setId(id);
        } else {
            zone.setId(java.util.UUID.randomUUID().toString());
        }
        zone.setFileName(fileName);
        String zoneName = properties.get("name") != null ? properties.get("name").toString() : null;
        if (zoneName != null && !zoneName.isEmpty()) {
            zone.setName(zoneName);
        } else {
            zone.setName("zone_" + (zoneIndex != null ? zoneIndex + 1 : "") + "_" + importFileName);
        }
        String zoneDescription = properties.get("description") != null ? properties.get("description").toString() : null;
        if (zoneDescription != null && !zoneDescription.isEmpty()) {
            zone.setDescription(zoneDescription);
        } else {
            zone.setDescription("Imported Zone " + (zoneIndex != null ? zoneIndex + 1 + " " : "") + "from file: " + importFileName);
        }
        String zoneType = properties.get("type") != null ? properties.get("type").toString() : null;
        if (zoneType != null && !zoneType.isEmpty()) {
            zone.setType(zoneType);
        } else {
            zone.setType("general");
        }
        String status = properties.get("status") != null ? properties.get("status").toString() : null;
        if (status != null && !status.isEmpty()) {
            zone.setStatus(status);
        } else {
            zone.setStatus("active");
        }
        
        zone.setCreatedAt(Timestamp.valueOf(java.time.LocalDateTime.now()).toString());
        zone.setGeohashes(calculateIntersectingGeohashes(feature).toArray(String[]::new));
        zone.setFeature(feature);
        return zone;
    }

    public List<Zone> importZonesFromFile(MultipartFile file, String fileName) throws IOException, ParseException {
        List<Zone> zones = new ArrayList<>();

        JsonNode jsonNode = objectMapper.readTree(file.getInputStream());
        if (jsonNode.get("type").asText().equals("FeatureCollection")) {
            FeatureCollection featureCollection = objectMapper.treeToValue(jsonNode, FeatureCollection.class);
            for (Feature feature : featureCollection.getFeatures()) {
                Zone zone = createZoneFromFeature(feature, fileName, file.getOriginalFilename(), zones.size());
                zones.add(zone);
            }
        } else if (jsonNode.get("type").asText().equals("Feature")) {
            Feature feature = objectMapper.treeToValue(jsonNode, Feature.class);
            Zone zone = createZoneFromFeature(feature, fileName, file.getOriginalFilename(), null);
            zones.add(zone);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + file.getContentType());
        }

        // Insert all zones into the database
        insertAll(zones);

        return zones;
    }
}
