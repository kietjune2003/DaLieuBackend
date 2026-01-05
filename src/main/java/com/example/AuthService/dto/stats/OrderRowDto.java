package com.example.AuthService.dto.stats;

import com.example.AuthService.enums.OrderStatus;
import com.example.AuthService.enums.PaymentMethod;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderRowDto {
    private Long id;
    private LocalDateTime createdAt;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private BigDecimal totalAmount;
    private Long userId;
}
