package com.example.baget.customer;

import com.example.baget.branch.Branch;
import com.example.baget.invoices.Invoice;
import com.example.baget.invoices.InvoiceFinanceService;
import com.example.baget.util.TransactionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AdvanceAllocationService {

    private final CustomerTransactionRepository customerTxRepository;
    private final InvoiceFinanceService invoiceFinanceService;

    @Transactional
    public void allocateAdvance(
            Invoice invoice,
            Branch branch,
            BigDecimal requestedAmount,
            String note
    ) {

        if (requestedAmount == null
                || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new TransactionException("Сума алокації повинна бути більше 0");
        }

        Customer customer = invoice.getCustomer();

        BigDecimal availableAdvance = customerTxRepository.calculateAvailableAdvance(customer.getCustNo());

        if (availableAdvance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("У клієнта відсутній доступний аванс");
        }

        if (requestedAmount.compareTo(availableAdvance) > 0) {
            throw new TransactionException("Сума алокації перевищує доступний аванс");
        }

        BigDecimal invoiceDebt = invoiceFinanceService.calculateInvoiceDebt(invoice);

        if (invoiceDebt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Інвойс вже оплачено");
        }

        BigDecimal allocationAmount = requestedAmount.min(invoiceDebt);

        CustomerTransaction tx =
                customerTxRepository.save(
                        CustomerTransaction.builder()
                                .customer(customer)
                                .branch(branch)
                                .invoice(invoice)
                                .type(CustomerTransactionType.ADVANCE_ALLOCATION)
                                .amount(allocationAmount.negate())
                                .note(note)
                                .build()
                );

//        invoiceFinanceService.refreshInvoiceStatus(invoice);

    }
}
