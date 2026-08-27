package com.practice.springbootdemo.advance_performance_module.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Manager Assignment search and pagination parameters")
public class AssignmentSearchCriteria {
    @Schema(description = "Manager ID filter", example = "2")
    private Long managerId;

    @Schema(description = "Employee ID filter", example = "5")
    private Long employeeId;

    @Schema(description = "Performance Cycle ID filter", example = "1")
    private Long cycleId;

    @Schema(description = "Active assignment filter", example = "true")
    private Boolean active;

    @Builder.Default
    @Schema(description = "Zero-based page number", example = "0")
    private Integer page = 0;

    @Builder.Default
    @Schema(description = "Page size", example = "10")
    private Integer size = 10;

    @Builder.Default
    @Schema(description = "Sort property", example = "id")
    private String sortBy = "id";

    @Builder.Default
    @Schema(description = "Sort direction (asc / desc)", example = "desc")
    private String direction = "desc";
}
