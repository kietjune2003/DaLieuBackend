package com.example.AuthService.dto.stats;

import com.example.AuthService.enums.PaymentMethod;
import com.example.AuthService.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentRowDto {
    private Long id;
    private Long orderId;
    private PaymentMethod method;
    private PaymentStatus status;
    private BigDecimal amount;
    private String vnpTxnRef;
    private String vnpTransactionNo;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}
