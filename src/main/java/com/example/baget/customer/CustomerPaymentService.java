package com.example.baget.customer;

import com.example.baget.finance.FinanceCategory;
import com.example.baget.finance.FinanceDirection;
import com.example.baget.finance.FinanceTransaction;
import com.example.baget.finance.FinanceTransactionRepository;
import com.example.baget.invoices.Invoice;
import com.example.baget.invoices.InvoiceEnums;
import com.example.baget.invoices.InvoiceRepository;
import com.example.baget.users.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerPaymentService {

    private final CustomerTransactionRepository customerTxRepository;
    private final FinanceTransactionRepository financeTxRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public void registerPayment(CustomerPaymentRequest dto, User user) {

        Customer customer = customerRepository.findById(dto.customerId())
                .orElseThrow();

        Invoice invoice = invoiceRepository.findById(dto.invoiceId())
                .orElseThrow();

        if (!invoice.getCustomer().getCustNo().equals(customer.getCustNo())) {
            throw new IllegalArgumentException("Invoice does not belong to customer");
        }

        if (dto.amount() == null || dto.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        BigDecimal debt = calculateDebt(invoice);

        if (dto.amount().compareTo(debt) > 0) {
            throw new IllegalArgumentException("Payment exceeds invoice debt");
        }

        // 1️⃣ клієнтський баланс
        CustomerTransaction customerTx = customerTxRepository.save(
                CustomerTransaction.builder()
                        .customer(customer)
                        .invoice(invoice)
                        .type(CustomerTransactionType.PAYMENT)
                        .amount(dto.amount().negate()) // мінус
                        .createdAt(OffsetDateTime.now())
                        .reference(dto.reference())
                        .build()
        );

        // 2️⃣ фінанси компанії
        financeTxRepository.save(
                FinanceTransaction.builder()
                        .direction(FinanceDirection.IN)
                        .category(FinanceCategory.CUSTOMER_PAYMENT)
                        .amount(dto.amount())
                        .createdAt(OffsetDateTime.now())
                        .customerTransactionId(customerTx.getId())
                        .createdBy(user)
                        .reference(dto.reference())
                        .build()
        );

        // 3️⃣ перераховуємо борг після платежу
        BigDecimal newDebt = calculateDebt(invoice);

        if (newDebt.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(InvoiceEnums.InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceEnums.InvoiceStatus.PARTIALLY_PAID);
        }
    }

    public List<CustomerPaymentDTO> getPaymentsByInvoice(Long invoiceId) {
        return customerTxRepository.findPaymentsByInvoiceId(invoiceId);
    }

    public BigDecimal calculateDebt(Invoice invoice) {

        BigDecimal total = invoice.getTotalAmount();

        BigDecimal txSum = customerTxRepository.sumTransactionsByInvoice(invoice);

        // txSum буде від’ємним для оплат
        return total.add(txSum);
    }

}
