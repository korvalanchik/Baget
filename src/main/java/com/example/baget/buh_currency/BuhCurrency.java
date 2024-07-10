package com.example.baget.buh_currency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "BuhCurrencies")
@Getter
@Setter
public class BuhCurrency {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer currencyNo;

    @Column(nullable = false, columnDefinition = "longtext")
    private String currency;

}
