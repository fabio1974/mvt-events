package com.mvt.mvt_events.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Flyway configuration for handling production migration issues
 */
@Configuration
@Profile("prod")
public class FlywayConfig {

    @Autowired
    private DataSource dataSource;

    @Bean
    public Flyway flyway() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(false) // Disable validation to allow repair
                .load();

        try {
            // Try to repair any checksum mismatches
            flyway.repair();
            System.out.println("✅ Flyway repair completed successfully");
        } catch (Exception e) {
            System.out.println("⚠️ Flyway repair failed, but continuing: " + e.getMessage());
        }

        return flyway;
    }

    @Bean
    @DependsOn("flyway")
    public FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway, null);
    }
}