package com.practice.springbootdemo.advance_performance_module.repository;

import com.practice.springbootdemo.advance_performance_module.entity.Goal;
import com.practice.springbootdemo.advance_performance_module.entity.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long>, JpaSpecificationExecutor<Goal> {
    List<Goal> findByEmployeeIdAndCycleId(Long employeeId, Long cycleId);
    List<Goal> findByManagerIdAndCycleId(Long managerId, Long cycleId);
    List<Goal> findByManagerId(Long managerId);
    List<Goal> findByEmployeeId(Long employeeId);
    List<Goal> findByEmployeeIdAndStatus(Long employeeId, GoalStatus status);
    List<Goal> findByCycleId(Long cycleId);
    long countByCycleId(Long cycleId);
    long countByCycleIdAndStatus(Long cycleId, GoalStatus status);
    long countByManagerIdAndCycleId(Long managerId, Long cycleId);
    long countByManagerIdAndCycleIdAndStatus(Long managerId, Long cycleId, GoalStatus status);
    long countByEmployeeIdAndCycleId(Long employeeId, Long cycleId);
    long countByEmployeeIdAndCycleIdAndStatus(Long employeeId, Long cycleId, GoalStatus status);
    @Query("SELECT COALESCE(SUM(g.weight), 0) FROM Goal g WHERE g.employeeId = :employeeId AND g.cycleId = :cycleId AND (:excludeGoalId IS NULL OR g.id != :excludeGoalId)")
    BigDecimal totalWeightExcluding(@Param("employeeId") Long employeeId, @Param("cycleId") Long cycleId, @Param("excludeGoalId") Long excludeGoalId);
    @Query("SELECT COALESCE(SUM(g.weight), 0) FROM Goal g WHERE g.employeeId = :employeeId AND g.cycleId = :cycleId")
    BigDecimal totalWeight(@Param("employeeId") Long employeeId, @Param("cycleId") Long cycleId);
    void deleteByEmployeeId(Long employeeId);
    void deleteByManagerId(Long managerId);
}