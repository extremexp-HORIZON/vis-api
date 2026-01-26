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

    @Value("${spring.datasource.host}")
    private String postgresHost;
    @Value("${spring.datasource.port}")
    private String postgresPort;
    @Value("${spring.datasource.database}")
    private String postgresDatabase;

    @Value("${spring.datasource.username}")
    private String postgresUsername;
    @Value("${spring.datasource.password}")
    private String postgresPassword;
    @Value("${spring.datasource.driver-class-name}")
    private String postgresDriverClassName;

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

        // Initialize PostgreSQL extension on each new connection
        String initSql = buildPostgresInitSql();
        config.setConnectionInitSql(initSql);

        return new HikariDataSource(config);
    }

    private String buildPostgresInitSql() {
        return String.format(
            "INSTALL postgres;" +
            "LOAD postgres;" +
            "INSTALL spatial;" +
            "LOAD spatial;" +
            " ATTACH 'dbname=%s user=%s password=%s host=%s port=%s' AS postgres_db (TYPE postgres, READ_ONLY);",
            postgresDatabase, postgresUsername, postgresPassword, postgresHost, postgresPort
        );
    }
}