package com.example.baget.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@AllArgsConstructor
public class CustomerBalanceDTO {

    private Long custNo;
    private String name;
    private String phone;

    private Long invoiceCount;

    private BigDecimal balance;

    private OffsetDateTime lastPaymentDate;
}
