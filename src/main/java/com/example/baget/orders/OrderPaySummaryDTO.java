package com.example.baget.orders;

public record OrderPaySummaryDTO(
        Long orderNo,
        Double billed,
        Double paid,
        Double due
) {}
