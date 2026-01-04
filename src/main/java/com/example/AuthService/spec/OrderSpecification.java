package com.example.AuthService.spec;

import com.example.AuthService.entity.Order;
import com.example.AuthService.enums.OrderStatus;
import com.example.AuthService.enums.PaymentMethod;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    public static Specification<Order> filter(
            OrderStatus status,
            PaymentMethod paymentMethod,
            Long userId,
            String keyword,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (paymentMethod != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
            }

            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("receiverName")), like),
                                cb.like(cb.lower(root.get("userEmail")), like),
                                cb.like(cb.lower(root.get("receiverPhone")), like)
                        )
                );
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), fromDate));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
