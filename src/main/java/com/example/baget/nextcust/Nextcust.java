package com.example.baget.nextcust;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Table(name = "nextcust")
@Entity
@Getter
@Setter
public class Nextcust {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long newCust;

}
