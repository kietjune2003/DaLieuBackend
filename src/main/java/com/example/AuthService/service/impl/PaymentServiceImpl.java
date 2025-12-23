package com.example.AuthService.service.impl;

import com.example.AuthService.entity.*;
import com.example.AuthService.enums.OrderStatus;
import com.example.AuthService.enums.PaymentMethod;
import com.example.AuthService.enums.PaymentStatus;
import com.example.AuthService.repository.DrugRepository;
import com.example.AuthService.repository.OrderRepository;
import com.example.AuthService.repository.PaymentRepository;
import com.example.AuthService.service.PaymentService;
import com.example.AuthService.util.VnPayUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final DrugRepository drugRepository;
    @Override
    public String createVnPayPayment(Long orderId, User user) {

        // ===== 1. Validate nghiệp vụ =====
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Không có quyền với đơn này");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order không ở trạng thái PENDING");
        }

        // ===== 2. Tạo mã giao dịch =====
        String txnRef = UUID.randomUUID().toString().replace("-", "");

        // ===== 3. Lưu payment =====
        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .method(PaymentMethod.VNPAY)
                .status(PaymentStatus.PENDING)
                .vnpTxnRef(txnRef)
                .build();
        paymentRepository.save(payment);

        // ===== 4. Amount (x100) =====
        long vnpAmount = order.getTotalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        // ===== 5. Params gửi VNPAY (KHÔNG encode) =====
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", tmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh_toan_don_hang_" + order.getId());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_IpAddr", "127.0.0.1"); // ⭐ BẮT BUỘC
        vnpParams.put("vnp_CreateDate",
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()));
        vnpParams.put("vnp_ExpireDate",
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now().plusMinutes(15)));

        vnpParams.put("vnp_SecureHashType", "HmacSHA512");


        // ===== 6. Params dùng để KÝ HASH (LOẠI BỎ 2 PARAM CẤM) =====
        Map<String, String> hashParams = new TreeMap<>(vnpParams);
        hashParams.remove("vnp_SecureHashType");
        hashParams.remove("vnp_SecureHash");

        // ===== 7. Build hashData (KHÔNG encode) =====
        String hashData = VnPayUtil.buildHashData(hashParams);

        // ===== 8. Ký HMAC SHA512 =====
        String secureHash = VnPayUtil.hmacSHA512(hashSecret, hashData);

        // ===== 9. Build query string (CÓ encode) =====
        String queryString = VnPayUtil.buildQueryString(new TreeMap<>(vnpParams));

        // ===== 10. URL cuối cùng =====
        return payUrl + "?" + queryString + "&vnp_SecureHash=" + secureHash;
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
            return false;
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");

        Payment payment = paymentRepository.findByVnpTxnRef(txnRef)
                .orElse(null);

        if (payment == null) return false;

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return true;
        }

        Order order = payment.getOrder();

        long amountFromVnpay = Long.parseLong(params.get("vnp_Amount"));
        long orderAmount = order.getTotalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        if (amountFromVnpay != orderAmount) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return false;
        }

        if ("00".equals(responseCode)) {

            // 🔥 TRỪ KHO NGAY KHI VNPAY SUCCESS
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

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setVnpTransactionNo(transactionNo);
            payment.setPaidAt(LocalDateTime.now());

            order.setStatus(OrderStatus.PAID);
            order.setPaymentMethod(PaymentMethod.VNPAY);

            orderRepository.save(order);
            paymentRepository.save(payment);

            return true;
        }

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        return false;
    }



}

