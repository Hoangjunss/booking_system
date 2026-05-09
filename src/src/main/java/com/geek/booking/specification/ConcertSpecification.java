package com.geek.booking.specification;

import com.geek.booking.entity.Concert;
import com.geek.booking.enums.ConcertStatus;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConcertSpecification {
    public static Specification<Concert> filterBy(String name, String venue, ConcertStatus status, LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (venue != null && !venue.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("venue")), "%" + venue.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}