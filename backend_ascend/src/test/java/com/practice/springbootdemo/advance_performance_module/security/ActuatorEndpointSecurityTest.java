package com.practice.springbootdemo.advance_performance_module.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class ActuatorEndpointSecurityTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("GET /actuator root discovery endpoint is publicly accessible")
    void testActuatorRootIsPublic() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links").isMap());
    }

    @Test
    @DisplayName("GET /actuator/health is publicly accessible without authentication")
    void testActuatorHealthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header("X-Correlation-ID", "trace-health-check-001"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "trace-health-check-001"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /actuator/info is publicly accessible without authentication")
    void testActuatorInfoIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/info")
                        .header("X-Correlation-ID", "trace-info-check-002"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "trace-info-check-002"));
    }

    @Test
    @DisplayName("GET /actuator/beans is publicly accessible")
    void testActuatorBeansIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts").isMap());
    }

    @Test
    @DisplayName("GET /actuator/mappings is publicly accessible")
    void testActuatorMappingsIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/mappings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contexts").isMap());
    }

    @Test
    @DisplayName("GET /actuator/metrics requires authentication (fails for anonymous user)")
    void testActuatorMetricsRequiresAuth() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    @DisplayName("GET /actuator/metrics is forbidden for non-HR role (EMPLOYEE)")
    void testActuatorMetricsForbiddenForEmployee() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    @DisplayName("GET /actuator/metrics is allowed for HR role")
    void testActuatorMetricsAllowedForHR() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }
}
