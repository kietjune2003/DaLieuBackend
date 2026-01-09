package com.example.AuthService.service.impl;

import com.example.AuthService.dto.request.CreateOrderRequest;
import com.example.AuthService.dto.request.OrderItemRequest;
import com.example.AuthService.dto.response.*;
import com.example.AuthService.entity.*;
import com.example.AuthService.enums.OrderStatus;
import com.example.AuthService.enums.PaymentMethod;
import com.example.AuthService.enums.PaymentStatus;
import com.example.AuthService.repository.DrugRepository;
import com.example.AuthService.repository.OrderRepository;
import com.example.AuthService.repository.PaymentRepository;
import com.example.AuthService.service.InventoryService;
import com.example.AuthService.service.OrderService;
import com.example.AuthService.service.PaymentService;
import com.example.AuthService.spec.OrderSpecification;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final DrugRepository drugRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    @Override
    @Transactional
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


            Integer available = inventoryService.calculateStock(drug.getId()) - drug.getReservedQuantity();

            if (available < itemReq.getQuantity()) {
                throw new RuntimeException(
                        "Không đủ tồn kho cho thuốc: " + drug.getName()
                );
            }


            drug.setReservedQuantity(drug.getReservedQuantity() + itemReq.getQuantity());

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
    @Transactional
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

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new RuntimeException("Đơn hàng không có sản phẩm");
        }

        if (order.getPaymentMethod() != null) {
            throw new RuntimeException("Đơn hàng đã chọn phương thức thanh toán");
        }

        order.setPaymentMethod(PaymentMethod.COD);

        return orderRepository.save(order);
    }

//    @Transactional
//    @Override
//    public void updateOrderStatus(Long orderId, OrderStatus newStatus, User user) {
//
//        Order order = orderRepository.findById(orderId)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));
//
//        OrderStatus current = order.getStatus();
//
//        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
//        PaymentMethod method = payment != null ? payment.getMethod() : PaymentMethod.COD;
//
//        boolean isAdminOrMod =
//                user.getRole().getName().equalsIgnoreCase("ADMIN") ||
//                        user.getRole().getName().equalsIgnoreCase("MODERATOR");
//
//        if (!isAdminOrMod) {
//            throw new RuntimeException("Chỉ ADMIN hoặc MODERATOR mới có quyền cập nhật trạng thái");
//        }
//
//        switch (newStatus) {
//
//            case SHIPPED:
//                if (
//                        (current == OrderStatus.PENDING && method == PaymentMethod.COD) ||
//                                (current == OrderStatus.PAID && method == PaymentMethod.VNPAY)
//                ) {
//
//
//                    if (method == PaymentMethod.COD) {
//                        for (OrderItem item : order.getItems()) {
//                            Drug drug = item.getDrug();
//
//                            if (drug.getStockQuantity() < item.getQuantity()) {
//                                throw new RuntimeException("Thuốc hết hàng: " + drug.getName());
//                            }
//
//                            drug.setStockQuantity(
//                                    drug.getStockQuantity() - item.getQuantity()
//                            );
//                            drugRepository.save(drug);
//                        }
//                    }
//
//                    order.setStatus(OrderStatus.SHIPPED);
//
//                } else {
//                    throw new RuntimeException("Không thể chuyển sang SHIPPED");
//                }
//                break;
//
//            case COMPLETED:
//                if (current == OrderStatus.SHIPPED) {
//                    order.setStatus(OrderStatus.COMPLETED);
//                    for (OrderItem item : order.getItems()) {
//                        Drug drug = item.getDrug();
//
//
//                        drug.setSoldQuantity(
//                                drug.getSoldQuantity() - item.getQuantity()
//                        );
//                        drugRepository.save(drug);
//                    }
//                } else {
//                    throw new RuntimeException("Chỉ đơn SHIPPED mới hoàn tất");
//                }
//                break;
//
//            default:
//                throw new RuntimeException("Không cho phép set trạng thái này");
//        }
//
//        orderRepository.save(order);
//    }

    @Transactional
    @Override
    public void cancelOrder(Long orderId, User user) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Không có quyền huỷ đơn này");
        }

        OrderStatus status = order.getStatus();

        if (status == OrderStatus.PENDING) {

            for (OrderItem item : order.getItems()) {
                Drug drug = item.getDrug();
                int qty = item.getQuantity();
                drug.setReservedQuantity(drug.getReservedQuantity() - qty);
            }

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            return;
        }

        if (status == OrderStatus.PAID
                && order.getPaymentMethod() == PaymentMethod.VNPAY) {

            order.setStatus(OrderStatus.CANCEL_REQUESTED);

            Payment payment = paymentRepository.findByOrder(order)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy payment"));

            payment.setStatus(PaymentStatus.REFUND_PENDING);

            orderRepository.save(order);
            paymentRepository.save(payment);
            return;
        }

        throw new RuntimeException("Không thể huỷ đơn ở trạng thái hiện tại");
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
                                        .imgUrl(item.getDrug().getImage())
                                        .build())
                                .toList()
                )
                .build();
    }

    @Override
    public OrderResponse getOrderById(User user, Long orderId) throws AccessDeniedException {

        boolean isAdminOrMod =
                user.getRole().getName().equalsIgnoreCase("ADMIN") ||
                        user.getRole().getName().equalsIgnoreCase("MODERATOR");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // 🔐 USER chỉ được xem đơn của chính mình
        if (!isAdminOrMod) {
            if (!order.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("Bạn không có quyền xem đơn hàng này");
            }
        }

        return mapToResponse(order);
    }

    @Transactional
    @Override
    public void approveRefund(Long orderId, User admin) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

        if (order.getStatus() != OrderStatus.CANCEL_REQUESTED) {
            throw new RuntimeException("Order không ở trạng thái yêu cầu hoàn");
        }

        if (order.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw new RuntimeException("Chỉ hỗ trợ hoàn tiền cho VNPAY");
        }

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment"));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new RuntimeException("Đơn hàng đã được hoàn tiền");
        }

        if (payment.getStatus() != PaymentStatus.REFUND_PENDING) {
            throw new RuntimeException("Payment không hợp lệ");
        }

        boolean refundSuccess = paymentService.callVnPayRefund(payment, admin);

        if (!refundSuccess) {
            payment.setStatus(PaymentStatus.REFUND_FAILED);
            throw new RuntimeException("Hoàn tiền thất bại");
        }

        for (OrderItem item : order.getItems()) {
            Drug drug = item.getDrug();
            int qty = item.getQuantity();


            drug.setSoldQuantity(drug.getSoldQuantity() - qty);
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.REFUNDED);
    }

    @Transactional
    @Override
    public void rejectRefund(Long orderId, User admin) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

        if (order.getStatus() != OrderStatus.CANCEL_REQUESTED) {
            throw new RuntimeException("Order không ở trạng thái yêu cầu hoàn");
        }

        if (order.getPaymentMethod() != PaymentMethod.VNPAY) {
            throw new RuntimeException("Đơn hàng không hỗ trợ hoàn tiền online");
        }

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment"));

        if (payment.getStatus() != PaymentStatus.REFUND_PENDING) {
            throw new RuntimeException("Payment không ở trạng thái chờ hoàn");
        }

        order.setStatus(OrderStatus.PAID);
        payment.setStatus(PaymentStatus.REFUND_FAILED);
    }


    @Override
    public Page<OrderResponse> getOrdersForAdmin(
            OrderStatus status,
            PaymentMethod paymentMethod,
            Long userId,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate,
            String sortBy,
            String sortDir,
            int page,
            int size
    ) {

        // ===== ALLOWED SORT FIELDS =====
        Set<String> allowedSortFields = Set.of(
                "id",
                "createdAt",
                "totalAmount",
                "status"
        );

        String sortField = (sortBy == null || sortBy.isBlank())
                ? "createdAt"
                : sortBy;

        if (!allowedSortFields.contains(sortField)) {
            sortField = "createdAt";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortField)
        );

        LocalDateTime fromDateTime = fromDate != null
                ? fromDate.atStartOfDay()
                : null;

        LocalDateTime toDateTime = toDate != null
                ? toDate.atTime(LocalTime.MAX)
                : null;

        Specification<Order> spec = OrderSpecification.filter(
                status,
                paymentMethod,
                userId,
                keyword,
                fromDateTime,
                toDateTime
        );

        return orderRepository
                .findAll(spec, pageable)
                .map(this::mapToResponse);
    }


    @Override
    public OrderResponse getOrderDetailForAdmin(Long orderId) {

        Order order = orderRepository.findDetailById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        return mapToResponse(order);
    }

    @Transactional
    @Override
    public BulkOrderActionResult bulkShipOrders(List<Long> orderIds) {

        List<Long> successIds = new ArrayList<>();
        List<BulkOrderError> errors = new ArrayList<>();

        for (Long orderId : orderIds) {
            try {
                Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new RuntimeException("Không tồn tại"));

                if (order.getStatus() != OrderStatus.PENDING &&
                        order.getStatus() != OrderStatus.PAID) {
                    throw new RuntimeException("Không thể ship ở trạng thái " + order.getStatus());
                }


                order.setStatus(OrderStatus.SHIPPED);
                successIds.add(orderId);

            } catch (Exception e) {
                errors.add(BulkOrderError.builder()
                        .orderId(orderId)
                        .reason(e.getMessage())
                        .build());
            }
        }

        return BulkOrderActionResult.builder()
                .total(orderIds.size())
                .success(successIds.size())
                .failed(errors.size())
                .successIds(successIds)
                .errors(errors)
                .build();
    }

    @Transactional
    @Override
    public BulkOrderActionResult bulkCompleteOrders(List<Long> orderIds) {

        List<Long> successIds = new ArrayList<>();
        List<BulkOrderError> errors = new ArrayList<>();

        for (Long orderId : orderIds) {
            try {
                Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new RuntimeException("Không tồn tại"));

                if (order.getStatus() != OrderStatus.SHIPPED) {
                    throw new RuntimeException("Chưa được ship");
                }


                if (order.getPaymentMethod() == PaymentMethod.COD) {

                    for (OrderItem item : order.getItems()) {
                        Drug drug = item.getDrug();
                        int qty = item.getQuantity();

                        if (drug.getReservedQuantity() < qty) {
                            throw new RuntimeException(
                                    "Kho không hợp lệ cho thuốc: " + drug.getName()
                            );
                        }

                        drug.setReservedQuantity(
                                drug.getReservedQuantity() - qty
                        );
                        drug.setSoldQuantity(
                                drug.getSoldQuantity() + qty
                        );
                    }
                }

                order.setStatus(OrderStatus.COMPLETED);
                successIds.add(orderId);

            } catch (Exception e) {
                errors.add(BulkOrderError.builder()
                        .orderId(orderId)
                        .reason(e.getMessage())
                        .build());
            }
        }

        return BulkOrderActionResult.builder()
                .total(orderIds.size())
                .success(successIds.size())
                .failed(errors.size())
                .successIds(successIds)
                .errors(errors)
                .build();
    }

    @Transactional
    public void refundPaidOrder(Order order, User admin) {

        Payment payment = paymentRepository.findByOrder(order)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment"));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new RuntimeException("Đơn hàng đã được hoàn tiền");
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new RuntimeException("Payment không ở trạng thái có thể hoàn");
        }


        boolean refundSuccess =
                paymentService.callVnPayRefund(payment, admin);

        if (!refundSuccess) {
            payment.setStatus(PaymentStatus.REFUND_FAILED);
            throw new RuntimeException("Hoàn tiền thất bại");
        }


        for (OrderItem item : order.getItems()) {
            Drug drug = item.getDrug();
            int qty = item.getQuantity();

            drug.setSoldQuantity(
                    drug.getSoldQuantity() - qty
            );

        }


        payment.setStatus(PaymentStatus.REFUNDED);
    }



    @Override
    @Transactional
    public BulkOrderActionResult bulkCancelOrders(
            List<Long> orderIds,
            User admin
    ) {

        List<Long> successIds = new ArrayList<>();
        List<BulkOrderError> errors = new ArrayList<>();

        for (Long orderId : orderIds) {
            try {
                Order order = orderRepository.findById(orderId)
                        .orElseThrow(() -> new RuntimeException("Không tồn tại order"));


                if (order.getStatus() == OrderStatus.PENDING) {

                    for (OrderItem item : order.getItems()) {
                        Drug drug = item.getDrug();
                        int qty = item.getQuantity();

                        if (drug.getReservedQuantity() < qty) {
                            throw new RuntimeException(
                                    "Kho không hợp lệ cho thuốc: " + drug.getName()
                            );
                        }

                        drug.setReservedQuantity(
                                drug.getReservedQuantity() - qty
                        );
                    }

                    order.setStatus(OrderStatus.CANCELLED);
                    successIds.add(orderId);
                    continue;
                }


                if (order.getStatus() == OrderStatus.PAID
                        && order.getPaymentMethod() == PaymentMethod.VNPAY) {

                    refundPaidOrder(order, admin);

                    order.setStatus(OrderStatus.REFUNDED);
                    successIds.add(orderId);
                    continue;
                }

                if (order.getStatus() == OrderStatus.SHIPPED
                        && order.getPaymentMethod() == PaymentMethod.VNPAY) {

                    refundPaidOrder(order, admin);

                    order.setStatus(OrderStatus.REFUNDED);
                    successIds.add(orderId);
                    continue;
                }


                throw new RuntimeException(
                        "Không thể huỷ đơn ở trạng thái " + order.getStatus()
                );

            } catch (Exception e) {
                errors.add(BulkOrderError.builder()
                        .orderId(orderId)
                        .reason(e.getMessage())
                        .build());
            }
        }

        return BulkOrderActionResult.builder()
                .total(orderIds.size())
                .success(successIds.size())
                .failed(errors.size())
                .successIds(successIds)
                .errors(errors)
                .build();
    }


    @Override
    @Transactional
    public BulkOrderActionResult bulkApproveRefund(
            List<Long> orderIds,
            User admin
    ) {

        List<Long> successIds = new ArrayList<>();
        List<BulkOrderError> errors = new ArrayList<>();

        for (Long orderId : orderIds) {
            try {
                approveRefund(orderId, admin);
                successIds.add(orderId);
            } catch (Exception e) {
                errors.add(BulkOrderError.builder()
                        .orderId(orderId)
                        .reason(e.getMessage())
                        .build());
            }
        }

        return BulkOrderActionResult.builder()
                .total(orderIds.size())
                .success(successIds.size())
                .failed(errors.size())
                .successIds(successIds)
                .errors(errors)
                .build();
    }
    @Override
    @Transactional
    public BulkOrderActionResult bulkRejectRefund(
            List<Long> orderIds,
            User admin
    ) {

        List<Long> successIds = new ArrayList<>();
        List<BulkOrderError> errors = new ArrayList<>();

        for (Long orderId : orderIds) {
            try {
                rejectRefund(orderId, admin);
                successIds.add(orderId);
            } catch (Exception e) {
                errors.add(BulkOrderError.builder()
                        .orderId(orderId)
                        .reason(e.getMessage())
                        .build());
            }
        }

        return BulkOrderActionResult.builder()
                .total(orderIds.size())
                .success(successIds.size())
                .failed(errors.size())
                .successIds(successIds)
                .errors(errors)
                .build();
    }


}
