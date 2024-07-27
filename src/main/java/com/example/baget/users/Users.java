package com.example.baget.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Table(name = "users")
@Entity
@Getter
@Setter
public class Users {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String salt;

}
