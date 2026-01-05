package com.example.AuthService.job;
import lombok.extern.slf4j.Slf4j;
import com.example.AuthService.entity.Drug;
import com.example.AuthService.entity.Order;
import com.example.AuthService.entity.OrderItem;
import com.example.AuthService.enums.OrderStatus;
import com.example.AuthService.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCleanupJob {

    private final OrderRepository orderRepository;

    @Scheduled(fixedRate = 15 * 60 * 1000) // mỗi 15 phút
    @Transactional
    public void cancelExpiredOrders() {

        LocalDateTime expiredTime =
                LocalDateTime.now().minusMinutes(15);

        log.info("[CLEANUP] Start cleanup job, expiredTime={}", expiredTime);

        List<Order> expiredOrders =
                orderRepository.findExpiredUnconfirmedOrders(expiredTime);

        if (expiredOrders.isEmpty()) {
            log.info("[CLEANUP] No expired orders found");
            return;
        }

        log.info("[CLEANUP] Found {} expired orders", expiredOrders.size());

        for (Order order : expiredOrders) {

            // double check an toàn order.getStatus() != OrderStatus.PENDING ||
            if (
                    order.getPaymentMethod() != null) {

                log.warn(
                        "[CLEANUP] Skip orderId={} (status={}, paymentMethod={})",
                        order.getId(),
                        order.getStatus(),
                        order.getPaymentMethod()
                );
                continue;
            }

            // 1️⃣ Trả kho đã reserve
            for (OrderItem item : order.getItems()) {
                Drug drug = item.getDrug();

                log.info(
                        "[CLEANUP] Release reserve: orderId={}, drugId={}, qty={}",
                        order.getId(),
                        drug.getId(),
                        item.getQuantity()
                );

                drug.setReservedQuantity(
                        drug.getReservedQuantity() - item.getQuantity()
                );
            }

            // 2️⃣ Huỷ đơn
            order.setStatus(OrderStatus.CANCELLED);

            log.info(
                    "[CLEANUP] Order CANCELLED: orderId={}",
                    order.getId()
            );
        }

        log.info("[CLEANUP] Cleanup job finished");
    }
}