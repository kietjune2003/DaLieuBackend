package com.example.AuthService.service.impl;

import com.example.AuthService.dto.request.CreateOrderRequest;
import com.example.AuthService.dto.request.OrderItemRequest;
import com.example.AuthService.entity.*;
import com.example.AuthService.enums.OrderStatus;
import com.example.AuthService.enums.PaymentMethod;
import com.example.AuthService.repository.DrugRepository;
import com.example.AuthService.repository.OrderRepository;
import com.example.AuthService.repository.PaymentRepository;
import com.example.AuthService.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final DrugRepository drugRepository;
    private final PaymentRepository paymentRepository;
    @Override
    public Order createOrder(CreateOrderRequest request, User user) {

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.getItems()) {

            Drug drug = drugRepository.findById(itemReq.getDrugId())
                    .orElseThrow(() ->
                            new RuntimeException("Không tìm thấy thuốc ID: " + itemReq.getDrugId()));

            if (itemReq.getQuantity() <= 0) {
                throw new RuntimeException("Số lượng không hợp lệ");
            }

            if (drug.getStockQuantity() < itemReq.getQuantity()) {
                throw new RuntimeException("Không đủ tồn kho cho thuốc: " + drug.getName());
            }

            BigDecimal unitPrice = drug.getPrice();
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .drug(drug)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(unitPrice.doubleValue())
                    .totalPrice(itemTotal.doubleValue())
                    .build();

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(itemTotal);
        }

        Order order = Order.builder()
                .user(user)
                .shippingAddress(request.getShippingAddress())
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        return orderRepository.save(order);
    }
    @Override
    public Order confirmCodPayment(Long orderId, User user) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy order ID: " + orderId));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền với đơn hàng này");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Đơn hàng không ở trạng thái PENDING");
        }

        order.setPaymentMethod(PaymentMethod.COD);

        // KHÔNG đổi status
        // KHÔNG trừ kho
        // KHÔNG tạo Payment

        return orderRepository.save(order);
    }
    @Transactional
    @Override
    public void updateOrderStatus(Long orderId, OrderStatus newStatus, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

        OrderStatus current = order.getStatus();
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        PaymentMethod method = payment != null ? payment.getMethod() : PaymentMethod.COD;

        boolean isAdmin = user.getRole().getName().equalsIgnoreCase("ADMIN")
                || user.getRole().getName().equalsIgnoreCase("MODERATOR");

        switch (newStatus) {
            case CANCELLED:
                if (current == OrderStatus.PENDING && (isAdmin || order.getUser().getId().equals(user.getId()))) {
                    // User hoặc admin có quyền hủy đơn PENDING
                    order.setStatus(OrderStatus.CANCELLED);
                } else {
                    throw new RuntimeException("Chỉ đơn PENDING mới có thể hủy");
                }
                break;

            case SHIPPED:
                if (current == OrderStatus.PENDING && method == PaymentMethod.COD && isAdmin) {
                    order.setStatus(OrderStatus.SHIPPED);
                } else if (current == OrderStatus.PAID && method == PaymentMethod.VNPAY && isAdmin) {
                    order.setStatus(OrderStatus.SHIPPED);
                } else {
                    throw new RuntimeException("Chỉ đơn PENDING COD hoặc PAID VNPAY mới được giao");
                }
                break;

            case COMPLETED:
                if (current == OrderStatus.SHIPPED && isAdmin) {
                    order.setStatus(OrderStatus.COMPLETED);
                } else {
                    throw new RuntimeException("Chỉ đơn SHIPPED mới hoàn tất được");
                }
                break;

            default:
                throw new RuntimeException("Trạng thái không hợp lệ hoặc không thể set trực tiếp");
        }

        orderRepository.save(order);
    }


}
