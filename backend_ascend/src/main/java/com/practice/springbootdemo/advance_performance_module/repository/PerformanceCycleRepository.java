package com.practice.springbootdemo.advance_performance_module.repository;

import com.practice.springbootdemo.advance_performance_module.entity.CycleStatus;
import com.practice.springbootdemo.advance_performance_module.entity.PerformanceCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerformanceCycleRepository extends JpaRepository<PerformanceCycle, Long>, JpaSpecificationExecutor<PerformanceCycle> {
    Optional<PerformanceCycle> findByNameIgnoreCase(String name);
    Optional<PerformanceCycle> findFirstByStatusOrderByStartDateDesc(CycleStatus status);
    boolean existsByNameIgnoreCase(String name);
    List<PerformanceCycle> findByStatus(CycleStatus status);
}