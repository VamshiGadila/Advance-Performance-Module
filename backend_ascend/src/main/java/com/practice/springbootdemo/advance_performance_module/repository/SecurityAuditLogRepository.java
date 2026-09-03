package com.practice.springbootdemo.advance_performance_module.repository;

import com.practice.springbootdemo.advance_performance_module.entity.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, Long> {

    List<SecurityAuditLog> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    List<SecurityAuditLog> findTop50ByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE SecurityAuditLog s SET s.userId = null WHERE s.userId = :userId")
    void detachUser(@org.springframework.data.repository.query.Param("userId") Long userId);
}
