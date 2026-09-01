package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.entity.AuthProvider;
import com.practice.springbootdemo.advance_performance_module.entity.Department;
import com.practice.springbootdemo.advance_performance_module.entity.Role;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import com.practice.springbootdemo.advance_performance_module.exception.UnauthorizedException;
import com.practice.springbootdemo.advance_performance_module.repository.DepartmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.ManagerAssignmentRepository;
import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class OAuth2AuthService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ManagerAssignmentRepository managerAssignmentRepository;
    private final UserCodeGeneratorService userCodeGeneratorService;

    public OAuth2AuthService(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            ManagerAssignmentRepository managerAssignmentRepository,
            UserCodeGeneratorService userCodeGeneratorService
    ) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.managerAssignmentRepository = managerAssignmentRepository;
        this.userCodeGeneratorService = userCodeGeneratorService;
    }

    @Transactional
    public User processOAuth2User(String email, String name, String googleSub) {
        String cleanEmail = email.trim().toLowerCase();
        log.info("Processing Google OAuth2 authentication for email: '{}', Google ID: '{}'", cleanEmail, googleSub);

        Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(cleanEmail);

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            log.info("Found existing user for email '{}' with Role '{}'", cleanEmail, existingUser.getRole());

            if (!existingUser.isActive()) {
                log.warn("OAuth login blocked: Account is inactive for User ID {}", existingUser.getId());
                throw new UnauthorizedException("Your account is inactive. Please contact HR.");
            }

            if (existingUser.isAccountLocked()) {
                log.warn("OAuth login blocked: Account locked for User ID {}", existingUser.getId());
                throw new UnauthorizedException("Your account is temporarily locked.");
            }

            if (existingUser.getProviderId() == null || existingUser.getProviderId().isBlank()) {
                existingUser.setAuthProvider(AuthProvider.GOOGLE);
                existingUser.setProviderId(googleSub);
            }

            if ((existingUser.getName() == null || existingUser.getName().isBlank()) && name != null) {
                existingUser.setName(name);
            }

            existingUser.setFailedLoginAttempts(0);
            existingUser.setLockoutUntil(null);
            return userRepository.save(existingUser);
        }

        log.info("User with email '{}' not found. Creating new EMPLOYEE record via Google OAuth", cleanEmail);
        String employeeCode = userCodeGeneratorService.generateEmployeeCode();

        Long defaultDeptId = departmentRepository.findAll().stream()
                .findFirst()
                .map(Department::getId)
                .orElse(null);

        User newUser = User.builder()
                .employeeCode(employeeCode)
                .name(name != null && !name.isBlank() ? name.trim() : "Google User")
                .email(cleanEmail)
                .passwordHash("{noop}OAUTH2_NO_PASSWORD_" + UUID.randomUUID())
                .role(Role.EMPLOYEE)
                .authProvider(AuthProvider.GOOGLE)
                .providerId(googleSub)
                .departmentId(defaultDeptId)
                .designation("Software Engineer")
                .active(true)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("Successfully provisioned new Employee via Google OAuth: ID={}, Code={}, Email={}",
                savedUser.getId(), savedUser.getEmployeeCode(), savedUser.getEmail());


        return savedUser;
    }
}