package gr.imsi.athenarc.xtremexpvisapi.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DuckDBConfig {

  @Bean
  public Connection duckdbConnection() throws SQLException {
    // In-memory DuckDB instance
    return DriverManager.getConnection("jdbc:duckdb:");
  }
}
