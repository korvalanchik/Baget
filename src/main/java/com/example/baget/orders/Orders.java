package com.example.baget.orders;

import com.example.baget.customer.Customer;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "orders")
@Getter
@Setter
public class Orders {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custNo", nullable = false)
    private Customer customer;

    @Column
    private Integer factNo;

    @Column
    private OffsetDateTime saleDate;

    @Column
    private OffsetDateTime shipDate;

    @Column
    private Integer empNo;

    @Column(length = 20)
    private String shipToContact;

    @Column(length = 30)
    private String shipToAddr1;

    @Column(length = 30)
    private String shipToAddr2;

    @Column(length = 15)
    private String shipToCity;

    @Column(length = 20)
    private String shipToState;

    @Column(length = 10)
    private String shipToZip;

    @Column(length = 20)
    private String shipToCountry;

    @Column(length = 15)
    private String shipToPhone;

    @Column(length = 7)
    private String shipVia;

    @Column(length = 15)
    private String po;

    @Column(length = 6)
    private String terms;

    @Column(length = 7)
    private String paymentMethod;

    @Column
    private Double itemsTotal;

    @Column
    private Double taxRate;

    @Column
    private Double freight;

    @Column
    private Double amountPaid;

    @Column
    private Double amountDueN;

    @Column
    private Double income;

    @Column
    private Integer priceLevel;

    @Column
    private Integer statusOrder;

    @Column
    private Long rahFacNo;

}
