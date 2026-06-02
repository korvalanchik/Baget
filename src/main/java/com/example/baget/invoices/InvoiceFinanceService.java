package com.example.baget.invoices;

import com.example.baget.customer.CustomerTransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class InvoiceFinanceService {

    private final CustomerTransactionRepository customerTxRepository;
    private final InvoiceRepository invoiceRepository;

    public BigDecimal calculateInvoiceDebt(Invoice invoice) {

        Long invoiceId = invoice.getId();

        BigDecimal paid = customerTxRepository.totalPayments(invoiceId);

        BigDecimal allocated = customerTxRepository.totalAdvanceAllocated(invoiceId);

        return invoice.getTotalAmount()
                .subtract(paid)
                .subtract(allocated)
                .max(BigDecimal.ZERO);
    }

    @Transactional
    public void refreshInvoiceStatus(Invoice invoice) {

        BigDecimal debt =
                calculateInvoiceDebt(invoice);

        if (debt.compareTo(BigDecimal.ZERO) == 0) {

            invoice.setStatus(
                    InvoiceEnums.InvoiceStatus.PAID);

        } else if (
                debt.compareTo(invoice.getTotalAmount()) < 0) {

            invoice.setStatus(
                    InvoiceEnums.InvoiceStatus.PARTIALLY_PAID);

        } else {

            invoice.setStatus(
                    InvoiceEnums.InvoiceStatus.ISSUED);
        }

        invoiceRepository.save(invoice);
    }
}