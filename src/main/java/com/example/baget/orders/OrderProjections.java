package com.example.baget.orders;

import com.example.baget.items.ItemsDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.List;

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

        OffsetDateTime getSaleDate();
        @JsonProperty("branchName")
        String getBranch_Name(); // Orders.branch.name

        OffsetDateTime getShipDate();

        @JsonProperty("empNo")
        String getEmployee_Username();

        Double getItemsTotal();

    }

    // для адміну
    interface AdminOrderView extends BaseOrdersView {
        Integer getStatusOrder();
    }

    // для касира
    interface CounterOrderView extends BaseOrdersView {
        // успадковує базові поля
    }

    // для звичайного користувача
    interface UserOrderView extends BaseOrdersView {
        // тільки базові поля
    }
}
