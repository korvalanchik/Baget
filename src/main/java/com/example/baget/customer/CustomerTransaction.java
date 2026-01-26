package com.example.baget.customer;

import com.example.baget.orders.Orders;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "customer_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Клієнт ОБОВʼЯЗКОВИЙ */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** Замовлення не завжди є (аванс, баланс клієнта) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_no")
    private Orders order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CustomerTransactionType type;

    /**
     * Правило:
     * +  => клієнт винен більше
     * -  => клієнт винен менше
     */
    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(length = 50)
    private String reference; // чек, платіжка, номер інвойсу

    @Column(columnDefinition = "TEXT")
    private String note;

    /** Для soft-delete / відміни */
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @PrePersist
    public void prePersist() {
        createdAt = OffsetDateTime.now();
    }
}
