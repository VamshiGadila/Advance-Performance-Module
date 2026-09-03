package com.practice.springbootdemo.advance_performance_module.repository;

import com.practice.springbootdemo.advance_performance_module.entity.VerificationCode;
import com.practice.springbootdemo.advance_performance_module.entity.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    @Query("SELECT v FROM VerificationCode v WHERE v.user.id = :userId AND v.purpose = :purpose " +
            "AND v.usedAt IS NULL AND v.invalidatedAt IS NULL AND v.expiresAt > :now " +
            "ORDER BY v.createdAt DESC")
    List<VerificationCode> findActiveCodes(
            @Param("userId") Long userId,
            @Param("purpose") VerificationPurpose purpose,
            @Param("now") LocalDateTime now
    );

    default Optional<VerificationCode> findLatestActiveCode(Long userId, VerificationPurpose purpose) {
        List<VerificationCode> list = findActiveCodes(userId, purpose, LocalDateTime.now());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Modifying
    @Query("UPDATE VerificationCode v SET v.invalidatedAt = :now " +
            "WHERE v.user.id = :userId AND v.purpose = :purpose " +
            "AND v.invalidatedAt IS NULL AND v.usedAt IS NULL")
    int invalidateAllActive(
            @Param("userId") Long userId,
            @Param("purpose") VerificationPurpose purpose,
            @Param("now") LocalDateTime now
    );

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE VerificationCode v SET v.attemptCount = v.attemptCount + 1 WHERE v.id = :id")
    int incrementAttempts(@Param("id") Long id);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE VerificationCode v SET v.invalidatedAt = :now WHERE v.id = :id AND v.invalidatedAt IS NULL")
    int invalidateById(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE VerificationCode v SET v.usedAt = :now " +
            "WHERE v.id = :id AND v.usedAt IS NULL AND v.invalidatedAt IS NULL")
    int consumeCode(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
