package com.example.baget.buh;

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
@Table(name = "Buhs")
@Getter
@Setter
public class Buh {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer count;

    @Column(nullable = false)
    private OffsetDateTime date;

    @Column(nullable = false)
    private Integer account;

    @Column(nullable = false)
    private Integer category;

    @Column(nullable = false)
    private Integer total;

    @Column(nullable = false)
    private Integer currency;

    @Column(
            nullable = false,
            name = "\"description\"",
            columnDefinition = "longtext"
    )
    private String description;

    @Column
    private Integer transfer;

}
