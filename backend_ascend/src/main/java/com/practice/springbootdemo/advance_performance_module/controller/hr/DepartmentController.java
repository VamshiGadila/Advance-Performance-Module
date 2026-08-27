package com.practice.springbootdemo.advance_performance_module.controller.hr;

import com.practice.springbootdemo.advance_performance_module.dto.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.CreateDepartmentRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.DepartmentResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.EmployeeResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.SetDefaultManagerRequest;
import com.practice.springbootdemo.advance_performance_module.service.hr.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/hr/departments")
@PreAuthorize("hasRole('HR')")
@Tag(name = "Departments", description = "HR Department Management APIs")
@SecurityRequirement(name = "BearerAuth")
public class DepartmentController {
    private final DepartmentService service;

    public DepartmentController(DepartmentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create Department", description = "Create a new organization department (HR only)")
    public ApiResponse<DepartmentResponse> create(@Valid @RequestBody CreateDepartmentRequest request) {
        log.info("REST: POST /api/hr/departments - Creating department: '{}'", request.name());
        DepartmentResponse response = service.create(request);
        return ApiResponse.success("Department created successfully", response);
    }

    @GetMapping
    @Operation(summary = "List Departments", description = "Retrieve all departments with assigned default managers (HR only)")
    public ApiResponse<List<DepartmentResponse>> list() {
        log.debug("REST: GET /api/hr/departments - Listing all departments");
        return ApiResponse.success(service.list());
    }

    @PutMapping("/{departmentId}/default-manager")
    @Operation(summary = "Set Default Manager", description = "Configure default manager for a department (HR only)")
    public ApiResponse<DepartmentResponse> setDefaultManager(
            @PathVariable Long departmentId,
            @Valid @RequestBody SetDefaultManagerRequest request
    ) {
        log.info("REST: PUT /api/hr/departments/{}/default-manager - Manager ID: {}", departmentId, request.managerId());
        return ApiResponse.success("Default manager set successfully", service.setDefaultManager(departmentId, request.managerId()));
    }

    @GetMapping("/{departmentId}/employees")
    @Operation(summary = "Get Department Employees", description = "List all employees belonging to a specific department (HR only)")
    public ApiResponse<List<EmployeeResponse>> departmentEmployees(@PathVariable Long departmentId) {
        log.debug("REST: GET /api/hr/departments/{}/employees - Fetching department employees", departmentId);
        return ApiResponse.success(service.getDepartmentEmployees(departmentId));
    }
}
