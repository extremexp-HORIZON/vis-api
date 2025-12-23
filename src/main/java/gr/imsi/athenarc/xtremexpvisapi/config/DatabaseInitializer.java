package gr.imsi.athenarc.xtremexpvisapi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Initializes the database schema on application startup.
 * Reads and executes the SQL script from src/main/resources/db/schema.sql
 */
@Component
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;

    public DatabaseInitializer(
            @Qualifier("postgresqlJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Value("${db.init.enabled:true}") boolean enabled) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
    }

    @PostConstruct
    public void initializeDatabase() {
        if (!enabled) {
            logger.info("Database initialization is disabled");
            return;
        }

        try {
            logger.info("Starting database schema initialization...");
            
            // Read the SQL script from classpath
            ClassPathResource resource = new ClassPathResource("db/schema.sql");
            String sqlScript = StreamUtils.copyToString(
                    resource.getInputStream(), 
                    StandardCharsets.UTF_8
            );

            // Execute the entire script using a single connection
            jdbcTemplate.execute((Connection connection) -> {
                try (Statement statement = connection.createStatement()) {
                    // Execute the entire script
                    statement.execute(sqlScript);
                    logger.info("Database schema initialized successfully");
                    return null;
                }
            });

        } catch (Exception e) {
            // Log but don't fail startup - tables might already exist
            logger.warn("Database schema initialization completed with warnings: {}", e.getMessage());
            logger.debug("Full error:", e);
        }
    }
}