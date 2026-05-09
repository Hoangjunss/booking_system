package com.geek.booking.specification;

import com.geek.booking.entity.Voucher;
import com.geek.booking.enums.DiscountType;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VoucherSpecification {
    public static Specification<Voucher> filterBy(String code, DiscountType discountType, Boolean isValid, LocalDateTime validFrom, LocalDateTime validTo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (code != null && !code.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%"));
            }
            if (discountType != null) {
                predicates.add(cb.equal(root.get("discountType"), discountType));
            }
            if (isValid != null && isValid) {
                predicates.add(cb.lessThanOrEqualTo(root.get("validFrom"), LocalDateTime.now()));
                predicates.add(cb.greaterThanOrEqualTo(root.get("validTo"), LocalDateTime.now()));
                predicates.add(cb.lessThan(root.get("usedCount"), root.get("usageLimit")));
            }
            if (validFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("validFrom"), validFrom));
            }
            if (validTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("validTo"), validTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}