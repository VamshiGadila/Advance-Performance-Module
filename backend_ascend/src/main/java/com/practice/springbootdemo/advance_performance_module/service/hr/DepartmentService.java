package com.practice.springbootdemo.advance_performance_module.service.hr;

import com.practice.springbootdemo.advance_performance_module.dto.auth.PublicDepartmentResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.CreateDepartmentRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.DepartmentResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.EmployeeResponse;
import com.practice.springbootdemo.advance_performance_module.entity.Department;
import com.practice.springbootdemo.advance_performance_module.entity.Role;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.DuplicateResourceException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.DepartmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class DepartmentService {
    private final DepartmentRepository repository;
    private final UserRepository userRepository;

    public DepartmentService(DepartmentRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        String name = request.name().trim();
        log.info("Creating new Department: '{}'", name);

        if (repository.existsByNameIgnoreCase(name)) {
            log.warn("Department creation rejected: Department '{}' already exists", name);
            throw new DuplicateResourceException("Department already exists: " + name);
        }

        Long defaultManagerId = request.defaultManagerId();
        if (defaultManagerId != null) {
            validateIsActiveManager(defaultManagerId);
        }

        Department department = Department.builder()
                .name(name)
                .description(request.description())
                .defaultManagerId(defaultManagerId)
                .build();

        Department saved = repository.save(department);
        log.info("Department created successfully: ID={}, Name='{}'", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list() {
        log.debug("Listing all departments");
        List<DepartmentResponse> list = repository.findAll().stream().map(this::toResponse).toList();
        log.debug("Retrieved {} total departments", list.size());
        return list;
    }

    @Transactional(readOnly = true)
    public List<PublicDepartmentResponse> listPublic() {
        log.debug("Listing public departments for registration");
        return repository.findAll().stream()
                .map(d -> new PublicDepartmentResponse(d.getId(), d.getName()))
                .toList();
    }

    @Transactional
    public DepartmentResponse setDefaultManager(Long departmentId, Long managerId) {
        log.info("Setting Default Manager ID {} for Department ID {}", managerId, departmentId);
        Department department = getOrThrow(departmentId);
        User manager = validateIsActiveManager(managerId);

        if (manager.getDepartmentId() == null || !manager.getDepartmentId().equals(departmentId)) {
            log.warn("Cannot set default manager: Manager {} does not belong to Department ID {}", manager.getEmployeeCode(), departmentId);
            throw new BadRequestException("Manager " + manager.getEmployeeCode() + " does not belong to this department");
        }

        department.setDefaultManagerId(managerId);
        Department saved = repository.save(department);
        log.info("Default Manager successfully updated: Department ID {}, Manager ID {}", departmentId, managerId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getDepartmentEmployees(Long departmentId) {
        log.debug("Fetching employees for Department ID: {}", departmentId);
        getOrThrow(departmentId);
        List<EmployeeResponse> employees = userRepository.findByDepartmentIdAndActiveTrue(departmentId).stream()
                .filter(u -> u.getRole() == Role.EMPLOYEE)
                .map(u -> new EmployeeResponse(
                        u.getId(),
                        u.getEmployeeCode(),
                        u.getName(),
                        u.getEmail(),
                        u.getDepartmentId(),
                        u.getRole()
                ))
                .toList();
        log.debug("Found {} active employees in Department ID {}", employees.size(), departmentId);
        return employees;
    }

    public Department getOrThrow(Long departmentId) {
        return repository.findById(departmentId)
                .orElseThrow(() -> {
                    log.error("Department lookup failed for ID: {}", departmentId);
                    return new ResourceNotFoundException("Department not found with ID: " + departmentId);
                });
    }

    public Optional<Department> find(Long departmentId) {
        return repository.findById(departmentId);
    }

    private User validateIsActiveManager(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> {
                    log.error("Manager lookup failed for ID: {}", managerId);
                    return new ResourceNotFoundException("Manager not found with ID: " + managerId);
                });
        if (manager.getRole() != Role.MANAGER) {
            log.warn("User ID {} is not a Manager (Current Role: {})", managerId, manager.getRole());
            throw new BadRequestException("Selected user is not a manager");
        }
        if (!manager.isActive()) {
            log.warn("Manager ID {} is inactive", managerId);
            throw new BadRequestException("Selected manager is not active");
        }
        return manager;
    }

    private DepartmentResponse toResponse(Department department) {
        String managerName = null;
        if (department.getDefaultManagerId() != null) {
            managerName = userRepository.findById(department.getDefaultManagerId())
                    .map(User::getName)
                    .orElse(null);
        }
        long employeeCount = userRepository.findByDepartmentIdAndRoleAndActiveTrue(
                department.getId(),
                Role.EMPLOYEE
        ).size();
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getDescription(),
                department.getDefaultManagerId(),
                managerName,
                employeeCount
        );
    }
}