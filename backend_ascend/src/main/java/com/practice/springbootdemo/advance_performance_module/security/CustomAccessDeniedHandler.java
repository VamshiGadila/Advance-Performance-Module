package com.practice.springbootdemo.advance_performance_module.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.practice.springbootdemo.advance_performance_module.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper;
    public CustomAccessDeniedHandler() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }
    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper().registerModule(new JavaTimeModule());
    }
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        log.warn("403 Forbidden: User attempted unauthorized access to URI: {} - Reason: {}",
                request.getRequestURI(), accessDeniedException.getMessage());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = ErrorResponse.of(
                "Access Denied: You do not have sufficient role permissions to perform this action",
                "ACCESS_DENIED"
        );
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}