package com.example.AuthService.enums;

public enum StatsMode {
    SALES_ALL,           // VNPAY (Payment.SUCCESS by paidAt) + COD (Order.COMPLETED by createdAt)
    CASHFLOW_VNPAY_ONLY  // chỉ Payment (SUCCESS/REFUNDED)
}
