package com.example.baget.invoices;

import com.example.baget.orders.InvoiceDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    @SuppressWarnings("JpaQlInspection")
    @Query("""
    select new com.example.baget.invoices.InvoiceDetailsDTO(
        i.id,
        i.invoiceNo,
        i.totalAmount,
        coalesce(sum(case when ct.amount < 0 then -ct.amount else 0 end), 0),
        i.totalAmount - coalesce(sum(case when ct.amount < 0 then -ct.amount else 0 end), 0),
        i.createdAt
    )
    from Invoice i
    left join CustomerTransaction ct
        on ct.invoice = i
        and ct.active = true
    where i.id = :invoiceId
    group by i.id, i.invoiceNo, i.totalAmount, i.createdAt
    """)
    Optional<InvoiceDetailsDTO> findInvoiceDetails(@Param("invoiceId") Long invoiceId);

}
