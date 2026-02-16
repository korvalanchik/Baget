package com.example.baget.invoices;

import java.math.BigDecimal;

public record InvoicePaymentRequest(
        Long branchNo,
        BigDecimal amount,
        String note
) {}
