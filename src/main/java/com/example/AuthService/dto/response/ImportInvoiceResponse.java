package com.example.AuthService.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ImportInvoiceResponse {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private List<ImportInvoiceDetailResponse> details;
}
