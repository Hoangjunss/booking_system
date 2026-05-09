package com.geek.booking.specification;

import com.geek.booking.entity.TicketCategory;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TicketCategorySpecification {
    public static Specification<TicketCategory> filterBy(String name, Long concertId, BigDecimal minPrice, BigDecimal maxPrice, Integer minAvailable) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (concertId != null) {
                predicates.add(cb.equal(root.get("concert").get("id"), concertId));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (minAvailable != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("availableQuantity"), minAvailable));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}