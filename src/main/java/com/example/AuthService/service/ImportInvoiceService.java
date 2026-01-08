package com.example.AuthService.service;

import com.example.AuthService.dto.request.ImportInvoiceRequest;
import com.example.AuthService.dto.response.ImportInvoiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ImportInvoiceService {

    Page<ImportInvoiceResponse> getAll(
            String keyword,
            Pageable pageable
    );


    ImportInvoiceResponse getById(Long id);

    ImportInvoiceResponse create(ImportInvoiceRequest request);
    ImportInvoiceResponse importFromExcel(MultipartFile file);

    void delete(Long id);
}
