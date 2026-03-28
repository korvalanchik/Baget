package com.example.baget.invoices;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceOrderRepository extends JpaRepository<InvoiceOrder, Long> {
    boolean existsByOrder_OrderNoIn(List<Long> orderNos);

    List<InvoiceOrder> findByInvoice_Id(Long id);

    boolean existsByOrder_OrderNo(Long orderNo);
}
