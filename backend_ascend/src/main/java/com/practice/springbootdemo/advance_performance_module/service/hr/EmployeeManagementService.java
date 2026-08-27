package com.practice.springbootdemo.advance_performance_module.service.hr;

import com.practice.springbootdemo.advance_performance_module.dto.hr.CreateManagerRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.EmployeeResponse;
import com.practice.springbootdemo.advance_performance_module.entity.Department;
import com.practice.springbootdemo.advance_performance_module.entity.Role;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.DepartmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import com.practice.springbootdemo.advance_performance_module.service.UserCodeGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class EmployeeManagementService {
    private final UserRepository users;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserCodeGeneratorService userCodeGeneratorService;
    private final DepartmentService departmentService;

    public EmployeeManagementService(
            UserRepository users,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder,
            UserCodeGeneratorService userCodeGeneratorService,
            DepartmentService departmentService
    ) {
        this.users = users;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.userCodeGeneratorService = userCodeGeneratorService;
        this.departmentService = departmentService;
    }

    @Transactional
    public EmployeeResponse createManager(CreateManagerRequest request) {
        String email = request.email().trim().toLowerCase();
        log.info("HR initiating manager creation: Name='{}', Email='{}', Department ID={}", request.name(), email, request.departmentId());

        if (users.existsByEmailIgnoreCase(email)) {
            log.warn("Manager creation rejected: Email '{}' already registered", email);
            throw new BadRequestException("Email is already registered: " + email);
        }

        Department department = departmentService.getOrThrow(request.departmentId());
        String managerCode = userCodeGeneratorService.generateManagerCode();
        String passwordToHash = request.getEffectivePassword();

        User manager = User.builder()
                .employeeCode(managerCode)
                .name(request.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(passwordToHash))
                .role(Role.MANAGER)
                .departmentId(department.getId())
                .active(true)
                .build();

        User saved = users.save(manager);
        log.info("Manager account created successfully: ID={}, Code={}, Email={}", saved.getId(), saved.getEmployeeCode(), saved.getEmail());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployees() {
        log.debug("Retrieving all active employees");
        List<EmployeeResponse> list = users.findByRoleAndActiveTrue(Role.EMPLOYEE).stream()
                .map(this::mapToResponse)
                .toList();
        log.debug("Retrieved {} active employees", list.size());
        return list;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getManagers() {
        log.debug("Retrieving all active managers");
        List<EmployeeResponse> list = users.findByRoleAndActiveTrue(Role.MANAGER).stream()
                .map(this::mapToResponse)
                .toList();
        log.debug("Retrieved {} active managers", list.size());
        return list;
    }

    @Transactional
    public EmployeeResponse promoteToManager(Long id) {
        log.info("HR initiating promotion to MANAGER for User ID {}", id);
        User user = users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));

        if (user.getRole() == Role.MANAGER) {
            log.warn("Promotion skipped: User ID {} is already a MANAGER", id);
            throw new BadRequestException("User '" + user.getName() + "' is already assigned the MANAGER role");
        }

        if (user.getRole() == Role.HR) {
            log.warn("Promotion rejected: User ID {} is an HR Administrator", id);
            throw new BadRequestException("Cannot modify role of an HR Administrator");
        }

        // Transition role to MANAGER and assign MGR code
        String oldCode = user.getEmployeeCode();
        String newManagerCode = userCodeGeneratorService.generateManagerCodeFor(oldCode);
        user.setRole(Role.MANAGER);
        user.setEmployeeCode(newManagerCode);

        User updated = users.save(user);
        log.info("User ID {} ('{}') successfully promoted to MANAGER. Code transitioned from '{}' to '{}'. Permanent ID #{} preserved.",
                updated.getId(), updated.getName(), oldCode, updated.getEmployeeCode(), updated.getId());

        return mapToResponse(updated);
    }

    private EmployeeResponse mapToResponse(User user) {
        String deptName = (user.getDepartmentId() != null)
                ? departmentRepository.findById(user.getDepartmentId()).map(Department::getName).orElse(null)
                : null;
        return new EmployeeResponse(
                user.getId(),
                user.getEmployeeCode(),
                user.getName(),
                user.getEmail(),
                user.getDepartmentId(),
                deptName,
                user.getRole(),
                user.getSkill(),
                user.getLocation(),
                user.getDomain(),
                user.getExperienceYears(),
                user.isActive()
        );
    }
}