package com.example.hrmspolicies2.config;

import com.example.hrmspolicies2.entity.Policy;
import com.example.hrmspolicies2.entity.User;
import com.example.hrmspolicies2.repository.PolicyRepository;
import com.example.hrmspolicies2.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * Seeds a default ADMIN account and starter HR policies on first startup.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initializeData(
            UserRepository userRepository,
            PolicyRepository policyRepository,
            PasswordEncoder passwordEncoder
    ) {

        return args -> {

            String adminEmail = "admin@gmail.com";
            User admin = userRepository.findByEmail(adminEmail).orElse(null);

            if (admin == null) {
                admin = User.builder()
                        .name("HR Admin")
                        .email(adminEmail)
                        .password(passwordEncoder.encode("admin123"))
                        .role("ADMIN")
                        .build();

                admin = userRepository.save(admin);

                log.info("=================================");
                log.info("Default admin account created (demo/dev only)");
                log.info("Email: {}", adminEmail);
                log.info("Password: admin123");
                log.info("=================================");
            }

            if (policyRepository.count() == 0) {
                List<Policy> samplePolicies = List.of(
                        Policy.builder()
                                .name("Annual Paid Time Off & Leave Policy")
                                .code("HR-POL-001")
                                .category("LEAVE")
                                .applicability("ALL")
                                .mandatory(true)
                                .status("ACTIVE")
                                .content("All full-time employees are entitled to 20 days of paid annual leave per calendar year. Leaves must be requested at least 5 business days in advance via the HRMS portal and approved by the reporting manager.")
                                .createdBy(admin)
                                .build(),
                        Policy.builder()
                                .name("Remote Work & Hybrid Flexibility Guidelines")
                                .code("HR-POL-002")
                                .category("WORKPLACE")
                                .applicability("ALL")
                                .mandatory(true)
                                .status("ACTIVE")
                                .content("Employees may work up to 2 days per week remotely with department lead alignment. Core collaboration hours are 10:00 AM to 4:00 PM local time.")
                                .createdBy(admin)
                                .build(),
                        Policy.builder()
                                .name("Code of Professional Conduct & Anti-Harassment")
                                .code("HR-POL-003")
                                .category("CONDUCT")
                                .applicability("ALL")
                                .mandatory(true)
                                .status("ACTIVE")
                                .content("We maintain zero tolerance for discrimination, harassment, or unethical conduct. All team members must treat peers, clients, and partners with dignity and respect.")
                                .createdBy(admin)
                                .build(),
                        Policy.builder()
                                .name("Information Security & Clean Desk Policy")
                                .code("HR-POL-004")
                                .category("SECURITY")
                                .applicability("ALL")
                                .mandatory(true)
                                .status("ACTIVE")
                                .content("Laptops must be locked whenever unattended. Two-factor authentication (2FA) is mandatory for all enterprise accounts. Passwords must never be shared.")
                                .createdBy(admin)
                                .build(),
                        Policy.builder()
                                .name("Employee Health Insurance & Wellness Benefits")
                                .code("HR-POL-005")
                                .category("BENEFITS")
                                .applicability("FULL_TIME")
                                .mandatory(false)
                                .status("ACTIVE")
                                .content("Comprehensive medical, dental, and vision insurance coverage provided to all full-time employees and immediate dependents. Annual wellness allowance of $500 available.")
                                .createdBy(admin)
                                .build()
                );

                policyRepository.saveAll(samplePolicies);
                log.info("Initialized {} starter HR policies", samplePolicies.size());
            }
        };
    }
}

