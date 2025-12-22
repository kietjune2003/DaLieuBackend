package com.example.AuthService.service.impl;

import com.example.AuthService.entity.Order;
import com.example.AuthService.entity.Payment;
import com.example.AuthService.entity.User;
import com.example.AuthService.enums.OrderStatus;
import com.example.AuthService.enums.PaymentMethod;
import com.example.AuthService.enums.PaymentStatus;
import com.example.AuthService.repository.OrderRepository;
import com.example.AuthService.repository.PaymentRepository;
import com.example.AuthService.service.PaymentService;
import com.example.AuthService.util.HmacUtil;
import com.example.AuthService.util.VnPayUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    @Value("${vnpay.payUrl}")
    private String payUrl;

    @Value("${vnpay.returnUrl}")
    private String returnUrl;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public String createVnPayPayment(Long orderId, User user) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

        // check owner
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Không có quyền với đơn này");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order không ở trạng thái PENDING");
        }

        // Sinh mã giao dịch nội bộ
        String txnRef = UUID.randomUUID().toString().replace("-", "");



        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .vnpTxnRef(txnRef)
                .build();

        paymentRepository.save(payment);

        // VNPay yêu cầu nhân 100
        BigDecimal vnpAmount = order.getTotalAmount().multiply(BigDecimal.valueOf(100));

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getId());
        params.put("vnp_OrderType", "billpayment");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_CreateDate",
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                        .format(LocalDateTime.now()));

        String query = VnPayUtil.buildQuery(params);
        String secureHash = VnPayUtil.hmacSHA512(hashSecret, query);

        return payUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    private boolean verifySignature(Map<String, String> params) {

        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null) return false;

        Map<String, String> sortedParams = new TreeMap<>();

        params.forEach((k, v) -> {
            if (v != null &&
                    !k.equals("vnp_SecureHash") &&
                    !k.equals("vnp_SecureHashType")) {
                sortedParams.put(k, v);
            }
        });

        StringBuilder hashData = new StringBuilder();
        sortedParams.forEach((k, v) ->
                hashData.append(k).append("=").append(v).append("&")
        );

        hashData.deleteCharAt(hashData.length() - 1);

        String calculatedHash =
                VnPayUtil.hmacSHA512(hashSecret, hashData.toString());

        return calculatedHash.equalsIgnoreCase(receivedHash);
    }
    @Override
    @Transactional
    public boolean handleVnpayReturn(Map<String, String> params) {

        if (!verifySignature(params)) {
            throw new RuntimeException("Sai chữ ký VNPay");
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");

        Payment payment = paymentRepository.findByVnpTxnRef(txnRef)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy payment"));

        // chống double callback
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return true;
        }

        if ("00".equals(responseCode)) {

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setVnpTransactionNo(transactionNo);
            payment.setPaidAt(LocalDateTime.now());

            Order order = payment.getOrder();
            order.setStatus(OrderStatus.PAID);

            orderRepository.save(order);
            paymentRepository.save(payment);

            return true;
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        return false;
    }
    @Override
    @Transactional
    public boolean handleVnpayIPN(Map<String, String> params) {

        if (!verifySignature(params)) {
            return false; // Sai chữ ký, báo lỗi cho VNPay
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        long vnpAmount = Long.parseLong(params.getOrDefault("vnp_Amount", "0")) / 100; // VNPay nhân 100

        Payment payment = paymentRepository.findByVnpTxnRef(txnRef)
                .orElse(null);

        if (payment == null) return false;

        // 🚫 chống double payment
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return true;
        }

        Order order = payment.getOrder();

        // kiểm tra amount khớp
        if (!order.getTotalAmount().equals(vnpAmount)) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return false;
        }

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setVnpTransactionNo(transactionNo);
            payment.setPaidAt(LocalDateTime.now());

            order.setStatus(OrderStatus.PAID);

            orderRepository.save(order);
            paymentRepository.save(payment);

            return true;
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        return false;
    }


}

