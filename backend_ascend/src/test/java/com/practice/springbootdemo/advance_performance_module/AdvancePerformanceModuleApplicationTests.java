package com.practice.springbootdemo.advance_performance_module;

import com.practice.springbootdemo.advance_performance_module.config.AscendHealthIndicator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AdvancePerformanceModuleApplicationTests {

    @Autowired(required = false)
    private AscendHealthIndicator ascendHealthIndicator;

    @Test
    @DisplayName("Context loads successfully with test profile")
    void contextLoads() {
        assertNotNull(ascendHealthIndicator, "AscendHealthIndicator should be loaded into application context");
    }

    @Test
    @DisplayName("Actuator Custom Health Indicator reports UP with DB connected")
    void testAscendHealthIndicatorReportsUp() {
        Health health = ascendHealthIndicator.health();
        assertNotNull(health);
        assertEquals(Status.UP, health.getStatus(), "Application health should be UP");
        assertTrue(health.getDetails().containsKey("database.status"), "Health should contain database status");
        assertEquals("CONNECTED", health.getDetails().get("database.status"));
        assertTrue(health.getDetails().containsKey("jvm.maxMemoryMb"), "Health should contain JVM memory details");
    }
}
