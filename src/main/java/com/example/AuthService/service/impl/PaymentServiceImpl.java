package com.example.AuthService.service.impl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
@Slf4j
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
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${vnpay.returnUrl}")
    private String returnUrl;
    @Value("${vnpay.refundUrl}")
    private String refundUrl;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final DrugRepository drugRepository;

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0];
        }
        return request.getRemoteAddr();
    }


    @Override
    public String createVnPayPayment(Long orderId, User user, HttpServletRequest request) {

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
        String clientIp = getClientIp(request);
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
//        vnpParams.put("vnp_IpAddr", clientIp);
        vnpParams.put("vnp_IpAddr", "8.8.8.8");

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

        sortedParams.forEach((k, v) -> {
            hashData.append(k)
                    .append("=")
                    .append(URLEncoder.encode(v, StandardCharsets.UTF_8))
                    .append("&");
        });

        hashData.deleteCharAt(hashData.length() - 1);

        String calculatedHash =
                VnPayUtil.hmacSHA512(hashSecret, hashData.toString());

        return calculatedHash.equalsIgnoreCase(receivedHash);
    }

    @Override
    @Transactional
    public boolean handleVnpayReturn(Map<String, String> params) {

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

        // ===== CHECK AMOUNT =====
        long amountFromVnpay = Long.parseLong(params.get("vnp_Amount"));
        long orderAmount = order.getTotalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        if (amountFromVnpay != orderAmount) {
            payment.setStatus(PaymentStatus.FAILED);
            return false;
        }


        if ("00".equals(responseCode)) {

            // 🔥 TRỪ KHO THEO RESERVE
            for (OrderItem item : order.getItems()) {
                Drug drug = item.getDrug();

                int reserved = drug.getReservedQuantity();
                int qty = item.getQuantity();

                if (reserved < qty) {
                    throw new RuntimeException(
                            "Dữ liệu kho không hợp lệ cho thuốc: " + drug.getName()
                    );
                }

                drug.setReservedQuantity(reserved - qty);
                drug.setSoldQuantity(drug.getSoldQuantity() + qty);
            }


            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setVnpTransactionNo(transactionNo);
            payment.setPaidAt(LocalDateTime.now());


            order.setStatus(OrderStatus.PAID);
            order.setPaymentMethod(PaymentMethod.VNPAY);

            return true;
        }

        // ===== FAILED =====
        payment.setStatus(PaymentStatus.FAILED);
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

        Order order = payment.getOrder();

        // ===== CHECK AMOUNT =====
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


            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                return true;
            }


            for (OrderItem item : order.getItems()) {
                Drug drug = item.getDrug();


                drugRepository.save(drug);
            }

            // ===== UPDATE PAYMENT =====
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setVnpTransactionNo(transactionNo);
            payment.setPaidAt(LocalDateTime.now());

            // ===== UPDATE ORDER =====
            order.setStatus(OrderStatus.PAID);
            order.setPaymentMethod(PaymentMethod.VNPAY);

            orderRepository.save(order);
            paymentRepository.save(payment);

            return true;
        }

        // ===== FAILED =====
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        return false;
    }

    @Override
    public boolean callVnPayRefund(Payment payment, User admin) {

        try {
            String requestId = UUID.randomUUID().toString().replace("-", "");
            String createDate = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            String transactionDate = payment.getPaidAt()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            long amount = payment.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            // ===== 1. BUILD HASH DATA (THEO THỨ TỰ CỨNG) =====
            String hashData = String.join("|",
                    requestId,
                    "2.1.0",
                    "refund",
                    tmnCode,
                    "02",
                    payment.getVnpTxnRef(),
                    String.valueOf(amount),
                    payment.getVnpTransactionNo(),
                    transactionDate,
                    admin.getEmail(),
                    createDate,
                    "127.0.0.1",
                    "Hoan tien don hang " + payment.getOrder().getId()
            );

            String secureHash = VnPayUtil.hmacSHA512(hashSecret, hashData);

            log.info("VNPay REFUND hashData = {}", hashData);
            log.info("VNPay REFUND secureHash = {}", secureHash);

            // ===== 2. JSON BODY =====
            Map<String, String> body = new HashMap<>();
            body.put("vnp_RequestId", requestId);
            body.put("vnp_Version", "2.1.0");
            body.put("vnp_Command", "refund");
            body.put("vnp_TmnCode", tmnCode);
            body.put("vnp_TransactionType", "02");
            body.put("vnp_TxnRef", payment.getVnpTxnRef());
            body.put("vnp_Amount", String.valueOf(amount));
            body.put("vnp_OrderInfo", "Hoan tien don hang " + payment.getOrder().getId());
            body.put("vnp_TransactionNo", payment.getVnpTransactionNo());
            body.put("vnp_TransactionDate", transactionDate);
            body.put("vnp_CreateBy", admin.getEmail());
            body.put("vnp_CreateDate", createDate);
            body.put("vnp_IpAddr", "127.0.0.1");
            body.put("vnp_SecureHash", secureHash);

            // ===== 3. HEADER =====
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request =
                    new HttpEntity<>(body, headers);

            // ===== 4. CALL VNPay =====
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    refundUrl,
                    request,
                    Map.class
            );

            Map<String, Object> res = response.getBody();
            if (res != null) {
                res.forEach((k, v) -> log.info("{} = {}", k, v));
                return "00".equals(res.get("vnp_ResponseCode"));
            }

            return false;

        } catch (Exception e) {
            log.error("VNPay refund exception", e);
            return false;
        }
    }



}

