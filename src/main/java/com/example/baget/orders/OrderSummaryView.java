package com.example.baget.orders;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public interface OrderSummaryView {
    Long getOrderNo();
    Integer getStatusOrder();
    OffsetDateTime getSaleDate();
    @JsonProperty("branchName")
    String getBranch_Name();

}
