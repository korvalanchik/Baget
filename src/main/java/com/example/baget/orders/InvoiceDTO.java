package com.example.baget.orders;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record InvoiceDTO(
        Long invoiceNo,
        Long anyOrderNo,
        BigDecimal totalCost,
        BigDecimal amountPaid,
        BigDecimal amountDue,
        OffsetDateTime saleDate
) {}
