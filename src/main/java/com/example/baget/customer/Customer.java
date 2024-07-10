package com.example.baget.customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "Customers")
@Getter
@Setter
public class Customer {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long custNo;

    @Column(length = 30)
    private String company;

    @Column(length = 30)
    private String addr1;

    @Column(length = 30)
    private String addr2;

    @Column(length = 15)
    private String city;

    @Column(length = 20)
    private String state;

    @Column(length = 10)
    private String zip;

    @Column(length = 20)
    private String country;

    @Column(length = 15)
    private String phone;

    @Column(length = 15)
    private String fax;

    @Column
    private Double taxRate;

    @Column(length = 20)
    private String contact;

    @Column
    private OffsetDateTime lastInvoiceDate;

    @Column
    private Double priceLevel;

}
