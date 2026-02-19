package com.example.baget.ledger;

import com.example.baget.branch.Branch;
import com.example.baget.users.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "ledger_entries",
        indexes = {
                @Index(name = "idx_ledger_branch_date", columnList = "branch_id, created_at"),
                @Index(name = "idx_ledger_customer", columnList = "customer_id"),
                @Index(name = "idx_ledger_order", columnList = "order_id"),
                @Index(name = "idx_ledger_invoice", columnList = "invoice_id"),
                @Index(name = "idx_ledger_category", columnList = "category"),
                @Index(name = "idx_ledger_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔹 Філія (обовʼязково)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    // 🔹 Напрям руху
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LedgerDirection direction;

    // 🔹 Категорія операції
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LedgerCategory category;

    // 🔹 Сума (завжди позитивна)
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    // 🔹 Дата створення запису
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // 🔹 Хто створив
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // 🔹 Опціональні зв’язки
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(length = 100)
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String note;
}
