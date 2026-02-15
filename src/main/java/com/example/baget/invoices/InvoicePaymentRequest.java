package com.example.baget.invoices;

import java.math.BigDecimal;

public record InvoicePaymentRequest(
        BigDecimal amount,
        String note
) {}
