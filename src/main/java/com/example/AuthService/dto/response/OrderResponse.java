package com.example.AuthService.dto.response;

import com.example.AuthService.enums.OrderStatus;
import com.example.AuthService.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private Long orderId;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private LocalDateTime createdAt;

    private String shippingAddress;
    private String receiverName;
    private String receiverPhone;

    private Long userId;
    private String userEmail;

    private List<OrderItemResponse> items;
}

