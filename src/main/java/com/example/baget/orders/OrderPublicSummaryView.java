package com.example.baget.orders;

import java.time.OffsetDateTime;

public interface OrderPublicSummaryView {
    Long getOrderNo();     // Номер рахунку
    OffsetDateTime getSaleDate();     // Дата створення/виписки
    Double getItemsTotal();   // Загальна сума рахунку
}
