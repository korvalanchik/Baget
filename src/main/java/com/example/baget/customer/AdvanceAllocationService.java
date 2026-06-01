package com.example.baget.customer;

import com.example.baget.branch.Branch;
import com.example.baget.invoices.Invoice;
import com.example.baget.util.TransactionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdvanceAllocationService {
    private final CustomerTransactionRepository customerTxRepository;

    @Transactional
    public CustomerTransaction allocateAdvance(
            Invoice invoice,
            Branch branch,
            BigDecimal amount,
            String note) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Сума розподілення авансу має бути більше 0");
        }

        Customer customer = invoice.getEffectivePayer();

        CustomerTransaction tx =
                CustomerTransaction.builder()
                        .customer(customer)
                        .branch(branch)
                        .invoice(invoice)
                        .type(CustomerTransactionType.ADVANCE_ALLOCATION)
                        .amount(amount.negate())
                        .note(note)
                        .build();

        return customerTxRepository.save(tx);
    }
}
