package com.example.AuthService.dto.response;

import lombok.Data;

@Data
public class ImportInvoiceDetailResponse {
    private Long id;
    private Long drugId;
    private String drugName;
    private Integer quantity;
}
