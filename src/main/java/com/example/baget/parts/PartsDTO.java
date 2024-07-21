package com.example.baget.parts;

import com.example.baget.vendors.VendorsDTO;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class PartsDTO {

    private Long partNo;

    private Long vendorNo;

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

    private Long version;

}
