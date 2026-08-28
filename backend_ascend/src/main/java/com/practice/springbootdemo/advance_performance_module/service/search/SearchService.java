package com.practice.springbootdemo.advance_performance_module.service.search;

import com.practice.springbootdemo.advance_performance_module.dto.common.PagedResponse;
import com.practice.springbootdemo.advance_performance_module.dto.employee.EmployeeGoalResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.AssignmentResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.CycleResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.EmployeeResponse;
import com.practice.springbootdemo.advance_performance_module.dto.manager.GoalResponse;
import com.practice.springbootdemo.advance_performance_module.dto.search.AssignmentSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.dto.search.CycleSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.dto.search.EmployeeSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.dto.search.GoalSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.entity.*;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.repository.*;
import com.practice.springbootdemo.advance_performance_module.specification.AssignmentSpecification;
import com.practice.springbootdemo.advance_performance_module.specification.CycleSpecification;
import com.practice.springbootdemo.advance_performance_module.specification.EmployeeSpecification;
import com.practice.springbootdemo.advance_performance_module.specification.GoalSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchService {
    private static final Set<String> ALLOWED_EMPLOYEE_SORT_FIELDS = Set.of(
            "id", "name", "email", "employeeCode", "experienceYears", "location", "skill", "domain", "createdAt"
    );
    private static final Set<String> ALLOWED_GOAL_SORT_FIELDS = Set.of(
            "id", "title", "dueDate", "weight", "progress", "status", "createdAt", "updatedAt"
    );
    private static final Set<String> ALLOWED_CYCLE_SORT_FIELDS = Set.of(
            "id", "name", "startDate", "endDate", "status", "createdAt"
    );
    private static final Set<String> ALLOWED_ASSIGNMENT_SORT_FIELDS = Set.of(
            "id", "managerId", "employeeId", "performanceCycleId", "active", "assignedDate"
    );

    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final ManagerAssignmentRepository assignmentRepository;
    private final DepartmentRepository departmentRepository;
    private final PerformanceCycleRepository performanceCycleRepository;

    public SearchService(
            UserRepository userRepository,
            GoalRepository goalRepository,
            ManagerAssignmentRepository assignmentRepository,
            DepartmentRepository departmentRepository,
            PerformanceCycleRepository performanceCycleRepository
    ) {
        this.userRepository = userRepository;
        this.goalRepository = goalRepository;
        this.assignmentRepository = assignmentRepository;
        this.departmentRepository = departmentRepository;
        this.performanceCycleRepository = performanceCycleRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<EmployeeResponse> searchEmployees(EmployeeSearchCriteria criteria) {
        log.info("Executing employee criteria search: search='{}', code='{}', role={}, dept={}",
                criteria.getSearch(), criteria.getEmployeeCode(), criteria.getRole(), criteria.getDepartmentId());

        long startTime = System.currentTimeMillis();
        Pageable pageable = createPageable(
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSortBy(),
                criteria.getDirection(),
                ALLOWED_EMPLOYEE_SORT_FIELDS,
                "name"
        );
        Page<User> userPage = userRepository.findAll(EmployeeSpecification.filterBy(criteria), pageable);
        long duration = System.currentTimeMillis() - startTime;
        log.debug("Employee criteria search completed in {}ms. Found {} records across {} pages",
                duration, userPage.getTotalElements(), userPage.getTotalPages());

        Map<Long, String> deptMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));

        Map<Long, ManagerAssignment> activeAssignMap = assignmentRepository.findByActiveTrue().stream()
                .collect(Collectors.toMap(ManagerAssignment::getEmployeeId, a -> a, (a, b) -> a));

        Map<Long, User> userMap = userRepository.findAll().stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        Page<EmployeeResponse> dtoPage = userPage.map(u -> {
            String deptName = u.getDepartmentId() != null ? deptMap.getOrDefault(u.getDepartmentId(), "-") : "-";
            String designation = (u.getDesignation() != null && !u.getDesignation().isBlank())
                    ? u.getDesignation()
                    : (u.getRole() == Role.MANAGER ? (deptName.equals("-") ? "Team Manager" : deptName + " Manager")
                       : (u.getRole() == Role.HR ? "HR Administrator" : "Software Engineer"));

            Long mgrId = null;
            String mgrName = null;
            String mgrCode = null;
            if (u.getRole() == Role.EMPLOYEE && activeAssignMap.containsKey(u.getId())) {
                ManagerAssignment ma = activeAssignMap.get(u.getId());
                mgrId = ma.getManagerId();
                User mgr = userMap.get(mgrId);
                if (mgr != null) {
                    mgrName = mgr.getName();
                    mgrCode = mgr.getEmployeeCode();
                }
            }

            return new EmployeeResponse(
                    u.getId(),
                    u.getEmployeeCode(),
                    u.getName(),
                    u.getEmail(),
                    u.getDepartmentId(),
                    deptName,
                    u.getRole(),
                    designation,
                    mgrId,
                    mgrName,
                    mgrCode,
                    u.getSkill(),
                    u.getLocation(),
                    u.getDomain(),
                    u.getExperienceYears(),
                    u.isActive()
            );
        });
        return PagedResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<GoalResponse> searchManagerGoals(GoalSearchCriteria criteria, Long managerId) {
        log.info("Executing manager goals search for Manager ID: {}", managerId);

        long startTime = System.currentTimeMillis();
        List<Long> assignedEmployeeIds = assignmentRepository.findByManagerIdAndActiveTrue(managerId).stream()
                .map(ManagerAssignment::getEmployeeId)
                .toList();
        Pageable pageable = createPageable(
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSortBy(),
                criteria.getDirection(),
                ALLOWED_GOAL_SORT_FIELDS,
                "dueDate"
        );
        Page<Goal> goalPage = goalRepository.findAll(GoalSpecification.filterBy(criteria, assignedEmployeeIds), pageable);
        long duration = System.currentTimeMillis() - startTime;
        log.debug("Manager goals search completed in {}ms. Found {} records", duration, goalPage.getTotalElements());

        Map<Long, String> userNames = userRepository.findAllById(
                goalPage.getContent().stream().flatMap(g -> java.util.stream.Stream.of(g.getEmployeeId(), g.getManagerId())).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, User::getName));

        Page<GoalResponse> dtoPage = goalPage.map(g -> new GoalResponse(
                g.getId(),
                g.getCycleId(),
                g.getEmployeeId(),
                userNames.getOrDefault(g.getEmployeeId(), "Unknown"),
                g.getManagerId(),
                userNames.getOrDefault(g.getManagerId(), "Unknown"),
                g.getGoalType(),
                g.getGoalScope(),
                g.getParentGoalId(),
                g.getTitle(),
                g.getDescription(),
                g.getTarget(),
                g.getWeight(),
                g.getDueDate(),
                g.getStatus(),
                g.getProgress(),
                g.getEmployeeComment(),
                g.getManagerComment(),
                g.isModificationRequested(),
                g.isEmployeeAccepted(),
                g.getCreatedAt(),
                g.getUpdatedAt(),
                g.getCompletedAt()
        ));
        return PagedResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<EmployeeGoalResponse> searchEmployeeGoals(GoalSearchCriteria criteria, Long employeeId) {
        log.info("Executing employee goals search for Employee ID: {}", employeeId);
        criteria.setEmployeeId(employeeId);
        Pageable pageable = createPageable(
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSortBy(),
                criteria.getDirection(),
                ALLOWED_GOAL_SORT_FIELDS,
                "dueDate"
        );
        Page<Goal> goalPage = goalRepository.findAll(GoalSpecification.filterBy(criteria, List.of(employeeId)), pageable);
        log.debug("Employee goals search found {} records", goalPage.getTotalElements());

        Page<EmployeeGoalResponse> dtoPage = goalPage.map(g -> new EmployeeGoalResponse(
                g.getId(),
                g.getCycleId(),
                g.getGoalType(),
                g.getGoalScope(),
                g.getTitle(),
                g.getDescription(),
                g.getTarget(),
                g.getWeight(),
                g.getDueDate(),
                g.getStatus(),
                g.getProgress(),
                g.getEmployeeComment(),
                g.getManagerComment(),
                g.isEmployeeAccepted(),
                g.isModificationRequested(),
                g.getCreatedAt(),
                g.getUpdatedAt(),
                g.getCompletedAt()
        ));
        return PagedResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CycleResponse> searchCycles(CycleSearchCriteria criteria) {
        log.info("Executing performance cycles search: status={}", criteria.getStatus());
        Pageable pageable = createPageable(
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSortBy(),
                criteria.getDirection(),
                ALLOWED_CYCLE_SORT_FIELDS,
                "startDate"
        );
        Page<PerformanceCycle> cyclePage = performanceCycleRepository.findAll(CycleSpecification.filterBy(criteria), pageable);
        log.debug("Cycles search returned {} records", cyclePage.getTotalElements());

        Page<CycleResponse> dtoPage = cyclePage.map(c -> new CycleResponse(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getStartDate(),
                c.getEndDate(),
                c.getStatus(),
                c.getCreatedBy(),
                c.getCreatedAt()
        ));
        return PagedResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AssignmentResponse> searchAssignments(AssignmentSearchCriteria criteria) {
        log.info("Executing manager assignments search: managerId={}, employeeId={}", criteria.getManagerId(), criteria.getEmployeeId());
        Pageable pageable = createPageable(
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSortBy(),
                criteria.getDirection(),
                ALLOWED_ASSIGNMENT_SORT_FIELDS,
                "id"
        );
        Page<ManagerAssignment> assignmentPage = assignmentRepository.findAll(AssignmentSpecification.filterBy(criteria), pageable);
        log.debug("Assignments search returned {} records", assignmentPage.getTotalElements());

        Map<Long, User> userMap = userRepository.findAllById(
                assignmentPage.getContent().stream()
                        .flatMap(a -> java.util.stream.Stream.of(a.getEmployeeId(), a.getManagerId()))
                        .collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, String> cycleMap = performanceCycleRepository.findAll().stream()
                .collect(Collectors.toMap(PerformanceCycle::getId, PerformanceCycle::getName, (a, b) -> a));

        Page<AssignmentResponse> dtoPage = assignmentPage.map(a -> {
            User emp = userMap.get(a.getEmployeeId());
            User mgr = userMap.get(a.getManagerId());
            return new AssignmentResponse(
                    a.getId(),
                    a.getEmployeeId(),
                    emp != null ? emp.getName() : "Unknown",
                    emp != null ? emp.getEmployeeCode() : "-",
                    a.getManagerId(),
                    mgr != null ? mgr.getName() : "Unknown",
                    mgr != null ? mgr.getEmployeeCode() : "-",
                    a.getPerformanceCycleId(),
                    a.getPerformanceCycleId() != null ? cycleMap.getOrDefault(a.getPerformanceCycleId(), "-") : "-",
                    a.isActive(),
                    a.getAssignedDate()
            );
        });
        return PagedResponse.from(dtoPage);
    }

    private Pageable createPageable(
            int page,
            int size,
            String sortBy,
            String direction,
            Set<String> allowedSortFields,
            String defaultSortField
    ) {
        if (page < 0) {
            log.warn("Invalid pagination requested: Negative page index {}", page);
            throw new BadRequestException("Page index cannot be negative (Zero-based pagination: page >= 0)");
        }
        if (size <= 0 || size > 100) {
            log.warn("Invalid pagination requested: Size {}", size);
            throw new BadRequestException("Page size must be between 1 and 100");
        }
        String sortProperty = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim() : defaultSortField;
        if (!allowedSortFields.contains(sortProperty)) {
            log.warn("Invalid sort field requested: '{}'", sortProperty);
            throw new BadRequestException("Invalid sort field: '" + sortProperty + "'. Allowed fields: " + allowedSortFields);
        }
        Sort.Direction sortDirection = Sort.Direction.ASC;
        if (direction != null && !direction.isBlank()) {
            if ("desc".equalsIgnoreCase(direction.trim())) {
                sortDirection = Sort.Direction.DESC;
            } else if (!"asc".equalsIgnoreCase(direction.trim())) {
                throw new BadRequestException("Invalid sort direction: '" + direction + "'. Allowed: 'asc', 'desc'");
            }
        }
        return PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));
    }
}