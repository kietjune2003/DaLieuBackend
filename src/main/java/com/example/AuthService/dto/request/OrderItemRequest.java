package com.example.AuthService.dto.request;

import lombok.Data;

@Data
public class OrderItemRequest {
    private Long drugId;
    private Integer quantity;
}
