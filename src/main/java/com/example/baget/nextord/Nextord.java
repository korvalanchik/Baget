package com.example.baget.nextord;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Table(name = "nextord")
@Entity
@Getter
@Setter
public class Nextord {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long newKey;

}
