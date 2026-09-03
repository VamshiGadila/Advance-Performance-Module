package com.practice.springbootdemo.advance_performance_module.dto.hr;

import com.practice.springbootdemo.advance_performance_module.entity.Role;
import com.practice.springbootdemo.advance_performance_module.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Employee profile, designation, manager and department details response")
public record EmployeeResponse(
        Long id,
        String employeeCode,
        String name,
        String email,
        Long departmentId,
        String departmentName,
        Role role,
        String designation,
        Long managerId,
        String managerName,
        String managerCode,
        String skill,
        String location,
        String domain,
        Integer experienceYears,
        boolean active,
        UserStatus status,
        LocalDateTime deactivatedUntil,
        String deactivationReason
) {
    public EmployeeResponse(
            Long id,
            String employeeCode,
            String name,
            String email,
            Long departmentId,
            String departmentName,
            Role role,
            String designation,
            Long managerId,
            String managerName,
            String managerCode,
            String skill,
            String location,
            String domain,
            Integer experienceYears,
            boolean active
    ) {
        this(id, employeeCode, name, email, departmentId, departmentName, role, designation, managerId, managerName, managerCode, skill, location, domain, experienceYears, active, active ? UserStatus.ACTIVE : UserStatus.DISABLED, null, null);
    }

    public EmployeeResponse(
            Long id,
            String employeeCode,
            String name,
            String email,
            Long departmentId,
            String departmentName,
            Role role,
            String skill,
            String location,
            String domain,
            Integer experienceYears,
            boolean active
    ) {
        this(id, employeeCode, name, email, departmentId, departmentName, role, null, null, null, null, skill, location, domain, experienceYears, active, active ? UserStatus.ACTIVE : UserStatus.DISABLED, null, null);
    }

    public EmployeeResponse(Long id, String employeeCode, String name, String email, Long departmentId, Role role) {
        this(id, employeeCode, name, email, departmentId, null, role, null, null, null, null, null, null, null, null, true, UserStatus.ACTIVE, null, null);
    }
}