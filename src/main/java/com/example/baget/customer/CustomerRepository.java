package com.example.baget.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @NonNull
    Page<Customer> findAll(@NonNull Pageable pageable);
    List<CustomerSelectView> findAllBy();
    List<Customer> findByMobileContaining(String prefix);
    Optional<Customer> findFirstByMobileContainingOrderByCustNoAsc(String phone);
    Optional<Customer> findTopByCompanyStartingWithOrderByCustNoDesc(String prefix);

    @Query(value = """
        WITH base_orders AS (
            SELECT
                o.OrderNo,
                o.CustNo,
                o.BranchNo
            FROM orders o
            WHERE o.BranchNo IN (:branches)
        ),
        
        orders_without_invoice AS (
            SELECT
                bo.CustNo,
                COUNT(*) AS pending_orders
            FROM base_orders bo
            LEFT JOIN invoice_orders io ON io.order_no = bo.OrderNo
            WHERE io.order_no IS NULL
            GROUP BY bo.CustNo
        ),
        
        invoice_stats AS (
            SELECT
                i.customer_id,
                COUNT(*) AS invoice_count
            FROM invoices i
            JOIN invoice_orders io ON io.invoice_id = i.id
            JOIN orders o ON o.OrderNo = io.order_no
            WHERE o.BranchNo IN (:branches)
            GROUP BY i.customer_id
        ),
        
        ledger_balance AS (
            SELECT
                le.customer_id,
                SUM(
                    CASE
                        WHEN le.direction = 'IN' THEN le.amount
                        ELSE -le.amount
                    END
                ) AS balance,
                MAX(
                    CASE
                        WHEN le.category = 'CUSTOMER_PAYMENT'
                        THEN le.created_at
                    END
                ) AS last_payment_date
            FROM ledger_entries le
            WHERE le.branch_id IN (:branches)
            GROUP BY le.customer_id
        )
        
        SELECT
            c.CustNo,
            c.Company,
            c.Mobile,
        
            COALESCE(lb.balance, 0) AS balance,
            COALESCE(inv.invoice_count, 0) AS invoice_count,
            lb.last_payment_date,
            COALESCE(owi.pending_orders, 0) AS pending_orders
        
        FROM customer c
        
        -- 🔹 тільки ті клієнти, що мають замовлення в цих філіях
        JOIN (
            SELECT DISTINCT CustNo FROM base_orders
        ) bo ON bo.CustNo = c.CustNo
        
        LEFT JOIN ledger_balance lb ON lb.customer_id = c.CustNo
        LEFT JOIN invoice_stats inv ON inv.customer_id = c.CustNo
        LEFT JOIN orders_without_invoice owi ON owi.CustNo = c.CustNo
        
        WHERE
            -- 🔥 головна умова
            COALESCE(owi.pending_orders, 0) > 0
            OR COALESCE(lb.balance, 0) > 0
    """, nativeQuery = true)
    List<CustomerBalanceProjection> findClientBalances(
            @Param("branches") Collection<Long> branches
    );


}
