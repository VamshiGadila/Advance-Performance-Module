package com.practice.springbootdemo.advance_performance_module.dto.auth;

import com.practice.springbootdemo.advance_performance_module.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Authentication token & user details response")
public record LoginResponse(
        String token,
        Long id,
        String employeeCode,
        String name,
        String email,
        Role role,
        Long departmentId,
        String departmentName
) {
        public LoginResponse(String token, Long id, String employeeCode, String name, String email, String roleName) {
                this(token, id, employeeCode, name, email, Role.valueOf(roleName), null, null);
        }
        public LoginResponse(String token, Long id, String employeeCode, String name, String email, Role role) {
                this(token, id, employeeCode, name, email, role, null, null);
        }
}