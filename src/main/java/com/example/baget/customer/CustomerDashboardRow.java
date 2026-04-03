package com.example.baget.customer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface CustomerDashboardRow {

    Long getCustomerId();
    String getCompany();
    String getMobile();

    Integer getPendingOrders();
    Integer getInvoiceCount();

    BigDecimal getBalance();
    OffsetDateTime getLastPaymentDate();

    Integer getConsolidatedInvoices();
    BigDecimal getTotalTurnover();
}