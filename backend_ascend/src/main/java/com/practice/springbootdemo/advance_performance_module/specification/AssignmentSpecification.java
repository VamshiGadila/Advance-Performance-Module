package com.practice.springbootdemo.advance_performance_module.specification;

import com.practice.springbootdemo.advance_performance_module.dto.search.AssignmentSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.entity.ManagerAssignment;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AssignmentSpecification {

    public static Specification<ManagerAssignment> filterBy(AssignmentSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getManagerId() != null) {
                predicates.add(cb.equal(root.get("managerId"), criteria.getManagerId()));
            }
            if (criteria.getEmployeeId() != null) {
                predicates.add(cb.equal(root.get("employeeId"), criteria.getEmployeeId()));
            }
            if (criteria.getCycleId() != null) {
                predicates.add(cb.equal(root.get("performanceCycleId"), criteria.getCycleId()));
            }
            if (criteria.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), criteria.getActive()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
