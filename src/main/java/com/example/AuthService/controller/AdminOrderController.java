package com.example.AuthService.controller;

import com.example.AuthService.dto.request.BulkOrderActionRequest;
import com.example.AuthService.dto.response.BulkOrderActionResult;
import com.example.AuthService.dto.response.OrderResponse;
import com.example.AuthService.entity.User;
import com.example.AuthService.enums.OrderStatus;
import com.example.AuthService.enums.PaymentMethod;
import com.example.AuthService.repository.UserRepository;
import com.example.AuthService.service.OrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
public class AdminOrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    public AdminOrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }
    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                orderService.getOrdersForAdmin(
                        status, paymentMethod, userId, keyword,
                        fromDate, toDate,
                        sortBy, sortDir,
                        page, size
                )
        );
    }



//    @PutMapping("/{orderId}/status")
//    @PreAuthorize("isAuthenticated()") // user hoặc admin
//    public ResponseEntity<?> updateOrderStatus(
//            @PathVariable Long orderId,
//            @RequestParam OrderStatus status,
//            @AuthenticationPrincipal UserDetails userDetails) {
//
//        User user = userRepository.findByEmail(userDetails.getUsername())
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
//
//        orderService.updateOrderStatus(orderId, status, user);
//        return ResponseEntity.ok("Order cập nhật trạng thái: " + status);
//    }
    @PutMapping("/{orderId}/approve-refund")
    public ResponseEntity<?> approveRefundSingle(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));

        BulkOrderActionResult result =
                orderService.bulkApproveRefund(List.of(orderId), admin);

        if (result.getFailed() > 0) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
    @PutMapping("/bulk/approve-refund")
    public ResponseEntity<?> approveRefundBulk(
            @RequestBody BulkOrderActionRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));

        return ResponseEntity.ok(
                orderService.bulkApproveRefund(
                        request.getOrderIds(),
                        admin
                )
        );
    }
    @PutMapping("/{orderId}/reject-refund")
    public ResponseEntity<?> rejectRefundSingle(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));

        BulkOrderActionResult result =
                orderService.bulkRejectRefund(List.of(orderId), admin);

        if (result.getFailed() > 0) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
    @PutMapping("/bulk/reject-refund")
    public ResponseEntity<?> rejectRefundBulk(
            @RequestBody BulkOrderActionRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));

        return ResponseEntity.ok(
                orderService.bulkRejectRefund(
                        request.getOrderIds(),
                        admin
                )
        );
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderDetail(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(orderService.getOrderDetailForAdmin(orderId));
    }


    @PutMapping("/bulk/cancel")
    public ResponseEntity<?> bulkCancel(
            @RequestBody BulkOrderActionRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));

        return ResponseEntity.ok(
                orderService.bulkCancelOrders(
                        request.getOrderIds(),
                        admin
                )
        );
    }
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelSingle(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy admin"));

        BulkOrderActionResult result =
                orderService.bulkCancelOrders(
                        List.of(orderId),
                        admin
                );

        if (result.getFailed() > 0) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }


    @PutMapping("/bulk/ship")
    public ResponseEntity<?> bulkShip(
            @RequestBody BulkOrderActionRequest request
    ) {
        return ResponseEntity.ok(
                orderService.bulkShipOrders(request.getOrderIds())
        );
    }

    @PutMapping("/bulk/complete")
    public ResponseEntity<?> bulkComplete(
            @RequestBody BulkOrderActionRequest request
    ) {
        return ResponseEntity.ok(
                orderService.bulkCompleteOrders(request.getOrderIds())
        );
    }

    @PutMapping("/{orderId}/ship")
    public ResponseEntity<?> shipSingle(@PathVariable Long orderId) {
        BulkOrderActionResult result =
                orderService.bulkShipOrders(List.of(orderId));

        if (result.getFailed() > 0) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{orderId}/complete")
    public ResponseEntity<?> completeSingle(@PathVariable Long orderId) {
        BulkOrderActionResult result =
                orderService.bulkCompleteOrders(List.of(orderId));

        if (result.getFailed() > 0) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

}
