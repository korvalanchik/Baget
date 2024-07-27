package com.example.baget.items;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Table(name = "items")
@Entity
@Getter
@Setter
public class Items {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderNo;

    @Column(nullable = false)
    private Long itemNo;

    @Column
    private Long partNo;

    @Column
    private Double profilWidth;

    @Column
    private Double width;

    @Column
    private Double height;

    @Column
    private Double qty;

    @Column
    private Double quantity;

    @Column
    private Double sellPrice;

    @Column
    private Double discount;

    @Column
    private Double onHand;

    @Column
    private Double cost;

}
