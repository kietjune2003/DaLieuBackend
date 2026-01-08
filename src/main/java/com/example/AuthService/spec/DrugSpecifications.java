package com.example.AuthService.spec;

import com.example.AuthService.dto.DrugFilter;
import com.example.AuthService.entity.Drug;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class DrugSpecifications {

    public static Specification<Drug> withFilter(DrugFilter f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (f != null) {

                if (StringUtils.hasText(f.getQ())) {
                    String like = "%" + f.getQ().toLowerCase().trim() + "%";
                    ps.add(cb.like(cb.lower(root.get("name")), like));
                }

                if (f.getMinPrice() != null) {
                    ps.add(cb.greaterThanOrEqualTo(root.get("price"), f.getMinPrice()));
                }

                if (f.getMaxPrice() != null) {
                    ps.add(cb.lessThanOrEqualTo(root.get("price"), f.getMaxPrice()));
                }

                if (f.getInStock() != null) {
                    if (f.getInStock()) {
                        ps.add(cb.greaterThan(root.get("stockQuantity"), 0));
                    } else {
                        ps.add(cb.equal(root.get("stockQuantity"), 0));
                    }
                }

                if (f.getHasImage() != null) {
                    if (f.getHasImage()) {
                        ps.add(cb.and(
                                cb.isNotNull(root.get("image")),
                                cb.notEqual(cb.trim(root.get("image")), "")
                        ));
                    } else {
                        ps.add(cb.or(
                                cb.isNull(root.get("image")),
                                cb.equal(cb.trim(root.get("image")), "")
                        ));
                    }
                }


                if (f.getIsActive() != null) {
                    ps.add(cb.equal(root.get("isActive"), f.getIsActive()));
                }
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}

