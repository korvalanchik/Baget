package com.example.baget.orders;

import com.example.baget.branch.Branch;
import com.example.baget.customer.Customer;
import com.example.baget.items.Items;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "orders")
@Getter
@Setter
public class Orders {

    @Id
    @Column(name = "OrderNo", nullable = false, updatable = false)
    private Long orderNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "CustNo", nullable = false)
    private Customer customer;

    @OneToMany(mappedBy = "order", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Items> items;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "BranchNo")
    private Branch branch;

//    @Column(name = "BranchNo")
//    private Long branchNo;

    @Column(name = "SaleDate")
    private OffsetDateTime saleDate;

    @Column(name = "ShipDate")
    private OffsetDateTime shipDate;

    @Column(name = "EmpNo")
    private Long empNo;

    @Column(name = "ShipToContact", length = 20)
    private String shipToContact;

    @Column(name = "ShipToAddr1", length = 30)
    private String shipToAddr1;

    @Column(name = "ShipToAddr2", length = 30)
    private String shipToAddr2;

    @Column(name = "ShipToCity", length = 15)
    private String shipToCity;

    @Column(name = "ShipToState", length = 20)
    private String shipToState;

    @Column(name = "ShipToZip", length = 10)
    private String shipToZip;

    @Column(name = "ShipToCountry", length = 20)
    private String shipToCountry;

    @Column(name = "ShipToPhone", length = 15)
    private String shipToPhone;

    @Column(name = "ShipVia", length = 7)
    private String shipVia;

    @Column(name = "Po", length = 15)
    private String po;

    @Column(name = "Terms", length = 6)
    private String terms;

    @Column(name = "PaymentMethod", length = 7)
    private String paymentMethod;

    @Column(name = "ItemsTotal")
    private Double itemsTotal;

    @Column(name = "TaxRate")
    private Double taxRate;

    @Column(name = "Freight")
    private Double freight;

    @Column(name = "AmountPaid")
    private Double amountPaid;

    @Column(name = "AmountDueN")
    private Double amountDueN;

    @Column(name = "Income")
    private Double income;

    @Column(name = "TotalCost")
    private Double totalCost;

    @Column(name = "PriceLevel")
    private Integer priceLevel;

    @Column(name = "StatusOrder")
    private Integer statusOrder;

    @Column(name = "RahFacNo")
    private Long rahFacNo;

    @Column(name = "Notice")
    private String notice;

}
