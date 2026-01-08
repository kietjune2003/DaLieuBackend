package com.example.AuthService.repository;

import com.example.AuthService.entity.Drug;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DrugRepository extends JpaRepository<Drug, Long>, JpaSpecificationExecutor<Drug> {

    @Query("select d.name from Drug d " +
            "where lower(d.name) like lower(concat('%', :q, '%'))")
    List<String> suggestNames(@Param("q") String q, Pageable pageable);
    Optional<Drug> findByNameIgnoreCase(String name);
    // đã có đầy đủ findAll(spec, pageable) từ JpaSpecificationExecutor
}
