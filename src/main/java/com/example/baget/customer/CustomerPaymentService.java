package com.example.baget.customer;

import com.example.baget.finance.FinanceCategory;
import com.example.baget.finance.FinanceDirection;
import com.example.baget.finance.FinanceTransaction;
import com.example.baget.finance.FinanceTransactionRepository;
import com.example.baget.invoices.Invoice;
import com.example.baget.invoices.InvoiceEnums;
import com.example.baget.invoices.InvoicePaymentRequest;
import com.example.baget.invoices.InvoiceRepository;
import com.example.baget.users.User;
import com.example.baget.util.TransactionException;
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
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public CustomerTransactionDTO registerInvoicePayment(Long invoiceId, InvoicePaymentRequest request, User user) {

        // 1️⃣ Завантажуємо інвойс разом з клієнтом
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new TransactionException("Інвойс не знайдено: " + invoiceId));

        Customer customer = invoice.getCustomer(); // вже в persistence context

        // 2️⃣ Перевіряємо суму
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        BigDecimal debt = calculateDebt(invoice);
        if (request.amount().compareTo(debt) > 0) {
            throw new IllegalArgumentException("Payment exceeds invoice debt");
        }

        // 3️⃣ Створюємо CustomerTransaction
        CustomerTransaction customerTx = customerTxRepository.save(
                CustomerTransaction.builder()
                        .customer(customer)
                        .invoice(invoice)
                        .type(CustomerTransactionType.PAYMENT)
                        .amount(request.amount().negate()) // мінус
                        .createdAt(OffsetDateTime.now())
                        .note(request.note())
                        .build()
        );

        // 4️⃣ Створюємо FinanceTransaction
        financeTxRepository.save(
                FinanceTransaction.builder()
                        .direction(FinanceDirection.IN)
                        .category(FinanceCategory.CUSTOMER_PAYMENT)
                        .amount(request.amount())
                        .createdAt(OffsetDateTime.now())
                        .customerTransactionId(customerTx.getId())
                        .createdBy(user)
                        .reference("INV-" + invoice.getInvoiceNo())
                        .build()
        );

        // 5️⃣ Оновлюємо статус інвойсу
        BigDecimal newDebt = calculateDebt(invoice);
        if (newDebt.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(InvoiceEnums.InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceEnums.InvoiceStatus.PARTIALLY_PAID);
        }

        // 6️⃣ Повертаємо DTO для фронтенду
        return CustomerTransactionDTO.builder()
                .id(customerTx.getId())
                .invoiceId(invoice.getId())
                .amount(customerTx.getAmount())
                .createdAt(customerTx.getCreatedAt())
                .note(customerTx.getNote())
                .reference("INV-" + invoice.getInvoiceNo())
                .build();
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
