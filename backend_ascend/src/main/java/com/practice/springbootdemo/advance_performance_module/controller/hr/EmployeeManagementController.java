package com.practice.springbootdemo.advance_performance_module.controller.hr;

import com.practice.springbootdemo.advance_performance_module.dto.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.dto.common.PagedResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.CreateManagerRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.EmployeeResponse;
import com.practice.springbootdemo.advance_performance_module.dto.search.EmployeeSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.service.hr.EmployeeManagementService;
import com.practice.springbootdemo.advance_performance_module.service.search.SearchService;
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
@RequestMapping("/api/hr/employees")
@PreAuthorize("hasRole('HR')")
@Tag(name = "Employee Management", description = "HR Employee & Manager Management APIs")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeManagementController {
    private final EmployeeManagementService service;
    private final SearchService searchService;

    public EmployeeManagementController(EmployeeManagementService service, SearchService searchService) {
        this.service = service;
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "Get All Employees", description = "Retrieve all active employees (HR only)")
    public ApiResponse<List<EmployeeResponse>> employees() {
        log.debug("REST: GET /api/hr/employees - Fetching all active employees");
        return ApiResponse.success(service.getEmployees());
    }

    @GetMapping("/managers")
    @Operation(summary = "Get All Managers", description = "Retrieve all active managers (HR only)")
    public ApiResponse<List<EmployeeResponse>> managers() {
        log.debug("REST: GET /api/hr/employees/managers - Fetching all active managers");
        return ApiResponse.success(service.getManagers());
    }

    @PostMapping("/managers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create Manager", description = "Register and provision a new Manager account (HR only)")
    public ApiResponse<EmployeeResponse> createManager(@Valid @RequestBody CreateManagerRequest request) {
        log.info("REST: POST /api/hr/employees/managers - Creating manager for: {}", request.email());
        EmployeeResponse response = service.createManager(request);
        return ApiResponse.success("Manager account created successfully", response);
    }

    @PatchMapping("/{id}/promote")
    @Operation(
            summary = "Promote Employee to Manager",
            description = "Promote an existing employee to Manager role while preserving permanent EMP code"
    )
    public ApiResponse<EmployeeResponse> promoteEmployee(@PathVariable Long id) {
        log.info("REST: PATCH /api/hr/employees/{}/promote - HR promoting employee to MANAGER", id);
        EmployeeResponse response = service.promoteToManager(id);
        return ApiResponse.success("Employee successfully promoted to Manager", response);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search Employees (HR)",
            description = "Dynamic multi-filter search with sorting and 0-based pagination for HR"
    )
    public ApiResponse<PagedResponse<EmployeeResponse>> searchEmployees(EmployeeSearchCriteria criteria) {
        log.debug("REST: GET /api/hr/employees/search - Executing criteria search");
        return ApiResponse.success(searchService.searchEmployees(criteria));
    }
}