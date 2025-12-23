package com.example.AuthService.service.impl;

import com.example.AuthService.dto.request.CreateOrderRequest;
import com.example.AuthService.dto.request.OrderItemRequest;
import com.example.AuthService.dto.response.OrderItemResponse;
import com.example.AuthService.dto.response.OrderResponse;
import com.example.AuthService.dto.response.PageResponse;
import com.example.AuthService.entity.*;
import com.example.AuthService.enums.OrderStatus;
import com.example.AuthService.enums.PaymentMethod;
import com.example.AuthService.repository.DrugRepository;
import com.example.AuthService.repository.OrderRepository;
import com.example.AuthService.repository.PaymentRepository;
import com.example.AuthService.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

        boolean isAdminOrMod =
                user.getRole().getName().equalsIgnoreCase("ADMIN") ||
                        user.getRole().getName().equalsIgnoreCase("MODERATOR");

        if (!isAdminOrMod) {
            throw new RuntimeException("Chỉ ADMIN hoặc MODERATOR mới có quyền cập nhật trạng thái");
        }

        switch (newStatus) {

            case SHIPPED:
                if (
                        (current == OrderStatus.PENDING && method == PaymentMethod.COD) ||
                                (current == OrderStatus.PAID && method == PaymentMethod.VNPAY)
                ) {

                    // CHỈ COD MỚI TRỪ KHO KHI SHIPPED
                    if (method == PaymentMethod.COD) {
                        for (OrderItem item : order.getItems()) {
                            Drug drug = item.getDrug();

                            if (drug.getStockQuantity() < item.getQuantity()) {
                                throw new RuntimeException("Thuốc hết hàng: " + drug.getName());
                            }

                            drug.setStockQuantity(
                                    drug.getStockQuantity() - item.getQuantity()
                            );
                            drugRepository.save(drug);
                        }
                    }

                    order.setStatus(OrderStatus.SHIPPED);

                } else {
                    throw new RuntimeException("Không thể chuyển sang SHIPPED");
                }
                break;

            case COMPLETED:
                if (current == OrderStatus.SHIPPED) {
                    order.setStatus(OrderStatus.COMPLETED);
                } else {
                    throw new RuntimeException("Chỉ đơn SHIPPED mới hoàn tất");
                }
                break;

            default:
                throw new RuntimeException("Không cho phép set trạng thái này");
        }

        orderRepository.save(order);
    }

    @Transactional
    @Override
    public void cancelOrder(Long orderId, User user) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

        OrderStatus currentStatus = order.getStatus();
        PaymentMethod method = order.getPaymentMethod();

        boolean isAdminOrMod =
                user.getRole().getName().equalsIgnoreCase("ADMIN") ||
                        user.getRole().getName().equalsIgnoreCase("MODERATOR");

        boolean isOwner = order.getUser().getId().equals(user.getId());

        if (currentStatus == OrderStatus.COMPLETED) {
            throw new RuntimeException("Đơn đã hoàn thành, không thể huỷ");
        }

        // USER
        if (!isAdminOrMod) {
            if (!isOwner) {
                throw new RuntimeException("Không có quyền huỷ đơn này");
            }
            if (currentStatus != OrderStatus.PENDING) {
                throw new RuntimeException("Người dùng chỉ được huỷ đơn khi PENDING");
            }
        }

        // 🔁 HOÀN KHO KHI ĐƠN ĐÃ TỪNG TRỪ KHO
        boolean needRestoreStock =
                (currentStatus == OrderStatus.SHIPPED && method == PaymentMethod.COD) ||
                        (currentStatus == OrderStatus.PAID && method == PaymentMethod.VNPAY) ||
                        (currentStatus == OrderStatus.SHIPPED && method == PaymentMethod.VNPAY);

        if (isAdminOrMod && needRestoreStock) {
            for (OrderItem item : order.getItems()) {
                Drug drug = item.getDrug();
                drug.setStockQuantity(
                        drug.getStockQuantity() + item.getQuantity()
                );
                drugRepository.save(drug);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }


    @Override
    public PageResponse<OrderResponse> getOrders(
            User user,
            OrderStatus status,
            PaymentMethod paymentMethod,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Long userId,
            Pageable pageable
    ) {

        boolean isAdminOrMod =
                user.getRole().getName().equalsIgnoreCase("ADMIN") ||
                        user.getRole().getName().equalsIgnoreCase("MODERATOR");

        Specification<Order> spec = Specification.allOf();

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status));
        }

        if (paymentMethod != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("paymentMethod"), paymentMethod));
        }

        if (fromDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
        }

        if (toDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
        }


        // 🔐 USER chỉ xem đơn của mình
        if (!isAdminOrMod) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("user").get("id"), user.getId()));
        }

        // 👮 ADMIN / MOD lọc theo userId
        if (isAdminOrMod && userId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("user").get("id"), userId));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("status"), status));
        }

        if (paymentMethod != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("paymentMethod"), paymentMethod));
        }

        if (fromDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
        }

        if (toDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
        }

        Page<Order> page = orderRepository.findAll(spec, pageable);

        return PageResponse.<OrderResponse>builder()
                .content(page.getContent().stream()
                        .map(this::mapToResponse)
                        .toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private OrderResponse mapToResponse(Order order) {

        return OrderResponse.builder()
                .orderId(order.getId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .createdAt(order.getCreatedAt())
                .shippingAddress(order.getShippingAddress())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .userId(order.getUser().getId())
                .userEmail(order.getUser().getEmail())
                .items(
                        order.getItems().stream()
                                .map(item -> OrderItemResponse.builder()
                                        .drugId(item.getDrug().getId())
                                        .drugName(item.getDrug().getName())
                                        .quantity(item.getQuantity())
                                        .unitPrice(item.getUnitPrice())
                                        .totalPrice(item.getTotalPrice())
                                        .build())
                                .toList()
                )
                .build();
    }


}
