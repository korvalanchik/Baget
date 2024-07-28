package com.example.baget.items;

import com.example.baget.orders.Orders;
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
    private Long itemNo;

    @ManyToOne
    @JoinColumn(name = "orderNo", nullable = false)
    private Orders order;

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
