package com.practice.springbootdemo.advance_performance_module.specification;

import com.practice.springbootdemo.advance_performance_module.dto.search.CycleSearchCriteria;
import com.practice.springbootdemo.advance_performance_module.entity.PerformanceCycle;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CycleSpecification {

    public static Specification<PerformanceCycle> filterBy(CycleSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getName() != null && !criteria.getName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + criteria.getName().trim().toLowerCase() + "%"));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            if (criteria.getStartDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), criteria.getStartDateFrom()));
            }
            if (criteria.getEndDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), criteria.getEndDateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
