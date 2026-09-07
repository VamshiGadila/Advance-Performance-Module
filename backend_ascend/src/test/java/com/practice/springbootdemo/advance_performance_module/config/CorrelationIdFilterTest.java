package com.practice.springbootdemo.advance_performance_module.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new CorrelationIdFilter();
    }

    @Test
    @DisplayName("Should use incoming X-Correlation-ID header if provided")
    void testUsesExistingCorrelationId() throws ServletException, IOException {
        String existingId = "custom-trace-uuid-12345";
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(existingId);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100");

        doAnswer(invocation -> {
            // Verify MDC has the correlation ID during request execution
            assertEquals(existingId, MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY));
            assertEquals("192.168.1.100", MDC.get(CorrelationIdFilter.MDC_CLIENT_IP_KEY));
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        // Verify response header set
        verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, existingId);

        // Verify MDC cleaned up after request finishes
        assertNull(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY));
        assertNull(MDC.get(CorrelationIdFilter.MDC_CLIENT_IP_KEY));
    }

    @Test
    @DisplayName("Should generate random UUID when X-Correlation-ID is missing")
    void testGeneratesRandomCorrelationId() throws ServletException, IOException {
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        doAnswer(invocation -> {
            String mdcId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
            assertNotNull(mdcId);
            assertFalse(mdcId.isBlank());
            assertEquals("127.0.0.1", MDC.get(CorrelationIdFilter.MDC_CLIENT_IP_KEY));
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), anyString());

        // Verify MDC cleared
        assertNull(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID_KEY));
    }
}
