package com.example.AuthService.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class ImportInvoiceRequest {
    private String name;
    private List<ImportInvoiceDetailRequest> details;
}
