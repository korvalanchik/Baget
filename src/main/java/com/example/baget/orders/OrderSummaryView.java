package com.example.baget.orders;

import org.springframework.beans.factory.annotation.Value;

import java.time.OffsetDateTime;

public interface OrderSummaryView {
    Long getOrderNo();
    Integer getStatusOrder();
    OffsetDateTime getSaleDate();

    @Value("#{target.branch.name}")
    String getBranchName();

}
