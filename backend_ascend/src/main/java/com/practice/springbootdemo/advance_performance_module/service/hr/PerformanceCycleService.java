package com.practice.springbootdemo.advance_performance_module.service.hr;

import com.practice.springbootdemo.advance_performance_module.dto.hr.CreateCycleRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.CycleResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.UpdateCycleRequest;
import com.practice.springbootdemo.advance_performance_module.entity.CycleStatus;
import com.practice.springbootdemo.advance_performance_module.entity.PerformanceCycle;
import com.practice.springbootdemo.advance_performance_module.exception.DuplicateResourceException;
import com.practice.springbootdemo.advance_performance_module.exception.InvalidPerformanceCycleException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.PerformanceCycleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class PerformanceCycleService {
    private final PerformanceCycleRepository repository;

    public PerformanceCycleService(PerformanceCycleRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CycleResponse create(CreateCycleRequest request, Long hrUserId) {
        log.info("HR User ID {} initiating creation of Performance Cycle: '{}' [{} to {}]",
                hrUserId, request.name(), request.startDate(), request.endDate());

        if (request.endDate().isBefore(request.startDate())) {
            log.warn("Cycle creation rejected: End date {} is before start date {}", request.endDate(), request.startDate());
            throw new InvalidPerformanceCycleException("End date cannot be before start date");
        }
        if (repository.existsByNameIgnoreCase(request.name().trim())) {
            log.warn("Cycle creation rejected: Cycle name '{}' already exists", request.name());
            throw new DuplicateResourceException("Performance cycle name already exists: " + request.name());
        }

        PerformanceCycle cycle = PerformanceCycle.builder()
                .name(request.name().trim())
                .description(request.description())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(CycleStatus.DRAFT)
                .createdBy(hrUserId)
                .build();

        PerformanceCycle saved = repository.save(cycle);
        log.info("Performance Cycle created in DRAFT status: ID={}, Name='{}'", saved.getId(), saved.getName());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CycleResponse> getAll() {
        log.debug("Retrieving all performance cycles");
        List<CycleResponse> cycles = repository.findAll().stream().map(this::mapToResponse).toList();
        log.debug("Found {} total performance cycles", cycles.size());
        return cycles;
    }

    @Transactional(readOnly = true)
    public CycleResponse getActive() {
        log.debug("Fetching current active performance cycle");
        PerformanceCycle cycle = repository.findFirstByStatusOrderByStartDateDesc(CycleStatus.ACTIVE)
                .orElseThrow(() -> {
                    log.warn("No active performance cycle found in system");
                    return new ResourceNotFoundException("No ACTIVE performance cycle found");
                });
        log.debug("Active performance cycle identified: ID={}, Name='{}'", cycle.getId(), cycle.getName());
        return mapToResponse(cycle);
    }

    @Transactional(readOnly = true)
    public CycleResponse getById(Long id) {
        log.debug("Fetching Performance Cycle by ID: {}", id);
        PerformanceCycle cycle = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Performance Cycle lookup failed for ID: {}", id);
                    return new ResourceNotFoundException("Performance cycle not found with ID: " + id);
                });
        return mapToResponse(cycle);
    }

    @Transactional
    public CycleResponse update(Long id, UpdateCycleRequest request) {
        log.info("Updating Performance Cycle ID: {}", id);
        PerformanceCycle cycle = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Performance Cycle update failed: ID {} not found", id);
                    return new ResourceNotFoundException("Performance cycle not found with ID: " + id);
                });

        if (cycle.getStatus() == CycleStatus.CLOSED) {
            log.warn("Update rejected: Cannot modify CLOSED cycle ID: {}", id);
            throw new InvalidPerformanceCycleException("Cannot modify a CLOSED performance cycle");
        }
        if (request.endDate().isBefore(request.startDate())) {
            log.warn("Update rejected: End date {} is before start date {}", request.endDate(), request.startDate());
            throw new InvalidPerformanceCycleException("End date cannot be before start date");
        }

        cycle.setName(request.name().trim());
        cycle.setDescription(request.description());
        cycle.setStartDate(request.startDate());
        cycle.setEndDate(request.endDate());
        PerformanceCycle saved = repository.save(cycle);
        log.info("Performance Cycle ID {} updated successfully: Name='{}'", id, saved.getName());
        return mapToResponse(saved);
    }

    @Transactional
    public CycleResponse activate(Long id) {
        log.info("Launching / Activating Performance Cycle ID: {}", id);
        PerformanceCycle cycle = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Activation failed: Performance Cycle ID {} not found", id);
                    return new ResourceNotFoundException("Performance cycle not found with ID: " + id);
                });

        if (cycle.getStatus() == CycleStatus.CLOSED) {
            log.warn("Activation rejected: Cannot activate CLOSED cycle ID: {}", id);
            throw new InvalidPerformanceCycleException("Cannot activate a CLOSED performance cycle");
        }

        cycle.setStatus(CycleStatus.ACTIVE);
        PerformanceCycle saved = repository.save(cycle);
        log.info("Performance Cycle ID {} successfully launched to ACTIVE status", id);
        return mapToResponse(saved);
    }

    @Transactional
    public CycleResponse close(Long id) {
        log.info("Closing Performance Cycle ID: {}", id);
        PerformanceCycle cycle = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Closure failed: Performance Cycle ID {} not found", id);
                    return new ResourceNotFoundException("Performance cycle not found with ID: " + id);
                });

        cycle.setStatus(CycleStatus.CLOSED);
        PerformanceCycle saved = repository.save(cycle);
        log.info("Performance Cycle ID {} successfully transitioned to CLOSED", id);
        return mapToResponse(saved);
    }

    private CycleResponse mapToResponse(PerformanceCycle cycle) {
        return new CycleResponse(
                cycle.getId(),
                cycle.getName(),
                cycle.getDescription(),
                cycle.getStartDate(),
                cycle.getEndDate(),
                cycle.getStatus(),
                cycle.getCreatedBy(),
                cycle.getCreatedAt()
        );
    }
}
