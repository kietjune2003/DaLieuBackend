package com.example.AuthService.service.impl;

import com.example.AuthService.dto.stats.*;
import com.example.AuthService.entity.OrderItem;
import com.example.AuthService.enums.*;
import com.example.AuthService.repository.OrderRepository;
import com.example.AuthService.repository.PaymentRepository;
import com.example.AuthService.service.AdminStatisticsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final EntityManager em;

    /* ========================= PUBLIC ========================= */

//    @Override
//    public RevenueSummaryDto getRevenueSummary(RevenueStatsFilter f) {
//        LocalDateTime from = requireFrom(f.getFrom());
//        LocalDateTime to = requireTo(f.getTo());
//
//        // ===== Revenue =====
//        BigDecimal vnpayGross = sumPaymentAmountByPaidAt(PaymentStatus.SUCCESS, PaymentMethod.VNPAY, from, to);
//        BigDecimal refundAmount =
//                sumPaymentAmountByRefundedAt(PaymentStatus.REFUNDED, PaymentMethod.VNPAY, from, to);
//
//        BigDecimal codRevenue = BigDecimal.ZERO;
//        if (f.getMode() == StatsMode.SALES_ALL) {
//            codRevenue = sumCodCompletedOrders(from, to);
//        }
//
//        BigDecimal grossRevenue = (f.getMode() == StatsMode.CASHFLOW_VNPAY_ONLY)
//                ? vnpayGross
//                : vnpayGross.add(codRevenue);
//
//        BigDecimal netRevenue = grossRevenue.subtract(refundAmount);
//
//        // ===== Orders count (để AOV) =====
//        long vnpayOrders = countDistinctOrdersFromPayments(PaymentStatus.SUCCESS, PaymentMethod.VNPAY, from, to);
//        long codOrders = (f.getMode() == StatsMode.SALES_ALL) ? countCodCompletedOrders(from, to) : 0;
//        long ordersCount = (f.getMode() == StatsMode.CASHFLOW_VNPAY_ONLY) ? vnpayOrders : (vnpayOrders + codOrders);
//
//        BigDecimal aov = (ordersCount > 0)
//                ? netRevenue.divide(BigDecimal.valueOf(ordersCount), 2, RoundingMode.HALF_UP)
//                : BigDecimal.ZERO;
//
//        // ===== COGS (tiền vốn) =====
//        BigDecimal vnpayCogs = sumVnpayCogsSuccess(from, to);
//        BigDecimal codCogs = (f.getMode() == StatsMode.SALES_ALL) ? sumCodCogsCompleted(from, to) : BigDecimal.ZERO;
//
//        BigDecimal grossCogs = (f.getMode() == StatsMode.CASHFLOW_VNPAY_ONLY)
//                ? vnpayCogs
//                : vnpayCogs.add(codCogs);
//
//        // refundedCogs hiện tính "approx" theo Payment.createdAt (vì chưa có refundedAt)
//        BigDecimal refundedCogs = sumVnpayCogsRefunded(from, to);
//        BigDecimal netCogs = grossCogs.subtract(refundedCogs);
//
//        // ===== Profit =====
//        BigDecimal grossProfit = grossRevenue.subtract(grossCogs);
//        BigDecimal netProfit = netRevenue.subtract(netCogs);
//        BigDecimal netMarginPct = pct(netProfit, netRevenue);
//
//        return RevenueSummaryDto.builder()
//                .grossRevenue(grossRevenue)
//                .refundAmount(refundAmount)
//                .netRevenue(netRevenue)
//                .vnpayRevenue(vnpayGross)
//                .codRevenue(codRevenue)
//                .ordersCount(ordersCount)
//                .aov(aov)
//
//                .grossCogs(grossCogs)
//                .refundedCogs(refundedCogs)
//                .netCogs(netCogs)
//
//                .grossProfit(grossProfit)
//                .netProfit(netProfit)
//                .netMarginPct(netMarginPct)
//                .build();
//    }

    @Override
    public RevenueSummaryDto getRevenueSummary(RevenueStatsFilter f) {
        LocalDateTime from = requireFrom(f.getFrom());
        LocalDateTime to = requireTo(f.getTo());

        // ===== Revenue =====
        BigDecimal vnpayNet = sumPaymentAmountByPaidAt(PaymentStatus.SUCCESS, PaymentMethod.VNPAY, from, to);
        BigDecimal refundAmount =
                sumPaymentAmountByRefundedAt(PaymentStatus.REFUNDED, PaymentMethod.VNPAY, from, to);

        BigDecimal codRevenue = BigDecimal.ZERO;
        if (f.getMode() == StatsMode.SALES_ALL) {
            codRevenue = sumCodCompletedOrders(from, to);
        }

        BigDecimal netRevenue = (f.getMode() == StatsMode.CASHFLOW_VNPAY_ONLY)
                ? vnpayNet
                : vnpayNet.add(codRevenue);

        BigDecimal grossRevenue = netRevenue.add(refundAmount);

        // ===== Orders count (để AOV) =====
        long vnpayOrders = countDistinctOrdersFromPayments(PaymentStatus.SUCCESS, PaymentMethod.VNPAY, from, to);
        long codOrders = (f.getMode() == StatsMode.SALES_ALL) ? countCodCompletedOrders(from, to) : 0;
        long ordersCount = (f.getMode() == StatsMode.CASHFLOW_VNPAY_ONLY) ? vnpayOrders : (vnpayOrders + codOrders);

        BigDecimal aov = (ordersCount > 0)
                ? netRevenue.divide(BigDecimal.valueOf(ordersCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // ===== COGS (tiền vốn) =====
        BigDecimal vnpayCogs = sumVnpayCogsSuccess(from, to);
        BigDecimal codCogs = (f.getMode() == StatsMode.SALES_ALL) ? sumCodCogsCompleted(from, to) : BigDecimal.ZERO;

        BigDecimal netCogs = (f.getMode() == StatsMode.CASHFLOW_VNPAY_ONLY)
                ? vnpayCogs
                : vnpayCogs.add(codCogs);

        // refundedCogs hiện tính "approx" theo Payment.createdAt (vì chưa có refundedAt)
        BigDecimal refundedCogs = sumVnpayCogsRefunded(from, to);
        BigDecimal grossCogs = netCogs.add(refundedCogs);

        // ===== Profit =====
        BigDecimal grossProfit = grossRevenue.subtract(grossCogs);
        BigDecimal netProfit = netRevenue.subtract(netCogs);
        BigDecimal netMarginPct = pct(netProfit, netRevenue);

        return RevenueSummaryDto.builder()
                .grossRevenue(grossRevenue)
                .refundAmount(refundAmount)
                .netRevenue(netRevenue)
                .vnpayRevenue(vnpayNet.add(refundAmount))
                .codRevenue(codRevenue)
                .ordersCount(ordersCount)
                .aov(aov)

                .grossCogs(grossCogs)
                .refundedCogs(refundedCogs)
                .netCogs(netCogs)

                .grossProfit(grossProfit)
                .netProfit(netProfit)
                .netMarginPct(netMarginPct)
                .build();
    }

//    @Override
//    public RevenueTimeSeriesDto getRevenueTimeSeries(RevenueStatsFilter f) {
//        LocalDateTime from = requireFrom(f.getFrom());
//        LocalDateTime to = requireTo(f.getTo());
//
//        // ===== Daily maps (Revenue) =====
//        Map<LocalDate, BigDecimal> vnpayByDay = sumPaymentSuccessByDay(from, to);
//        Map<LocalDate, BigDecimal> refundByDay = sumPaymentRefundByDayApprox(from, to); // approx theo createdAt
//        Map<LocalDate, BigDecimal> codByDay = (f.getMode() == StatsMode.SALES_ALL)
//                ? sumCodCompletedByDay(from, to)
//                : Collections.emptyMap();
//
//        // ===== Daily maps (COGS) =====
//        Map<LocalDate, BigDecimal> vnpayCogsByDay = sumVnpayCogsSuccessByDay(from, to);
//        Map<LocalDate, BigDecimal> refundCogsByDay = sumVnpayCogsRefundedByDayApprox(from, to);
//        Map<LocalDate, BigDecimal> codCogsByDay = (f.getMode() == StatsMode.SALES_ALL)
//                ? sumCodCogsCompletedByDay(from, to)
//                : Collections.emptyMap();
//
//        // Buckets
//        LocalDate startDate = from.toLocalDate();
//        LocalDate endExclusive = to.toLocalDate(); // vì to là exclusive (thường là đầu ngày + 1)
//        List<LocalDate> buckets = generateBuckets(startDate, endExclusive, f.getGroupBy());
//
//        List<RevenueTimePointDto> points = new ArrayList<>();
//        for (LocalDate bucketStart : buckets) {
//            LocalDate bucketEndExclusive = nextBucketStart(bucketStart, f.getGroupBy());
//
//            BigDecimal vnpay = sumBetween(vnpayByDay, bucketStart, bucketEndExclusive);
//            BigDecimal cod = sumBetween(codByDay, bucketStart, bucketEndExclusive);
//            BigDecimal refund = sumBetween(refundByDay, bucketStart, bucketEndExclusive);
//
//            BigDecimal grossRevenue = (f.getMode() == StatsMode.CASHFLOW_VNPAY_ONLY) ? vnpay : vnpay.add(cod);
//            BigDecimal netRevenue = grossRevenue.subtract(refund);
//
//            BigDecimal vnpayCogs = sumBetween(vnpayCogsByDay, bucketStart, bucketEndExclusive);
//            BigDecimal codCogs = sumBetween(codCogsByDay, bucketStart, bucketEndExclusive);
//            BigDecimal refundedCogs = sumBetween(refundCogsByDay, bucketStart, bucketEndExclusive);
//
//            BigDecimal grossCogs = (f.getMode() == StatsMode.CASHFLOW_VNPAY_ONLY) ? vnpayCogs : vnpayCogs.add(codCogs);
//            BigDecimal netCogs = grossCogs.subtract(refundedCogs);
//
//            BigDecimal netProfit = netRevenue.subtract(netCogs);
//            BigDecimal netMarginPct = pct(netProfit, netRevenue);
//
//            points.add(RevenueTimePointDto.builder()
//                    .bucket(bucketStart)
//                    .vnpayGross(vnpay)
//                    .codRevenue(cod)
//                    .refundAmount(refund)
//                    .netRevenue(netRevenue)
//
//                    .grossCogs(grossCogs)
//                    .refundedCogs(refundedCogs)
//                    .netCogs(netCogs)
//
//                    .netProfit(netProfit)
//                    .netMarginPct(netMarginPct)
//                    .build());
//        }
//
//        return RevenueTimeSeriesDto.builder()
//                .groupBy(f.getGroupBy())
//                .points(points)
//                .build();
//    }
@Override
public RevenueTimeSeriesDto getRevenueTimeSeries(RevenueStatsFilter f) {
    LocalDateTime from = requireFrom(f.getFrom());
    LocalDateTime to = requireTo(f.getTo());

    // ===== Daily maps (Revenue) =====
    Map<LocalDate, BigDecimal> vnpayNetByDay = sumPaymentSuccessByDay(from, to);
    Map<LocalDate, BigDecimal> refundByDay = sumPaymentRefundByDayApprox(from, to);
    Map<LocalDate, BigDecimal> codByDay = (f.getMode() == StatsMode.SALES_ALL)
            ? sumCodCompletedByDay(from, to)
            : Collections.emptyMap();

    // ===== Daily maps (COGS) =====
    Map<LocalDate, BigDecimal> vnpayCogsByDay = sumVnpayCogsSuccessByDay(from, to);
    Map<LocalDate, BigDecimal> refundCogsByDay = sumVnpayCogsRefundedByDayApprox(from, to);
    Map<LocalDate, BigDecimal> codCogsByDay = (f.getMode() == StatsMode.SALES_ALL)
            ? sumCodCogsCompletedByDay(from, to)
            : Collections.emptyMap();

    // Buckets
    LocalDate startDate = from.toLocalDate();
    LocalDate endExclusive = to.toLocalDate();
    List<LocalDate> buckets = generateBuckets(startDate, endExclusive, f.getGroupBy());

    List<RevenueTimePointDto> points = new ArrayList<>();

    for (LocalDate bucketStart : buckets) {
        LocalDate bucketEndExclusive = nextBucketStart(bucketStart, f.getGroupBy());

        // ===== Revenue =====
        BigDecimal vnpayNet = sumBetween(vnpayNetByDay, bucketStart, bucketEndExclusive);
        BigDecimal refundAmount = sumBetween(refundByDay, bucketStart, bucketEndExclusive);
        BigDecimal codRevenue = sumBetween(codByDay, bucketStart, bucketEndExclusive);

        BigDecimal vnpayGross = vnpayNet.add(refundAmount);

        BigDecimal netRevenue = (f.getMode() == StatsMode.CASHFLOW_VNPAY_ONLY)
                ? vnpayNet
                : vnpayNet.add(codRevenue);

        // ===== COGS =====
        BigDecimal vnpayCogs = sumBetween(vnpayCogsByDay, bucketStart, bucketEndExclusive);
        BigDecimal codCogs = sumBetween(codCogsByDay, bucketStart, bucketEndExclusive);
        BigDecimal refundedCogs = sumBetween(refundCogsByDay, bucketStart, bucketEndExclusive);

        BigDecimal netCogs = (f.getMode() == StatsMode.CASHFLOW_VNPAY_ONLY)
                ? vnpayCogs
                : vnpayCogs.add(codCogs);

        BigDecimal grossCogs = netCogs.add(refundedCogs);

        // ===== Profit =====
        BigDecimal netProfit = netRevenue.subtract(netCogs);
        BigDecimal netMarginPct = pct(netProfit, netRevenue);

        points.add(RevenueTimePointDto.builder()
                .bucket(bucketStart)

                // Revenue
                .vnpayGross(vnpayGross)
                .codRevenue(codRevenue)
                .refundAmount(refundAmount)
                .netRevenue(netRevenue)

                // COGS
                .grossCogs(grossCogs)
                .refundedCogs(refundedCogs)
                .netCogs(netCogs)

                // Profit
                .netProfit(netProfit)
                .netMarginPct(netMarginPct)
                .build());
    }

    return RevenueTimeSeriesDto.builder()
            .groupBy(f.getGroupBy())
            .points(points)
            .build();
}


    @Override
    public Page<TopProductDto> getTopProducts(RevenueStatsFilter f) {
        LocalDateTime from = requireFrom(f.getFrom());
        LocalDateTime to = requireTo(f.getTo());

        int topN = (f.getTopN() != null && f.getTopN() > 0) ? f.getTopN() : 10;

        // vnpay (SUCCESS)
        List<TopProductDto> vnpay = topProductsVnpay(from, to, topN, f.getDrugIds(), f.getUserId());

        // cod (COMPLETED)
        List<TopProductDto> cod = (f.getMode() == StatsMode.SALES_ALL)
                ? topProductsCod(from, to, topN, f.getDrugIds(), f.getUserId())
                : List.of();

        // Merge by drugId (recompute profit/margin)
        Map<Long, TopProductDto> merged = new HashMap<>();
        for (TopProductDto t : vnpay) {
            merged.put(t.getDrugId(), t);
        }
        for (TopProductDto t : cod) {
            merged.merge(t.getDrugId(), t, (a, b) -> {
                BigDecimal revenue = nz(a.getRevenue()).add(nz(b.getRevenue()));
                BigDecimal cogs = nz(a.getCogs()).add(nz(b.getCogs()));
                long qty = a.getQuantity() + b.getQuantity();

                BigDecimal profit = revenue.subtract(cogs);
                BigDecimal margin = pct(profit, revenue);

                return TopProductDto.builder()
                        .drugId(a.getDrugId())
                        .drugName(a.getDrugName() != null ? a.getDrugName() : b.getDrugName())
                        .quantity(qty)
                        .revenue(revenue)
                        .cogs(cogs)
                        .profit(profit)
                        .marginPct(margin)
                        .build();
            });
        }

        List<TopProductDto> sorted = merged.values().stream()
                .sorted(Comparator.comparing(TopProductDto::getRevenue, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .limit(topN)
                .collect(Collectors.toList());

        return new PageImpl<>(sorted, PageRequest.of(0, topN), sorted.size());
    }

    /* ========================= PROFIT HELPERS ========================= */

    private BigDecimal pct(BigDecimal num, BigDecimal den) {
        if (den == null || den.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        if (num == null) num = BigDecimal.ZERO;
        return num.multiply(BigDecimal.valueOf(100)).divide(den, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /* ========================= TIME VALIDATION ========================= */

    private LocalDateTime requireFrom(LocalDateTime from) {
        return (from == null) ? LocalDateTime.now().minusDays(30) : from;
    }

    private LocalDateTime requireTo(LocalDateTime to) {
        return (to == null) ? LocalDateTime.now() : to;
    }

    /* ========================= REVENUE QUERIES ========================= */

    private BigDecimal sumPaymentAmountByPaidAt(PaymentStatus st, PaymentMethod pm, LocalDateTime from, LocalDateTime to) {
        String jpql = """
            select coalesce(sum(p.amount), 0)
            from Payment p
            where p.status = :st
              and p.method = :pm
              and p.paidAt is not null
              and p.paidAt >= :from and p.paidAt < :to
        """;
        return em.createQuery(jpql, BigDecimal.class)
                .setParameter("st", st)
                .setParameter("pm", pm)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    // refund amount "approx" theo createdAt
    private BigDecimal sumPaymentAmountByRefundedAt(
            PaymentStatus st,
            PaymentMethod pm,
            LocalDateTime from,
            LocalDateTime to
    ) {
        String jpql = """
        select coalesce(sum(p.amount), 0)
        from Payment p
        where p.status = :st
          and p.method = :pm
          and p.refundedAt is not null
          and p.refundedAt >= :from and p.refundedAt < :to
    """;
        return em.createQuery(jpql, BigDecimal.class)
                .setParameter("st", st)
                .setParameter("pm", pm)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }


    private BigDecimal sumCodCompletedOrders(LocalDateTime from, LocalDateTime to) {
        String jpql = """
            select coalesce(sum(o.totalAmount), 0)
            from Order o
            where o.paymentMethod = :cod
              and o.status = :st
              and o.createdAt >= :from and o.createdAt < :to
        """;
        return em.createQuery(jpql, BigDecimal.class)
                .setParameter("cod", PaymentMethod.COD)
                .setParameter("st", OrderStatus.COMPLETED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    private long countCodCompletedOrders(LocalDateTime from, LocalDateTime to) {
        String jpql = """
            select count(o.id)
            from Order o
            where o.paymentMethod = :cod
              and o.status = :st
              and o.createdAt >= :from and o.createdAt < :to
        """;
        Long v = em.createQuery(jpql, Long.class)
                .setParameter("cod", PaymentMethod.COD)
                .setParameter("st", OrderStatus.COMPLETED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return v == null ? 0 : v;
    }

    private long countDistinctOrdersFromPayments(PaymentStatus st, PaymentMethod pm, LocalDateTime from, LocalDateTime to) {
        String jpql = """
            select count(distinct p.order.id)
            from Payment p
            where p.status = :st
              and p.method = :pm
              and p.paidAt is not null
              and p.paidAt >= :from and p.paidAt < :to
        """;
        Long v = em.createQuery(jpql, Long.class)
                .setParameter("st", st)
                .setParameter("pm", pm)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
        return v == null ? 0 : v;
    }

    private Map<LocalDate, BigDecimal> sumPaymentSuccessByDay(LocalDateTime from, LocalDateTime to) {
        String jpql = """
            select function('date', p.paidAt), coalesce(sum(p.amount), 0)
            from Payment p
            where p.status = :st
              and p.method = :pm
              and p.paidAt is not null
              and p.paidAt >= :from and p.paidAt < :to
            group by function('date', p.paidAt)
            order by function('date', p.paidAt)
        """;
        List<Object[]> rows = em.createQuery(jpql, Object[].class)
                .setParameter("st", PaymentStatus.SUCCESS)
                .setParameter("pm", PaymentMethod.VNPAY)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return toDateMap(rows);
    }

    private Map<LocalDate, BigDecimal> sumPaymentRefundByDayApprox(LocalDateTime from, LocalDateTime to) {
        String jpql = """
            select function('date', p.createdAt), coalesce(sum(p.amount), 0)
            from Payment p
            where p.status = :st
              and p.method = :pm
              and p.createdAt >= :from and p.createdAt < :to
            group by function('date', p.createdAt)
            order by function('date', p.createdAt)
        """;
        List<Object[]> rows = em.createQuery(jpql, Object[].class)
                .setParameter("st", PaymentStatus.REFUNDED)
                .setParameter("pm", PaymentMethod.VNPAY)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return toDateMap(rows);
    }

    private Map<LocalDate, BigDecimal> sumCodCompletedByDay(LocalDateTime from, LocalDateTime to) {
        String jpql = """
            select function('date', o.createdAt), coalesce(sum(o.totalAmount), 0)
            from Order o
            where o.paymentMethod = :cod
              and o.status = :st
              and o.createdAt >= :from and o.createdAt < :to
            group by function('date', o.createdAt)
            order by function('date', o.createdAt)
        """;
        List<Object[]> rows = em.createQuery(jpql, Object[].class)
                .setParameter("cod", PaymentMethod.COD)
                .setParameter("st", OrderStatus.COMPLETED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return toDateMap(rows);
    }

    /* ========================= COGS QUERIES ========================= */

    // VNPAY COGS for SUCCESS (by paidAt)
    private BigDecimal sumVnpayCogsSuccess(LocalDateTime from, LocalDateTime to) {
        String jpql = """
            select coalesce(sum(d.importPrice * oi.quantity), 0)
            from OrderItem oi
            join oi.drug d
            join oi.order o,
                 Payment p
            where p.order = o
              and p.status = :st
              and p.method = :pm
              and p.paidAt is not null
              and p.paidAt >= :from and p.paidAt < :to
        """;
        return em.createQuery(jpql, BigDecimal.class)
                .setParameter("st", PaymentStatus.SUCCESS)
                .setParameter("pm", PaymentMethod.VNPAY)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    // COD COGS for COMPLETED (by order.createdAt)
    private BigDecimal sumCodCogsCompleted(LocalDateTime from, LocalDateTime to) {
        String jpql = """
            select coalesce(sum(d.importPrice * oi.quantity), 0)
            from OrderItem oi
            join oi.drug d
            join oi.order o
            where o.paymentMethod = :cod
              and o.status = :st
              and o.createdAt >= :from and o.createdAt < :to
        """;
        return em.createQuery(jpql, BigDecimal.class)
                .setParameter("cod", PaymentMethod.COD)
                .setParameter("st", OrderStatus.COMPLETED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    // REFUNDED COGS approx by payment.createdAt
    private BigDecimal sumVnpayCogsRefunded(LocalDateTime from, LocalDateTime to) {
        String jpql = """
        select coalesce(sum(d.importPrice * oi.quantity), 0)
        from OrderItem oi
        join oi.drug d
        join oi.order o,
             Payment p
        where p.order = o
          and p.status = :st
          and p.method = :pm
          and p.refundedAt is not null
          and p.refundedAt >= :from and p.refundedAt < :to
    """;
        return em.createQuery(jpql, BigDecimal.class)
                .setParameter("st", PaymentStatus.REFUNDED)
                .setParameter("pm", PaymentMethod.VNPAY)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }


    private Map<LocalDate, BigDecimal> sumVnpayCogsSuccessByDay(LocalDateTime from, LocalDateTime to) {
        String jpql = """
            select function('date', p.paidAt), coalesce(sum(d.importPrice * oi.quantity), 0)
            from OrderItem oi
            join oi.drug d
            join oi.order o,
                 Payment p
            where p.order = o
              and p.status = :st
              and p.method = :pm
              and p.paidAt is not null
              and p.paidAt >= :from and p.paidAt < :to
            group by function('date', p.paidAt)
            order by function('date', p.paidAt)
        """;
        List<Object[]> rows = em.createQuery(jpql, Object[].class)
                .setParameter("st", PaymentStatus.SUCCESS)
                .setParameter("pm", PaymentMethod.VNPAY)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return toDateMap(rows);
    }

    private Map<LocalDate, BigDecimal> sumVnpayCogsRefundedByDayApprox(LocalDateTime from, LocalDateTime to) {
        String jpql = """
            select function('date', p.createdAt), coalesce(sum(d.importPrice * oi.quantity), 0)
            from OrderItem oi
            join oi.drug d
            join oi.order o,
                 Payment p
            where p.order = o
              and p.status = :st
              and p.method = :pm
              and p.createdAt >= :from and p.createdAt < :to
            group by function('date', p.createdAt)
            order by function('date', p.createdAt)
        """;
        List<Object[]> rows = em.createQuery(jpql, Object[].class)
                .setParameter("st", PaymentStatus.REFUNDED)
                .setParameter("pm", PaymentMethod.VNPAY)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return toDateMap(rows);
    }

    private Map<LocalDate, BigDecimal> sumCodCogsCompletedByDay(LocalDateTime from, LocalDateTime to) {
        String jpql = """
            select function('date', o.createdAt), coalesce(sum(d.importPrice * oi.quantity), 0)
            from OrderItem oi
            join oi.drug d
            join oi.order o
            where o.paymentMethod = :cod
              and o.status = :st
              and o.createdAt >= :from and o.createdAt < :to
            group by function('date', o.createdAt)
            order by function('date', o.createdAt)
        """;
        List<Object[]> rows = em.createQuery(jpql, Object[].class)
                .setParameter("cod", PaymentMethod.COD)
                .setParameter("st", OrderStatus.COMPLETED)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
        return toDateMap(rows);
    }

    /* ========================= TOP PRODUCTS ========================= */

    private List<TopProductDto> topProductsVnpay(
            LocalDateTime from,
            LocalDateTime to,
            int topN,
            Set<Long> drugIds,
            Long userId
    ) {
        // build JPQL dynamically -> tránh lỗi param drugIds khi rỗng
        StringBuilder jpql = new StringBuilder("""
            select d.id, d.name,
                   coalesce(sum(oi.quantity), 0),
                   coalesce(sum(oi.totalPrice), 0),
                   coalesce(sum(d.importPrice * oi.quantity), 0)
            from OrderItem oi
            join oi.drug d
            join oi.order o,
                 Payment p
            where p.order = o
              and p.status = :pst
              and p.method = :pm
              and p.paidAt is not null
              and p.paidAt >= :from and p.paidAt < :to
        """);

        if (userId != null) jpql.append(" and o.user.id = :userId ");
        if (drugIds != null && !drugIds.isEmpty()) jpql.append(" and d.id in :drugIds ");

        jpql.append("""
            group by d.id, d.name
            order by coalesce(sum(oi.totalPrice), 0) desc
        """);

        TypedQuery<Object[]> q = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("pst", PaymentStatus.SUCCESS)
                .setParameter("pm", PaymentMethod.VNPAY)
                .setParameter("from", from)
                .setParameter("to", to);

        if (userId != null) q.setParameter("userId", userId);
        if (drugIds != null && !drugIds.isEmpty()) q.setParameter("drugIds", drugIds);

        q.setMaxResults(topN);

        return mapTopProductRows(q.getResultList());
    }

    private List<TopProductDto> topProductsCod(
            LocalDateTime from,
            LocalDateTime to,
            int topN,
            Set<Long> drugIds,
            Long userId
    ) {
        StringBuilder jpql = new StringBuilder("""
            select d.id, d.name,
                   coalesce(sum(oi.quantity), 0),
                   coalesce(sum(oi.totalPrice), 0),
                   coalesce(sum(d.importPrice * oi.quantity), 0)
            from OrderItem oi
            join oi.drug d
            join oi.order o
            where o.paymentMethod = :cod
              and o.status = :ost
              and o.createdAt >= :from and o.createdAt < :to
        """);

        if (userId != null) jpql.append(" and o.user.id = :userId ");
        if (drugIds != null && !drugIds.isEmpty()) jpql.append(" and d.id in :drugIds ");

        jpql.append("""
            group by d.id, d.name
            order by coalesce(sum(oi.totalPrice), 0) desc
        """);

        TypedQuery<Object[]> q = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("cod", PaymentMethod.COD)
                .setParameter("ost", OrderStatus.COMPLETED)
                .setParameter("from", from)
                .setParameter("to", to);

        if (userId != null) q.setParameter("userId", userId);
        if (drugIds != null && !drugIds.isEmpty()) q.setParameter("drugIds", drugIds);

        q.setMaxResults(topN);

        return mapTopProductRows(q.getResultList());
    }

    private List<TopProductDto> mapTopProductRows(List<Object[]> rows) {
        List<TopProductDto> list = new ArrayList<>();
        for (Object[] r : rows) {
            Long drugId = ((Number) r[0]).longValue();
            String name = String.valueOf(r[1]);
            long qty = ((Number) r[2]).longValue();

            // totalPrice đang Double -> cast Number -> BigDecimal
            BigDecimal revenue = BigDecimal.valueOf(((Number) r[3]).doubleValue());

            BigDecimal cogs;
            if (r[4] instanceof BigDecimal bd) cogs = bd;
            else cogs = BigDecimal.valueOf(((Number) r[4]).doubleValue());

            BigDecimal profit = revenue.subtract(cogs);
            BigDecimal margin = pct(profit, revenue);

            list.add(TopProductDto.builder()
                    .drugId(drugId)
                    .drugName(name)
                    .quantity(qty)
                    .revenue(revenue)
                    .cogs(cogs)
                    .profit(profit)
                    .marginPct(margin)
                    .build());
        }
        return list;
    }

    /* ========================= BUCKET HELPERS ========================= */

    private List<LocalDate> generateBuckets(LocalDate start, LocalDate endExclusive, StatsGroupBy g) {
        LocalDate cur = alignToBucketStart(start, g);
        List<LocalDate> out = new ArrayList<>();
        while (cur.isBefore(endExclusive)) {
            out.add(cur);
            cur = nextBucketStart(cur, g);
            if (out.size() > 5000) break;
        }
        return out;
    }

    private LocalDate alignToBucketStart(LocalDate d, StatsGroupBy g) {
        return switch (g) {
            case DAY -> d;
            case WEEK -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> d.withDayOfMonth(1);
        };
    }

    private LocalDate nextBucketStart(LocalDate bucketStart, StatsGroupBy g) {
        return switch (g) {
            case DAY -> bucketStart.plusDays(1);
            case WEEK -> bucketStart.plusWeeks(1);
            case MONTH -> bucketStart.plusMonths(1).withDayOfMonth(1);
        };
    }

    private BigDecimal sumBetween(Map<LocalDate, BigDecimal> daily, LocalDate start, LocalDate endExclusive) {
        if (daily == null || daily.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        LocalDate cur = start;
        while (cur.isBefore(endExclusive)) {
            sum = sum.add(daily.getOrDefault(cur, BigDecimal.ZERO));
            cur = cur.plusDays(1);
            if (sum.scale() > 10) sum = sum.setScale(10, RoundingMode.HALF_UP);
        }
        return sum;
    }

    private Map<LocalDate, BigDecimal> toDateMap(List<Object[]> rows) {
        Map<LocalDate, BigDecimal> map = new HashMap<>();
        for (Object[] r : rows) {
            LocalDate d;
            if (r[0] instanceof java.sql.Date sqlDate) d = sqlDate.toLocalDate();
            else if (r[0] instanceof LocalDate ld) d = ld;
            else d = LocalDate.parse(String.valueOf(r[0]));

            BigDecimal v;
            if (r[1] instanceof BigDecimal bd) v = bd;
            else v = BigDecimal.valueOf(((Number) r[1]).doubleValue());

            map.put(d, v);
        }
        return map;
    }
}
