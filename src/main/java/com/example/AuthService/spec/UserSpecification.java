package com.example.AuthService.spec;

import com.example.AuthService.entity.User;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;


public class UserSpecification {

    public static Specification<User> filter(
            String keyword,
            Long roleId,
            Boolean enabled
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 🔍 keyword: name hoặc email
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), like),
                                cb.like(cb.lower(root.get("email")), like)
                        )
                );
            }

            // 🎭 lọc theo role
            if (roleId != null) {
                predicates.add(
                        cb.equal(root.get("role").get("id"), roleId)
                );
            }

            // ✅ lọc theo enabled
            if (enabled != null) {
                predicates.add(
                        cb.equal(root.get("enabled"), enabled)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

