package com.example.baget.invoices;

import com.example.baget.orders.InvoiceDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByInvoiceNo(Long invoiceNo);

    @SuppressWarnings("JpaQlInspection")
    @Query("""
        select new com.example.baget.orders.InvoiceDTO(
            i.invoiceNo,
            min(io.order.orderNo),
            i.totalAmount,
            coalesce(sum(io.amount), 0),
            i.totalAmount - coalesce(sum(io.amount), 0),
            i.createdAt
        )
        from Invoice i
        left join i.invoiceOrders io
        where i.customer.custNo = :custNo
        group by i.id, i.invoiceNo, i.totalAmount, i.createdAt
        order by i.createdAt desc
    """)
    List<InvoiceDTO> findInvoicesByCustomer(@Param("custNo") Long custNo);


}
