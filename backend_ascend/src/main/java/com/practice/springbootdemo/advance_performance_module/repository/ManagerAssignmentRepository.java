package com.practice.springbootdemo.advance_performance_module.repository;

import com.practice.springbootdemo.advance_performance_module.entity.ManagerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ManagerAssignmentRepository extends JpaRepository<ManagerAssignment, Long>, JpaSpecificationExecutor<ManagerAssignment> {
    Optional<ManagerAssignment> findByEmployeeIdAndActiveTrue(Long employeeId);
    List<ManagerAssignment> findByManagerIdAndActiveTrue(Long managerId);
    List<ManagerAssignment> findByActiveTrue();
    boolean existsByEmployeeIdAndManagerIdAndActiveTrue(Long employeeId, Long managerId);
    boolean existsByEmployeeIdAndManagerIdAndPerformanceCycleIdAndActiveTrue(Long employeeId, Long managerId, Long cycleId);
    @Query("SELECT ma.employeeId FROM ManagerAssignment ma WHERE ma.managerId = :managerId AND ma.active = true")
    List<Long> findAssignedEmployeeIds(@Param("managerId") Long managerId);
    Optional<ManagerAssignment> findByEmployeeIdAndPerformanceCycleIdAndActiveTrue(Long employeeId, Long cycleId);
    void deleteByEmployeeId(Long employeeId);
    void deleteByManagerId(Long managerId);
}
