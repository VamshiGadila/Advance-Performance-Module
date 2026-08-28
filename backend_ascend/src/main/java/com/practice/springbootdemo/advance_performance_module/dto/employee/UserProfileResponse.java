package com.practice.springbootdemo.advance_performance_module.dto.employee;

import com.practice.springbootdemo.advance_performance_module.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detailed user profile response with enterprise credentials and editable attributes")
public record UserProfileResponse(
        Long id,
        String employeeCode,
        String name,
        String email,
        Role role,
        Long departmentId,
        String departmentName,
        String designation,
        Long managerId,
        String managerName,
        String managerCode,
        String skill,
        String domain,
        String location,
        Integer experienceYears
) {}
