package com.geek.booking.specification;



import com.geek.booking.entity.BookingItem;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BookingItemSpecification {
    public static Specification<BookingItem> filterBy(Long bookingId, Long categoryId, BigDecimal minUnitPrice, BigDecimal maxUnitPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (bookingId != null) {
                predicates.add(cb.equal(root.get("booking").get("id"), bookingId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("ticketCategory").get("id"), categoryId));
            }
            if (minUnitPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("unitPrice"), minUnitPrice));
            }
            if (maxUnitPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("unitPrice"), maxUnitPrice));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}