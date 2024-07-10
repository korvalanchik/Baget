package com.example.baget.vendors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "Vendorses")
@Getter
@Setter
public class Vendors {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vendorNo;

    @Column(length = 30)
    private String vendorName;

    @Column(length = 30)
    private String address1;

    @Column(length = 30)
    private String address2;

    @Column(length = 20)
    private String city;

    @Column(length = 20)
    private String state;

    @Column(length = 10)
    private String zip;

    @Column(length = 15)
    private String country;

    @Column(length = 15)
    private String phone;

    @Column(length = 15)
    private String fax;

    @Column
    private Integer preferred;

}
