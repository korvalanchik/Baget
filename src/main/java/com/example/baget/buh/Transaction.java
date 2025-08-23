package com.example.baget.buh;

import com.example.baget.orders.Orders;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_no", nullable = false)
    private Orders order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    private TransactionType transactionType;

    @Column(name = "transaction_date")
    private OffsetDateTime transactionDate = OffsetDateTime.now();

    @Column(nullable = false)
    private Double amount;

    @Column(length = 50)
    private String reference;

    @Column(length = 20)
    private String status; // Pending, Completed, Canceled

    @Column(columnDefinition = "TEXT")
    private String note;
}
