package com.example.baget.customer;

import com.example.baget.finance.FinanceCategory;
import com.example.baget.finance.FinanceDirection;
import com.example.baget.finance.FinanceTransaction;
import com.example.baget.finance.FinanceTransactionRepository;
import com.example.baget.orders.Orders;
import com.example.baget.users.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CustomerPaymentService {

    private final CustomerTransactionRepository customerTxRepository;
    private final FinanceTransactionRepository financeTxRepository;

    @Transactional
    public void registerPayment(Customer customer, Orders order, BigDecimal amount, String reference, User user) {

        // 1️⃣ клієнтський баланс
        CustomerTransaction customerTx = customerTxRepository.save(
                CustomerTransaction.builder()
                        .customer(customer)
                        .order(order)
                        .type(CustomerTransactionType.PAYMENT)
                        .amount(amount.negate()) // мінус
                        .createdAt(OffsetDateTime.now())
                        .reference(reference)
                        .build()
        );

        // 2️⃣ фінанси компанії
        financeTxRepository.save(
                FinanceTransaction.builder()
                        .direction(FinanceDirection.IN)
                        .category(FinanceCategory.CUSTOMER_PAYMENT)
                        .amount(amount)
                        .createdAt(OffsetDateTime.now())
                        .customerTransactionId(customerTx.getId())
                        .createdBy(user)
                        .reference(reference)
                        .build()
        );
    }
}
