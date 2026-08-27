package com.practice.springbootdemo.advance_performance_module.controller.hr;

import com.practice.springbootdemo.advance_performance_module.dto.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.dto.common.PagedResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.AssignManagerRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.AssignmentResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.UpdateAssignmentRequest;
import com.practice.springbootdemo.advance_performance_module.dto.search.AssignmentSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.service.hr.ManagerAssignmentService;
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
@RequestMapping("/api/hr/manager-assignments")
@PreAuthorize("hasRole('HR')")
@Tag(name = "Manager Assignments", description = "HR Manager-to-Employee Assignment Management APIs")
@SecurityRequirement(name = "BearerAuth")
public class ManagerAssignmentController {
    private final ManagerAssignmentService service;
    private final SearchService searchService;

    public ManagerAssignmentController(ManagerAssignmentService service, SearchService searchService) {
        this.service = service;
        this.searchService = searchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Assign Manager to Employee", description = "Create a manager-to-employee assignment (HR only)")
    public ApiResponse<AssignmentResponse> assign(@Valid @RequestBody AssignManagerRequest request) {
        log.info("REST: POST /api/hr/manager-assignments - Assigning Manager ID {} to Employee ID {}", request.managerId(), request.employeeId());
        AssignmentResponse response = service.assign(request);
        return ApiResponse.success("Manager assigned to employee successfully", response);
    }

    @GetMapping
    @Operation(summary = "Get All Assignments", description = "Retrieve all active manager-to-employee assignments (HR only)")
    public ApiResponse<List<AssignmentResponse>> getAssignments() {
        log.debug("REST: GET /api/hr/manager-assignments - Listing all assignments");
        return ApiResponse.success(service.getAssignments());
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search Manager Assignments",
            description = "Dynamic multi-filter search with sorting and 0-based pagination for Manager Assignments"
    )
    public ApiResponse<PagedResponse<AssignmentResponse>> searchAssignments(AssignmentSearchCriteria criteria) {
        log.debug("REST: GET /api/hr/manager-assignments/search - Criteria: {}", criteria);
        return ApiResponse.success(searchService.searchAssignments(criteria));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Assignment By ID", description = "Retrieve manager-to-employee assignment details by ID (HR only)")
    public ApiResponse<AssignmentResponse> getById(@PathVariable Long id) {
        log.debug("REST: GET /api/hr/manager-assignments/{} - Fetching assignment", id);
        return ApiResponse.success(service.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Manager Assignment", description = "Update / reassign manager-to-employee relationship (HR only)")
    public ApiResponse<AssignmentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssignmentRequest request
    ) {
        log.info("REST: PUT /api/hr/manager-assignments/{} - Reassigning to Manager ID: {}", id, request.managerId());
        return ApiResponse.success("Manager assignment updated successfully", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete / Deactivate Assignment", description = "Deactivate a manager-to-employee assignment (HR only)")
    public void delete(@PathVariable Long id) {
        log.info("REST: DELETE /api/hr/manager-assignments/{} - Deactivating assignment", id);
        service.delete(id);
    }
}
