package com.example.baget.customer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface CustomerBalanceProjection {

    Long getCustNo();
    String getCompany();
    String getPhone();

    BigDecimal getBalance();
    Long getInvoiceCount();
    OffsetDateTime getLastPaymentDate();

    Long getPendingOrders();
}