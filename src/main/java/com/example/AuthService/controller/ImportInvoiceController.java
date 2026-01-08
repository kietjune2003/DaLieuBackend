package com.example.AuthService.controller;

import com.example.AuthService.dto.request.ImportInvoiceRequest;
import com.example.AuthService.dto.response.ImportInvoiceResponse;
import com.example.AuthService.entity.ImportInvoice;
import com.example.AuthService.service.ImportInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/import-invoices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ImportInvoiceController {

    private final ImportInvoiceService importInvoiceService;
    @GetMapping
    public ResponseEntity<Page<ImportInvoiceResponse>> getAll(
            @RequestParam(required = false) String q,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                importInvoiceService.getAll(q, pageable)
        );
    }
    @PostMapping("/import-excel")
    public ResponseEntity<ImportInvoiceResponse> importFromExcel(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                importInvoiceService.importFromExcel(file)
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<ImportInvoiceResponse> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                importInvoiceService.getById(id)
        );
    }

    @PostMapping
    public ResponseEntity<ImportInvoiceResponse> create(
            @RequestBody ImportInvoiceRequest request
    ) {
        return ResponseEntity.ok(
                importInvoiceService.create(request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        importInvoiceService.delete(id);
        return ResponseEntity.ok("Deleted");
    }
}
