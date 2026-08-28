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
                String rawSearch = criteria.getSearch().trim();
                String term = "%" + rawSearch.toLowerCase() + "%";

                List<Predicate> orPredicates = new ArrayList<>();
                orPredicates.add(cb.like(cb.lower(root.get("employeeCode")), term));
                orPredicates.add(cb.like(cb.lower(root.get("name")), term));
                orPredicates.add(cb.like(cb.lower(root.get("email")), term));
                orPredicates.add(cb.like(cb.lower(cb.coalesce(root.get("skill"), "")), term));
                orPredicates.add(cb.like(cb.lower(cb.coalesce(root.get("domain"), "")), term));
                orPredicates.add(cb.like(cb.lower(cb.coalesce(root.get("location"), "")), term));

                String numericOnly = rawSearch.replaceAll("[^0-9]", "");
                if (!numericOnly.isEmpty()) {
                    try {
                        Long parsedId = Long.parseLong(numericOnly);
                        orPredicates.add(cb.equal(root.get("id"), parsedId));
                    } catch (NumberFormatException ignored) {
                    }
                }

                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
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

            if (criteria.getRole() != null) {
                predicates.add(cb.equal(root.get("role"), criteria.getRole()));
            }

            if (criteria.getSkill() != null && !criteria.getSkill().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("skill")), "%" + criteria.getSkill().trim().toLowerCase() + "%"));
            }

            if (criteria.getLocation() != null && !criteria.getLocation().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + criteria.getLocation().trim().toLowerCase() + "%"));
            }

            if (criteria.getDomain() != null && !criteria.getDomain().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("domain")), "%" + criteria.getDomain().trim().toLowerCase() + "%"));
            }

            if (criteria.getDepartmentId() != null) {
                predicates.add(cb.equal(root.get("departmentId"), criteria.getDepartmentId()));
            }

            if (criteria.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), criteria.getActive()));
            }

            if (criteria.getMinExperience() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("experienceYears"), criteria.getMinExperience()));
            }
            if (criteria.getMaxExperience() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("experienceYears"), criteria.getMaxExperience()));
            }

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