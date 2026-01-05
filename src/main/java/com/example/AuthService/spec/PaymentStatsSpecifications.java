package com.example.AuthService.spec;

import com.example.AuthService.dto.stats.RevenueStatsFilter;
import com.example.AuthService.entity.Payment;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class PaymentStatsSpecifications {

    public static Specification<Payment> withFilter(RevenueStatsFilter f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            // Payment table filter thường theo paidAt nếu có, fallback createdAt
            if (f.getFrom() != null) {
                ps.add(cb.or(
                        cb.greaterThanOrEqualTo(root.get("paidAt"), f.getFrom()),
                        cb.and(
                                cb.isNull(root.get("paidAt")),
                                cb.greaterThanOrEqualTo(root.get("createdAt"), f.getFrom())
                        )
                ));
            }
            if (f.getTo() != null) {
                ps.add(cb.or(
                        cb.lessThan(root.get("paidAt"), f.getTo()),
                        cb.and(
                                cb.isNull(root.get("paidAt")),
                                cb.lessThan(root.get("createdAt"), f.getTo())
                        )
                ));
            }

            if (f.getPaymentStatuses() != null && !f.getPaymentStatuses().isEmpty()) {
                ps.add(root.get("status").in(f.getPaymentStatuses()));
            }
            if (f.getPaymentMethods() != null && !f.getPaymentMethods().isEmpty()) {
                ps.add(root.get("method").in(f.getPaymentMethods()));
            }
            if (f.getUserId() != null) {
                ps.add(cb.equal(root.get("order").get("user").get("id"), f.getUserId()));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
