package com.example.baget.buh;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transaction_types")
@Getter
@Setter
@NoArgsConstructor
public class TransactionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long typeId;

    @Column(nullable = false, unique = true, length = 20)
    private String code; // INVOICE, PAYMENT, REFUND

    @Column(length = 100)
    private String description;
}
