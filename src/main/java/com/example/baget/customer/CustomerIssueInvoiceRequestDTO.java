package com.example.baget.customer;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class CustomerIssueInvoiceRequestDTO {
    private BigDecimal amount; // опціонально (якщо хочеш дозволити корекцію)
    private String reference;
    private OffsetDateTime shipDate;
}
