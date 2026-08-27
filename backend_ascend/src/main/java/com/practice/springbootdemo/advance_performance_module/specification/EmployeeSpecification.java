package com.practice.springbootdemo.advance_performance_module.specification;

import com.practice.springbootdemo.advance_performance_module.dto.search.EmployeeSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.entity.ManagerAssignment;
import com.practice.springbootdemo.advance_performance_module.entity.User;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EmployeeSpecification {

    public static Specification<User> filterBy(EmployeeSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();


            if (criteria.getSearch() != null && !criteria.getSearch().isBlank()) {
                String term = "%" + criteria.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("employeeCode")), term),
                        cb.like(cb.lower(root.get("name")), term),
                        cb.like(cb.lower(root.get("email")), term),
                        cb.like(cb.lower(cb.coalesce(root.get("skill"), "")), term),
                        cb.like(cb.lower(cb.coalesce(root.get("domain"), "")), term),
                        cb.like(cb.lower(cb.coalesce(root.get("location"), "")), term)
                ));
            } else {

                if (criteria.getEmployeeCode() != null && !criteria.getEmployeeCode().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("employeeCode")), "%" + criteria.getEmployeeCode().trim().toLowerCase() + "%"));
                }
                if (criteria.getName() != null && !criteria.getName().isBlank()) {
                    String nameTerm = "%" + criteria.getName().trim().toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("name")), nameTerm),
                            cb.like(cb.lower(root.get("employeeCode")), nameTerm)
                    ));
                }
                if (criteria.getEmail() != null && !criteria.getEmail().isBlank()) {
                    predicates.add(cb.like(cb.lower(root.get("email")), "%" + criteria.getEmail().trim().toLowerCase() + "%"));
                }
            }

            // Role Filter
            if (criteria.getRole() != null) {
                predicates.add(cb.equal(root.get("role"), criteria.getRole()));
            }

            // Skill Filter
            if (criteria.getSkill() != null && !criteria.getSkill().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("skill")), "%" + criteria.getSkill().trim().toLowerCase() + "%"));
            }

            // Work Location Filter
            if (criteria.getLocation() != null && !criteria.getLocation().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + criteria.getLocation().trim().toLowerCase() + "%"));
            }

            // Domain Filter
            if (criteria.getDomain() != null && !criteria.getDomain().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("domain")), "%" + criteria.getDomain().trim().toLowerCase() + "%"));
            }

            // Department Filter
            if (criteria.getDepartmentId() != null) {
                predicates.add(cb.equal(root.get("departmentId"), criteria.getDepartmentId()));
            }

            // Active Status Filter
            if (criteria.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), criteria.getActive()));
            }

            // Experience Bounds
            if (criteria.getMinExperience() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("experienceYears"), criteria.getMinExperience()));
            }
            if (criteria.getMaxExperience() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("experienceYears"), criteria.getMaxExperience()));
            }

            // Manager Assigned Subquery
            if (criteria.getManagerId() != null && query != null) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<ManagerAssignment> assignmentRoot = subquery.from(ManagerAssignment.class);
                subquery.select(assignmentRoot.get("employeeId"))
                        .where(cb.equal(assignmentRoot.get("managerId"), criteria.getManagerId()), cb.isTrue(assignmentRoot.get("active")));
                predicates.add(root.get("id").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}