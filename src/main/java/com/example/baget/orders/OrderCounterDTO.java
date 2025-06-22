package com.example.baget.orders;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class OrderCounterDTO extends BaseOrdersDTO {

    private Double amountPaid;

    private Double amountDueN;

    private Double totalCost;

}
