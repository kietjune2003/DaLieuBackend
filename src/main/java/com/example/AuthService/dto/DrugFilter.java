package com.example.AuthService.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DrugFilter {
    private String q;             // tìm name/title chứa q
    private BigDecimal minPrice;  // giá tối thiểu
    private BigDecimal maxPrice;  // giá tối đa
    private Boolean inStock;      // true => stockQuantity > 0; false => == 0
    private Boolean hasImage;
    private Boolean isActive;// true => image not null && not blank
}
