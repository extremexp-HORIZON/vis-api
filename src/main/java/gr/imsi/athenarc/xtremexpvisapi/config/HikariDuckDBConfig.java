package gr.imsi.athenarc.xtremexpvisapi.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class HikariDuckDBConfig {

    @Value("${duckdb.drone.database.path:}")
    private String droneDatabasePath;

    /**
     * Primary DataSource for general DuckDB operations (in-memory).
     * Used by DataServiceV2 and other services that need temporary queries.
     */
    @Bean
    @Primary
    public DataSource hikariDuckDBDataSource() {
        HikariConfig config = new HikariConfig();
        
        // Always in-memory for general use
        config.setJdbcUrl("jdbc:duckdb:");
        config.setDriverClassName("org.duckdb.DuckDBDriver");
        config.setMaximumPoolSize(5);
        config.setPoolName("DuckDBHikariPool-InMemory");

        return new HikariDataSource(config);
    }

    /**
     * Dedicated DataSource for drone telemetry data (file-based).
     * Only created if drone database path is configured.
     * Used by DroneDataRepository.
     */
    @Bean(name = "droneDuckDBDataSource")
    public DataSource droneDuckDBDataSource() {
        HikariConfig config = new HikariConfig();

        // Connect directly to persistent file if configured
        if (droneDatabasePath != null && !droneDatabasePath.isBlank()) {
            config.setJdbcUrl("jdbc:duckdb:" + droneDatabasePath);
            config.setPoolName("DuckDBHikariPool-Drone");
        } else {
            // Fallback to in-memory if not configured
            config.setJdbcUrl("jdbc:duckdb:");
            config.setPoolName("DuckDBHikariPool-Drone-InMemory");
        }
        
        config.setDriverClassName("org.duckdb.DuckDBDriver");
        config.setMaximumPoolSize(5);

        return new HikariDataSource(config);
    }
}