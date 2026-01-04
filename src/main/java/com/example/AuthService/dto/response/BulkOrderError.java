package com.example.AuthService.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BulkOrderError {
    private Long orderId;
    private String reason;
}
