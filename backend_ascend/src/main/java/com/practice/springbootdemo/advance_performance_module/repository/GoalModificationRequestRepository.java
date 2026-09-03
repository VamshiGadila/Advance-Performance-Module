package com.practice.springbootdemo.advance_performance_module.repository;

import com.practice.springbootdemo.advance_performance_module.entity.ModificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import com.practice.springbootdemo.advance_performance_module.entity.GoalModificationRequest;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoalModificationRequestRepository extends JpaRepository<GoalModificationRequest, Long>, JpaSpecificationExecutor<GoalModificationRequest> {
    List<GoalModificationRequest> findByGoalId(Long goalId);
    List<GoalModificationRequest> findByEmployeeId(Long employeeId);
    List<GoalModificationRequest> findByManagerId(Long managerId);
    List<GoalModificationRequest> findByManagerIdAndStatus(Long managerId, ModificationStatus status);
    Optional<GoalModificationRequest> findFirstByGoalIdAndStatusOrderByRequestedAtDesc(Long goalId, ModificationStatus status);
    boolean existsByGoalIdAndStatus(Long goalId, ModificationStatus status);
    void deleteByEmployeeId(Long employeeId);
    void deleteByManagerId(Long managerId);
}