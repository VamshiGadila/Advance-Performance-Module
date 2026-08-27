package com.practice.springbootdemo.advance_performance_module.dto.auth;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public department item for registration dropdown")
public record PublicDepartmentResponse(
        Long id,
        String name
) {}
