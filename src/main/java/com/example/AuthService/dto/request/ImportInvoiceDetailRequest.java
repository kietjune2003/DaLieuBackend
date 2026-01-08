package com.example.AuthService.dto.request;

import lombok.Data;

@Data
public class ImportInvoiceDetailRequest {

    private Integer quantity;
    private String drugName;
}
