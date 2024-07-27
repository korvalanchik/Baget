package com.example.baget.nextitem;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Table(name = "nextitem")
@Entity
@Getter
@Setter
public class Nextitem {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long newKey;

}
