package com.example.AuthService.repository;

import com.example.AuthService.entity.ImportInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportInvoiceRepository
        extends JpaRepository<ImportInvoice, Long> {
    Page<ImportInvoice> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );
}
