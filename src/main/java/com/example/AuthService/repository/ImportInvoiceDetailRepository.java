package com.example.AuthService.repository;

import com.example.AuthService.entity.ImportInvoiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImportInvoiceDetailRepository extends JpaRepository<ImportInvoiceDetail, Long> {

    @Query("""
        SELECT COALESCE(SUM(d.quantity), 0)
        FROM ImportInvoiceDetail d
        WHERE d.drug.id = :drugId
    """)
    Integer totalImported(Long drugId);
    @Query("SELECT i.drug.id, SUM(i.quantity), COALESCE(SUM(o.quantity), 0) FROM ImportInvoiceDetail i " +
            "LEFT JOIN OrderItem o ON o.drug.id = i.drug.id " +
            "WHERE i.drug.id IN :drugIds GROUP BY i.drug.id")
    List<Object[]> findStockDataForDrugs(@Param("drugIds") List<Long> drugIds);
    @Query("""
    SELECT i.drug.id, COALESCE(SUM(i.quantity), 0)
    FROM ImportInvoiceDetail i
    WHERE i.drug.id IN :drugIds
    GROUP BY i.drug.id
""")
    List<Object[]> findTotalImportedForDrugs(
            @Param("drugIds") List<Long> drugIds
    );


}
