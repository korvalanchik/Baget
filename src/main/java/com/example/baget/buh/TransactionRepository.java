package com.example.baget.buh;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByOrder_OrderNo(Long orderNo);
    @Query("SELECT COALESCE(SUM(t.amount), 0) " +
            "FROM Transaction t " +
            "WHERE t.customer.custNo = :custNo AND t.status = 'Completed'")
    Double getCustomerBalance(@Param("custNo") Long custNo);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.order.orderNo = :orderNo")
    Double sumPaidByOrder(@Param("orderNo") Long orderNo);

    List<TransactionHistoryView> findByOrder_RahFacNoOrderByTransactionDateDesc(Long invoiceNo);

}
