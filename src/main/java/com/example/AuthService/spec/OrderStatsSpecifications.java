package com.example.AuthService.spec;

import com.example.AuthService.dto.stats.RevenueStatsFilter;
import com.example.AuthService.entity.Order;
import com.example.AuthService.entity.OrderItem;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class OrderStatsSpecifications {

    public static Specification<Order> withFilter(RevenueStatsFilter f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (f.getFrom() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), f.getFrom()));
            }
            if (f.getTo() != null) {
                ps.add(cb.lessThan(root.get("createdAt"), f.getTo()));
            }
            if (f.getOrderStatuses() != null && !f.getOrderStatuses().isEmpty()) {
                ps.add(root.get("status").in(f.getOrderStatuses()));
            }
            if (f.getPaymentMethods() != null && !f.getPaymentMethods().isEmpty()) {
                ps.add(root.get("paymentMethod").in(f.getPaymentMethods()));
            }
            if (f.getUserId() != null) {
                ps.add(cb.equal(root.get("user").get("id"), f.getUserId()));
            }

            if (f.getDrugIds() != null && !f.getDrugIds().isEmpty()) {
                // join items -> drug
                query.distinct(true);
                Join<Order, OrderItem> items = root.join("items", JoinType.INNER);
                ps.add(items.get("drug").get("id").in(f.getDrugIds()));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
