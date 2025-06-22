package com.example.baget.orders;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class OrderAdminDTO extends BaseOrdersDTO {

    private Double taxRate;

    private Double amountPaid;

    private Double amountDueN;

    private Double income;

    private Double totalCost;

    private Integer priceLevel;

}
