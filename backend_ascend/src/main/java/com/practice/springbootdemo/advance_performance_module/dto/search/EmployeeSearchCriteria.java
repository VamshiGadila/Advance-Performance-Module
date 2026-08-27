package com.practice.springbootdemo.advance_performance_module.dto.search;

import com.practice.springbootdemo.advance_performance_module.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Employee dynamic search and pagination parameters")
public class EmployeeSearchCriteria {
    @Schema(description = "Global search across name, code, or email", example = "EMP001")
    private String search;
    @Schema(description = "Partial name match", example = "John")
    private String name;
    @Schema(description = "Employee code match", example = "EMP001")
    private String employeeCode;
    @Schema(description = "Partial email match", example = "john@")
    private String email;
    @Schema(description = "Role filter (HR, MANAGER, EMPLOYEE)", example = "EMPLOYEE")
    private Role role;
    @Schema(description = "Skill filter", example = "Java")
    private String skill;
    @Schema(description = "Work location filter", example = "Hyderabad")
    private String location;
    @Schema(description = "Domain filter", example = "Backend")
    private String domain;
    @Schema(description = "Department ID filter", example = "2")
    private Long departmentId;
    @Schema(description = "Assigned Manager ID filter", example = "5")
    private Long managerId;
    @Schema(description = "Active status filter", example = "true")
    private Boolean active;
    @Schema(description = "Minimum years of experience", example = "2")
    private Integer minExperience;
    @Schema(description = "Maximum years of experience", example = "10")
    private Integer maxExperience;
    @Builder.Default
    @Schema(description = "Zero-based page number", example = "0")
    private Integer page = 0;
    @Builder.Default
    @Schema(description = "Page size", example = "10")
    private Integer size = 10;
    @Builder.Default
    @Schema(description = "Sort property", example = "name")
    private String sortBy = "name";
    @Builder.Default
    @Schema(description = "Sort direction (asc / desc)", example = "asc")
    private String direction = "asc";
}
