package com.example.baget.customer;

import java.math.BigDecimal;

public record CustomerPaymentRequest(
        Long customerId,
        Long orderNo,
        BigDecimal amount,
        String reference
) {}
