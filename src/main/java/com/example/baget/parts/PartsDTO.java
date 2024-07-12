package com.example.baget.parts;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PartsDTO {

    private Long partNo;

    private Double vendorNo;

    @Size(max = 30)
    private String description;

    private Double profilWidth;

    private Double inQuality;

    private Double onHand;

    private Double onOrder;

    private Double cost;

    private Double listPrice;

    private Double listPrice1;

    private Double listPrice2;

    private Integer noPercent;

    private Double listPrice3;

}
