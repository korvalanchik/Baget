package com.example.baget.finance;

import com.example.baget.customer.CustomerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, Long> {
}
