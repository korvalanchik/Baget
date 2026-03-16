package com.example.baget.ledger;

import com.example.baget.customer.CustomerLedgerDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {
    @SuppressWarnings("JpaQlInspection")
    @Query("""
    SELECT new com.example.baget.customer.CustomerLedgerDTO(
        le.createdAt,
        le.direction,
        le.amount,
        le.category,
        le.note
    )
    FROM LedgerEntry le
    WHERE le.customerId = :customerId
    ORDER BY le.createdAt DESC
    """)
    List<CustomerLedgerDTO> findCustomerLedger(Long customerId);
}
