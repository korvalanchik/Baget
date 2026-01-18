package com.example.baget.finance;

import com.example.baget.users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class FinanceTransactionService {

    private final FinanceTransactionRepository financeTransactionRepository;

    public FinanceTransaction registerExpense(
            FinanceCategory category,
            BigDecimal amount,
            String reference,
            User user
    ) {
        return financeTransactionRepository.save(
                FinanceTransaction.builder()
                        .direction(FinanceDirection.OUT)
                        .category(category)
                        .amount(amount)
                        .createdAt(OffsetDateTime.now())
                        .reference(reference)
                        .createdBy(user)
                        .build()
        );
    }
}
