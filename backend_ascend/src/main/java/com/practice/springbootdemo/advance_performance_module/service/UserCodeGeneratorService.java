package com.practice.springbootdemo.advance_performance_module.service;

import com.practice.springbootdemo.advance_performance_module.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserCodeGeneratorService {
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    public UserCodeGeneratorService(JdbcTemplate jdbcTemplate, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void initSequences() {
        try {
            jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS employee_code_seq START WITH 1 INCREMENT BY 1");
            jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS manager_code_seq START WITH 1 INCREMENT BY 1");
            jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS hr_code_seq START WITH 1 INCREMENT BY 1");
            log.info("PostgreSQL user code sequence counters verified/initialized.");
        } catch (Exception e) {
            log.warn("Sequence initialization notice: {}", e.getMessage());
        }
    }

    public synchronized String generateEmployeeCode() {
        String code = getNextUniqueCode("employee_code_seq", "EMP");
        log.debug("Generated unique employee code: '{}'", code);
        return code;
    }

    public synchronized String generateManagerCodeFor(String currentCode) {
        if (currentCode != null && currentCode.startsWith("EMP")) {
            String candidate = "MGR" + currentCode.substring(3);
            if (!userRepository.existsByEmployeeCode(candidate)) {
                log.info("Generating matching manager code '{}' for promoted employee '{}'", candidate, currentCode);
                return candidate;
            }
        }
        return generateManagerCode();
    }

    public synchronized String generateManagerCode() {
        String code = getNextUniqueCode("manager_code_seq", "MGR");
        log.debug("Generated unique manager code: '{}'", code);
        return code;
    }

    public synchronized String generateHRCode() {
        String code = getNextUniqueCode("hr_code_seq", "HR");
        log.debug("Generated unique HR code: '{}'", code);
        return code;
    }

    private String getNextUniqueCode(String sequenceName, String prefix) {
        String code;
        int attempts = 0;
        do {
            Long number = getNextVal(sequenceName);
            code = String.format("%s%03d", prefix, number != null ? number : (attempts + 1));
            attempts++;
        } while (userRepository.existsByEmployeeCode(code) && attempts < 1000);
        return code;
    }

    private Long getNextVal(String sequenceName) {
        try {
            return jdbcTemplate.queryForObject("SELECT nextval('" + sequenceName + "')", Long.class);
        } catch (Exception e) {
            try {
                jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS " + sequenceName + " START WITH 1 INCREMENT BY 1");
                return jdbcTemplate.queryForObject("SELECT nextval('" + sequenceName + "')", Long.class);
            } catch (Exception ex) {
                return System.currentTimeMillis() % 10000;
            }
        }
    }
}