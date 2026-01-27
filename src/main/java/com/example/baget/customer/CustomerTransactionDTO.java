package com.example.baget.customer;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
public class CustomerTransactionDTO {
    private Long id;
    private Long customerId;
    private Long orderNo;
    private String type;
    private BigDecimal amount;
    private OffsetDateTime createdAt;
    private String reference;
    private String note;
}
