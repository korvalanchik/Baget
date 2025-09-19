package com.example.baget.buh;

import com.example.baget.orders.OrderPaySummaryDTO;

import java.util.List;

public record TransactionCollectiveInvoiceDTO(
        Long invoiceNo,
        List<OrderPaySummaryDTO> orders, // список замовлень у цьому рахунку
        Double totalBilled,           // нараховано
        Double totalPaid,             // сплачено
        Double totalDue,              // залишок
        Double totalCustomerBalance        // баланс клієнта
) {}
