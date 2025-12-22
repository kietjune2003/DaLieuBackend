package com.example.AuthService.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CreateOrderRequest {

    private String shippingAddress;
    private String receiverName;
    private String receiverPhone;

    private List<OrderItemRequest> items;
}
