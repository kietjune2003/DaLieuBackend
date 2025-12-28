package com.example.AuthService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponse {
    private Long drugId;
    private String drugName;
    private Integer quantity;
    private Double unitPrice;
    private Double totalPrice;
    private String imgUrl;
}

