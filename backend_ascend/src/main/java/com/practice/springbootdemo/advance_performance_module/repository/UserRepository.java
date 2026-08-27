package com.practice.springbootdemo.advance_performance_module.repository;


import com.practice.springbootdemo.advance_performance_module.entity.Role;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByEmployeeCode(String employeeCode);
    Optional<User> findByEmailIgnoreCaseOrEmployeeCodeIgnoreCase(String email, String employeeCode);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmployeeCode(String employeeCode);
    List<User> findAllByRoleAndActiveTrue(Role role);
    List<User> findByRoleAndActiveTrue(Role role);
    List<User> findByDepartmentIdAndActiveTrue(Long departmentId);
    List<User> findByDepartmentIdAndRoleAndActiveTrue(Long departmentId, Role role);
    List<User> findAllByDepartmentIdAndRoleAndActiveTrue(Long departmentId, Role role);
    List<User> findByIdIn(List<Long> ids);
}