package com.example.baget.invoices;

import java.math.BigDecimal;

public record InvoicePaymentRequest(
        Long branchNo,
        Long customerId,
        BigDecimal amount,
        String note
) {}
