package com.practice.springbootdemo.advance_performance_module.repository;

import com.practice.springbootdemo.advance_performance_module.entity.ResetAuthorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ResetAuthorizationRepository extends JpaRepository<ResetAuthorization, Long> {

    Optional<ResetAuthorization> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE ResetAuthorization r SET r.usedAt = :now WHERE r.id = :id AND r.usedAt IS NULL")
    int consumeAuthorization(@Param("id") Long id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE ResetAuthorization r SET r.usedAt = :now WHERE r.user.id = :userId AND r.usedAt IS NULL")
    int invalidateAllForUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM ResetAuthorization r WHERE r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
