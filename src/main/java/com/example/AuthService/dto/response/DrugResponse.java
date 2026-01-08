package com.example.AuthService.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DrugResponse {
    private Long id;
    private String name;
    private String title;
    private String image;
    private BigDecimal price;
    private BigDecimal importPrice;
    private Boolean isActive;
    private Integer soldQuantity;
    private Integer stockQuantity;
}
