package com.example.baget.ledger;

import com.example.baget.customer.CustomerLedgerDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    @SuppressWarnings("JpaQlInspection")
    @Query("""
    SELECT new com.example.baget.customer.CustomerLedgerDTO(
        le.createdAt,
        le.direction,
        le.amount,
        le.category,
        le.note
    )
    FROM LedgerEntry le
    WHERE le.customerId = :customerId
    ORDER BY le.createdAt DESC
    """)
    List<CustomerLedgerDTO> findCustomerLedger(Long customerId);

    @Query("""
        SELECT COALESCE(SUM(
            CASE
                WHEN l.direction = 'IN' THEN l.amount
                ELSE -l.amount
            END
        ), 0)
        FROM LedgerEntry l
        WHERE l.customerId = :customerId
    """)
    BigDecimal getCustomerBalance(Long customerId);

    @Query("""
    SELECT COALESCE(SUM(le.amount), 0)
    FROM LedgerEntry le
    WHERE le.invoiceId = :invoiceId
      AND le.direction = com.example.baget.ledger.LedgerDirection.IN
""")
    BigDecimal sumInByInvoice(@Param("invoiceId") Long invoiceId);


    @Query("""
    SELECT COALESCE(SUM(le.amount), 0)
    FROM LedgerEntry le
    WHERE le.invoiceId = :invoiceId
      AND le.direction = com.example.baget.ledger.LedgerDirection.OUT
""")
    BigDecimal sumOutByInvoice(@Param("invoiceId") Long invoiceId);


    @Query("""
    SELECT COALESCE(SUM(le.amount), 0)
    FROM LedgerEntry le
    WHERE le.orderId = :orderId
      AND le.direction = com.example.baget.ledger.LedgerDirection.IN
""")
    BigDecimal sumInByOrder(@Param("orderId") Long orderId);


    @Query("""
    SELECT COALESCE(SUM(le.amount), 0)
    FROM LedgerEntry le
    WHERE le.orderId = :orderId
      AND le.direction = com.example.baget.ledger.LedgerDirection.OUT
""")
    BigDecimal sumOutByOrder(@Param("orderId") Long orderId);}
