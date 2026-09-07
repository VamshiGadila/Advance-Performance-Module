package com.practice.springbootdemo.advance_performance_module.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Actuator Health Indicator for ASCEND Performance Module.
 * Provides deep health diagnostics including database connectivity,
 * JVM memory metrics, and subsystem operational readiness.
 */
@Component("ascendApp")
public class AscendHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public AscendHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();
        boolean dbHealthy = checkDatabase(details);

        // Memory Metrics
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;

        details.put("jvm.maxMemoryMb", maxMemory / (1024 * 1024));
        details.put("jvm.totalMemoryMb", totalMemory / (1024 * 1024));
        details.put("jvm.usedMemoryMb", usedMemory / (1024 * 1024));
        details.put("jvm.freeMemoryMb", freeMemory / (1024 * 1024));
        details.put("subsystem", "ASCEND Performance & OKR Tracking Engine");
        details.put("statusDescription", dbHealthy ? "Operational - Ready to accept traffic" : "Degraded - Database unreachable");

        if (dbHealthy) {
            return Health.up().withDetails(details).build();
        } else {
            return Health.down().withDetails(details).build();
        }
    }

    private boolean checkDatabase(Map<String, Object> details) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1");
            details.put("database.status", "CONNECTED");
            details.put("database.product", connection.getMetaData().getDatabaseProductName());
            details.put("database.version", connection.getMetaData().getDatabaseProductVersion());
            return true;
        } catch (Exception e) {
            details.put("database.status", "DISCONNECTED");
            details.put("database.error", e.getMessage());
            return false;
        }
    }
}
