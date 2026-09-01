package com.practice.springbootdemo.advance_performance_module.service.hr;

import com.practice.springbootdemo.advance_performance_module.dto.hr.CreateManagerRequest;
import com.practice.springbootdemo.advance_performance_module.dto.hr.EmployeeResponse;
import com.practice.springbootdemo.advance_performance_module.dto.hr.ManagerHierarchyResponse;
import com.practice.springbootdemo.advance_performance_module.entity.Department;
import com.practice.springbootdemo.advance_performance_module.entity.ManagerAssignment;
import com.practice.springbootdemo.advance_performance_module.entity.Role;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.exception.BadRequestException;
import com.practice.springbootdemo.advance_performance_module.exception.ResourceNotFoundException;
import com.practice.springbootdemo.advance_performance_module.repository.DepartmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import com.practice.springbootdemo.advance_performance_module.service.UserCodeGeneratorService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
public class EmployeeManagementService {

    private final UserRepository users;
    private final DepartmentRepository departmentRepository;
    private final ManagerAssignmentRepository managerAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserCodeGeneratorService userCodeGeneratorService;
    private final DepartmentService departmentService;

    public EmployeeManagementService(
            UserRepository users,
            DepartmentRepository departmentRepository,
            ManagerAssignmentRepository managerAssignmentRepository,
            PasswordEncoder passwordEncoder,
            UserCodeGeneratorService userCodeGeneratorService,
            DepartmentService departmentService
    ) {
        this.users = users;
        this.departmentRepository = departmentRepository;
        this.managerAssignmentRepository = managerAssignmentRepository;
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

        String designation = (request.designation() != null && !request.designation().isBlank())
                ? request.designation().trim()
                : department.getName() + " Manager";

        User manager = User.builder()
                .employeeCode(managerCode)
                .name(request.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(passwordToHash))
                .role(Role.MANAGER)
                .departmentId(department.getId())
                .designation(designation)
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

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllStaff() {
        log.debug("Retrieving all active organization staff");
        return users.findAll().stream()
                .filter(User::isActive)
                .map(this::mapToResponse)
                .toList();
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

        // Transition designation to Manager title
        String deptName = (user.getDepartmentId() != null)
                ? departmentRepository.findById(user.getDepartmentId()).map(Department::getName).orElse("Team")
                : "Team";
        user.setDesignation(deptName + " Manager");

        User updated = users.save(user);
        log.info("User ID {} ('{}') successfully promoted to MANAGER. Code transitioned from '{}' to '{}'. Permanent ID #{} preserved.",
                updated.getId(), updated.getName(), oldCode, updated.getEmployeeCode(), updated.getId());

        return mapToResponse(updated);
    }

    @Transactional
    public EmployeeResponse changeManager(Long employeeId, Long newManagerId) {
        log.info("HR changing manager for employee ID {} to new manager ID {}", employeeId, newManagerId);
        if (employeeId.equals(newManagerId)) {
            throw new BadRequestException("An employee cannot be assigned as their own manager");
        }

        User employee = users.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        User manager = users.findById(newManagerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found with ID: " + newManagerId));

        if (manager.getRole() != Role.MANAGER) {
            throw new BadRequestException("Selected user '" + manager.getName() + "' does not hold the MANAGER role");
        }

        // Deactivate previous active assignment(s)
        Optional<ManagerAssignment> currentOpt = managerAssignmentRepository.findByEmployeeIdAndActiveTrue(employeeId);
        currentOpt.ifPresent(curr -> {
            curr.setActive(false);
            managerAssignmentRepository.save(curr);
        });

        // Create new active assignment
        ManagerAssignment newAssignment = ManagerAssignment.builder()
                .employeeId(employeeId)
                .managerId(newManagerId)
                .active(true)
                .assignedDate(LocalDateTime.now())
                .build();
        managerAssignmentRepository.save(newAssignment);

        log.info("Employee ID {} ('{}') successfully reassigned to Manager ID {} ('{}')",
                employeeId, employee.getName(), newManagerId, manager.getName());

        return mapToResponse(employee);
    }

    @Transactional
    public EmployeeResponse transferDepartment(Long employeeId, Long newDepartmentId) {
        log.info("HR transferring employee ID {} to Department ID {}", employeeId, newDepartmentId);
        User user = users.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + employeeId));

        Department department = departmentService.getOrThrow(newDepartmentId);
        user.setDepartmentId(department.getId());
        User saved = users.save(user);

        log.info("Employee ID {} ('{}') successfully transferred to Department '{}'",
                employeeId, user.getName(), department.getName());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ManagerHierarchyResponse> getManagerHierarchy() {
        log.debug("Building organizational manager hierarchy tree");
        List<User> managers = users.findByRoleAndActiveTrue(Role.MANAGER);

        return managers.stream().map(mgr -> {
            String deptName = (mgr.getDepartmentId() != null)
                    ? departmentRepository.findById(mgr.getDepartmentId()).map(Department::getName).orElse("General")
                    : "General";

            List<ManagerAssignment> assignments = managerAssignmentRepository.findByManagerIdAndActiveTrue(mgr.getId());
            List<EmployeeResponse> directReports = assignments.stream()
                    .map(a -> users.findById(a.getEmployeeId()).orElse(null))
                    .filter(Objects::nonNull)
                    .filter(User::isActive)
                    .map(this::mapToResponse)
                    .toList();

            String designation = (mgr.getDesignation() != null && !mgr.getDesignation().isBlank())
                    ? mgr.getDesignation()
                    : deptName + " Manager";

            return new ManagerHierarchyResponse(
                    mgr.getId(),
                    mgr.getEmployeeCode(),
                    mgr.getName(),
                    mgr.getEmail(),
                    designation,
                    mgr.getDepartmentId(),
                    deptName,
                    directReports.size(),
                    directReports
            );
        }).toList();
    }

    private EmployeeResponse mapToResponse(User user) {
        String deptName = (user.getDepartmentId() != null)
                ? departmentRepository.findById(user.getDepartmentId()).map(Department::getName).orElse(null)
                : null;

        String designation = (user.getDesignation() != null && !user.getDesignation().isBlank())
                ? user.getDesignation()
                : (user.getRole() == Role.MANAGER ? (deptName != null ? deptName + " Manager" : "Team Manager")
                   : (user.getRole() == Role.HR ? "HR Administrator" : "Software Engineer"));

        Long managerId = null;
        String managerName = null;
        String managerCode = null;

        if (user.getRole() == Role.EMPLOYEE) {
            Optional<ManagerAssignment> activeAssignment = managerAssignmentRepository.findByEmployeeIdAndActiveTrue(user.getId());
            if (activeAssignment.isPresent()) {
                managerId = activeAssignment.get().getManagerId();
                User mgr = users.findById(managerId).orElse(null);
                if (mgr != null) {
                    managerName = mgr.getName();
                    managerCode = mgr.getEmployeeCode();
                }
            }
        }

        return new EmployeeResponse(
                user.getId(),
                user.getEmployeeCode(),
                user.getName(),
                user.getEmail(),
                user.getDepartmentId(),
                deptName,
                user.getRole(),
                designation,
                managerId,
                managerName,
                managerCode,
                user.getSkill(),
                user.getLocation(),
                user.getDomain(),
                user.getExperienceYears(),
                user.isActive()
        );
    }

    @Transactional(readOnly = true)
    public com.practice.springbootdemo.advance_performance_module.dto.employee.UserProfileResponse getUserProfile(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        String deptName = (user.getDepartmentId() != null)
                ? departmentRepository.findById(user.getDepartmentId()).map(Department::getName).orElse(null)
                : null;

        String designation = (user.getDesignation() != null && !user.getDesignation().isBlank())
                ? user.getDesignation()
                : (user.getRole() == Role.MANAGER ? (deptName != null ? deptName + " Manager" : "Team Manager")
                   : (user.getRole() == Role.HR ? "HR Administrator" : "Software Engineer"));

        Long managerId = null;
        String managerName = null;
        String managerCode = null;

        if (user.getRole() == Role.EMPLOYEE) {
            Optional<ManagerAssignment> activeAssignment = managerAssignmentRepository.findByEmployeeIdAndActiveTrue(user.getId());
            if (activeAssignment.isPresent()) {
                managerId = activeAssignment.get().getManagerId();
                User mgr = users.findById(managerId).orElse(null);
                if (mgr != null) {
                    managerName = mgr.getName();
                    managerCode = mgr.getEmployeeCode();
                }
            }
        }

        return new com.practice.springbootdemo.advance_performance_module.dto.employee.UserProfileResponse(
                user.getId(),
                user.getEmployeeCode(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getDepartmentId(),
                deptName,
                designation,
                managerId,
                managerName,
                managerCode,
                user.getSkill(),
                user.getDomain(),
                user.getLocation(),
                user.getExperienceYears()
        );
    }

    @Transactional
    public com.practice.springbootdemo.advance_performance_module.dto.employee.UserProfileResponse updateUserProfile(
            Long userId,
            com.practice.springbootdemo.advance_performance_module.dto.employee.UpdateProfileRequest request
    ) {
        log.info("Updating profile attributes for User ID {}", userId);
        User user = users.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // 1. Update Full Name if provided
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }

        // 2. Update Credentials if a new password is provided
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (request.newPassword().length() < 6) {
                throw new BadRequestException("New password must be at least 6 characters");
            }
            if (!request.newPassword().equals(request.confirmPassword())) {
                throw new BadRequestException("New password and confirm password do not match");
            }

            // If user has an existing standard password, require current password verification
            boolean isOAuthPlaceholder = user.getPasswordHash() != null && user.getPasswordHash().startsWith("{noop}OAUTH2_NO_PASSWORD_");
            if (user.getPasswordHash() != null && !isOAuthPlaceholder) {
                if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                    throw new BadRequestException("Current password is required to update credentials");
                }
                if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                    throw new BadRequestException("Current password is incorrect");
                }
            }

            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            user.setPasswordChangedAt(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            log.info("Password credentials updated for User ID {}", userId);
        }

        // 3. Update Professional Attributes
        user.setSkill(request.skill().trim());
        user.setDomain(request.domain().trim());
        user.setLocation(request.location().trim());
        user.setExperienceYears(request.experienceYears());

        User saved = users.save(user);
        log.info("Profile successfully updated for User ID {} ('{}')", saved.getId(), saved.getName());
        return getUserProfile(saved.getId());
    }
}