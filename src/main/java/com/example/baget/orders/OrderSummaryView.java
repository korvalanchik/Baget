package com.example.baget.orders;

import java.time.OffsetDateTime;

public interface OrderSummaryView {
    Long getOrderNo();
    Integer getStatusOrder();
    OffsetDateTime getSaleDate();

}
