package gr.imsi.athenarc.xtremexpvisapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
public class DuckDBConfig {
    
    @Bean
    public Connection duckdbConnection() throws SQLException {
        // In-memory DuckDB instance
        return DriverManager.getConnection("jdbc:duckdb:");
    }
}