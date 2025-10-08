package com.example.baget.orders;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public interface OrderProjections {
    // базові поля для всіх користувачів
    interface BaseOrdersView {
        Long getOrderNo();

        @JsonProperty("custNo")
        Long getCustomer_CustNo();

        @JsonProperty("company")
        String getCustomer_Company();

        @JsonProperty("phone")
        String getCustomer_Mobile();

        @JsonProperty("addr1")
        String getCustomer_Addr1();

        @JsonProperty("branchName")
        String getBranch_Name(); // Orders.branch.name

        OffsetDateTime getSaleDate();

        OffsetDateTime getShipDate();

        @JsonProperty("empNo")
        String getEmployee_Username();

        Double getItemsTotal();

        Integer getStatusOrder();

    }

    // для адміну
    interface AdminOrderView extends BaseOrdersView {
        Double getTaxRate();
        Double getFreight();
        Double getAmountPaid();
        Double getAmountDueN();
        Double getIncome();
        Double getTotalCost();
        Integer getPriceLevel();
    }

    // для касира
    interface CounterOrderView extends BaseOrdersView {
        Double getAmountPaid();
    }

    // для звичайного користувача
    interface UserOrderView extends BaseOrdersView {
        String getNotice();
    }

    interface PublicOrderView {
        Long getOrderNo();     // Номер рахунку
        OffsetDateTime getSaleDate();     // Дата створення/виписки
        Double getItemsTotal();   // Загальна сума рахунку
    }
    interface PrivateOrderView extends PublicOrderView{
        @JsonProperty("company")
        String getCustomer_Company();

        @JsonProperty("phone")
        String getCustomer_Mobile();
        @JsonProperty("branchName")
        String getBranch_Name(); // Orders.branch.name
        OffsetDateTime getShipDate();
        @JsonProperty("empNo")
        String getEmployee_Username();
        Integer getStatusOrder();
        String getNotice();
    }

}
