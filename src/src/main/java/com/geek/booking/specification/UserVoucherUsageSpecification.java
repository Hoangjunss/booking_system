package com.geek.booking.specification;

import com.geek.booking.entity.UserVoucherUsage;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserVoucherUsageSpecification {
    public static Specification<UserVoucherUsage> filterBy(Long userId, Long voucherId, Long bookingId, LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (voucherId != null) {
                predicates.add(cb.equal(root.get("voucher").get("id"), voucherId));
            }
            if (bookingId != null) {
                predicates.add(cb.equal(root.get("booking").get("id"), bookingId));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("usedAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("usedAt"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}