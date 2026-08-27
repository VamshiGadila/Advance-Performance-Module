package com.practice.springbootdemo.advance_performance_module.controller.search;

import com.practice.springbootdemo.advance_performance_module.dto.common.ApiResponse;
import com.practice.springbootdemo.advance_performance_module.dto.common.PagedResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.EmployeeResponse;
import com.practice.springbootdemo.advance_performance_module.dto.search.EmployeeSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.service.search.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/employees")
@Tag(name = "Search & Filtering", description = "Advanced Dynamic Search, Pagination & Sorting APIs")
@SecurityRequirement(name = "BearerAuth")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('HR', 'MANAGER', 'EMPLOYEE')")
    @Operation(
            summary = "Advanced Dynamic Employee Search",
            description = "Search employees dynamically with optional filters (name, email, skill, location, domain, experience, status), sorting, and 0-based pagination."
    )
    public ApiResponse<PagedResponse<EmployeeResponse>> searchEmployees(EmployeeSearchCriteria criteria) {
        log.info("REST: GET /api/employees/search - Criteria search request received: search='{}', code='{}'",
                criteria.getSearch(), criteria.getEmployeeCode());
        return ApiResponse.success(searchService.searchEmployees(criteria));
    }
}