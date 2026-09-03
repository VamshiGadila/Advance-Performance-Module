package com.practice.springbootdemo.advance_performance_module.controller.hr;

import com.practice.springbootdemo.advance_performance_module.dto.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.dto.common.PagedResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.CreateManagerRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.DeactivateEmployeeRequest;
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

    @GetMapping("/all")
    @Operation(summary = "Get All Organization Staff", description = "Retrieve all active staff members across roles (HR only)")
    public ApiResponse<List<EmployeeResponse>> allStaff() {
        log.debug("REST: GET /api/hr/employees/all - Fetching all staff");
        return ApiResponse.success(service.getAllStaff());
    }

    @PatchMapping("/{id}/manager")
    @Operation(
            summary = "Assign / Change Employee Manager",
            description = "Assign an employee to a reporting manager or move them to a new manager (HR only)"
    )
    public ApiResponse<EmployeeResponse> changeManager(
            @PathVariable Long id,
            @Valid @RequestBody com.practice.springbootdemo.advance_performance_module.dto.hr.ChangeManagerRequest request
    ) {
        log.info("REST: PATCH /api/hr/employees/{}/manager - Reassigning to manager {}", id, request.managerId());
        EmployeeResponse response = service.changeManager(id, request.managerId());
        return ApiResponse.success("Manager assigned successfully", response);
    }

    @PatchMapping("/{id}/department")
    @Operation(
            summary = "Transfer Employee Department",
            description = "Transfer an employee to a new department (HR only)"
    )
    public ApiResponse<EmployeeResponse> transferDepartment(
            @PathVariable Long id,
            @Valid @RequestBody com.practice.springbootdemo.advance_performance_module.dto.hr.TransferDepartmentRequest request
    ) {
        log.info("REST: PATCH /api/hr/employees/{}/department - Transferring to department {}", id, request.departmentId());
        EmployeeResponse response = service.transferDepartment(id, request.departmentId());
        return ApiResponse.success("Department transferred successfully", response);
    }

    @GetMapping("/hierarchy")
    @Operation(
            summary = "Get Manager Hierarchy",
            description = "Retrieve organization manager hierarchy tree with direct reporting employees (HR only)"
    )
    public ApiResponse<List<com.practice.springbootdemo.advance_performance_module.dto.hr.ManagerHierarchyResponse>> getHierarchy() {
        log.debug("REST: GET /api/hr/employees/hierarchy - Fetching manager hierarchy");
        return ApiResponse.success(service.getManagerHierarchy());
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

    @PostMapping("/{id}/deactivate")
    @Operation(
            summary = "Temporarily Deactivate Employee",
            description = "Suspend employee access for a specified duration in hours or days (HR only)"
    )
    public ApiResponse<EmployeeResponse> deactivateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody DeactivateEmployeeRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        Long hrUserId = com.practice.springbootdemo.advance_performance_module.security.SecurityUtils.getUserIdFromAuth(authentication);
        log.info("REST: POST /api/hr/employees/{}/deactivate - HR User ID {} suspending account for {} {}",
                id, hrUserId, request.durationValue(), request.durationUnit());
        EmployeeResponse response = service.deactivateEmployee(id, request, hrUserId);
        return ApiResponse.success("Employee account temporarily deactivated successfully", response);
    }

    @PostMapping("/{id}/reactivate")
    @Operation(
            summary = "Reactivate Employee Account",
            description = "Restore active access for a previously suspended employee account (HR only)"
    )
    public ApiResponse<EmployeeResponse> reactivateEmployee(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication
    ) {
        Long hrUserId = com.practice.springbootdemo.advance_performance_module.security.SecurityUtils.getUserIdFromAuth(authentication);
        log.info("REST: POST /api/hr/employees/{}/reactivate - HR User ID {} reactivating account", id, hrUserId);
        EmployeeResponse response = service.reactivateEmployee(id, hrUserId);
        return ApiResponse.success("Employee account reactivated successfully", response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Permanently Remove Employee",
            description = "Permanently purge employee account and all dependent operational records from the DB (HR only)"
    )
    public ApiResponse<Void> removeEmployee(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication
    ) {
        Long hrUserId = com.practice.springbootdemo.advance_performance_module.security.SecurityUtils.getUserIdFromAuth(authentication);
        log.warn("REST: DELETE /api/hr/employees/{} - HR User ID {} executing permanent employee deletion", id, hrUserId);
        service.deleteEmployee(id, hrUserId);
        return ApiResponse.success("Employee permanently removed from the system", null);
    }
}