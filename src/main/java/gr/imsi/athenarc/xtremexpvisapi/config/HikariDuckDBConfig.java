package gr.imsi.athenarc.xtremexpvisapi.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HikariDuckDBConfig {

  @Bean
  public DataSource hikariDuckDBDataSource() {
    HikariConfig config = new HikariConfig();

    // DuckDB embedded JDBC URL
    config.setJdbcUrl("jdbc:duckdb:");
    config.setDriverClassName("org.duckdb.DuckDBDriver");

    // Optional: fine-tune pool settings
    config.setMaximumPoolSize(5);
    config.setPoolName("DuckDBHikariPool");

    return new HikariDataSource(config);
  }
}
