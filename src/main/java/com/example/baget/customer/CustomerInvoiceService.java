package com.example.baget.customer;

import com.example.baget.invoices.*;
import com.example.baget.ledger.LedgerCategory;
import com.example.baget.ledger.LedgerDirection;
import com.example.baget.ledger.LedgerEntry;
import com.example.baget.ledger.LedgerRepository;
import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.example.baget.users.User;
import com.example.baget.users.UsersRepository;
import com.example.baget.util.TransactionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class CustomerInvoiceService {

    private final CustomerTransactionRepository customerTxRepository;
    private final OrdersRepository ordersRepository;
    private final UsersRepository usersRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceOrderRepository invoiceOrderRepository;
    private final LedgerRepository ledgerRepository;
    private final EntityManager entityManager;
    private final InvoiceMapper invoiceMapper;

    @Transactional
    public InvoiceDTO issueInvoice(Long orderNo, CustomerIssueInvoiceRequestDTO request, Authentication authentication) {

        String username = authentication.getName();

        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new TransactionException("Користувач не знайдений: " + username));

        // 1️⃣ Отримуємо order
        Orders order = ordersRepository.findById(orderNo)
                .orElseThrow(() -> new TransactionException("Замовлення не знайдено: " + orderNo));

        Customer customer = order.getCustomer();
        if (customer == null) {
            throw new TransactionException("Замовлення не має клієнта");
        }

        // 2️⃣ Перевірка: чи вже є інвойс
        boolean alreadyInvoiced = invoiceOrderRepository.existsByOrder_OrderNo(orderNo);
        if (alreadyInvoiced) {
            throw new TransactionException("Інвойс вже створений для замовлення №" + orderNo);
        }

        // 3️⃣ Сума
        BigDecimal amount = request.getAmount() != null
                ? request.getAmount()
                : order.getAmountDueN();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Некоректна сума рахунку: " + amount);
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 4️⃣ Генеруємо номер інвойсу
        Long invoiceNo = generateTodayCode();
        while (invoiceRepository.existsByInvoiceNo(invoiceNo)) {
            invoiceNo++;
        }

        // 5️⃣ Створюємо Invoice
        Invoice invoice = Invoice.builder()
                .invoiceNo(invoiceNo)
                .customer(customer)
                .type(InvoiceEnums.InvoiceType.SIMPLE)
                .status(InvoiceEnums.InvoiceStatus.ISSUED)
                .lifecycle(InvoiceEnums.InvoiceLifecycle.ACTIVE)
                .totalAmount(amount)
                .note(request.getReference())
                .build();

        invoiceRepository.save(invoice);
        entityManager.flush();

        // 6️⃣ Створюємо InvoiceOrder
        InvoiceOrder io = new InvoiceOrder();
        io.setInvoice(invoice);
        io.setOrder(order);
        io.setAmount(amount);

        invoiceOrderRepository.save(io);

        // 7️⃣ Ledger → борг клієнта
        ledgerRepository.save(
                LedgerEntry.builder()
                        .branch(order.getBranch())
                        .direction(LedgerDirection.OUT) // 🔥 борг
                        .category(LedgerCategory.INVOICE_ISSUED)
                        .amount(amount)
                        .createdAt(now)
                        .createdBy(user)
                        .customerId(customer.getCustNo())
                        .orderId(order.getOrderNo())
                        .invoiceId(invoice.getId())
                        .reference("INV-" + invoiceNo)
                        .note("Виставлення інвойсу")
                        .build()
        );

        // 8️⃣ CustomerTransaction (для UI/історії)
        CustomerTransaction tx = CustomerTransaction.builder()
                .customer(customer)
                .branch(order.getBranch())
                .invoice(invoice)
                .type(CustomerTransactionType.INVOICE)
                .amount(amount.negate()) // 🔥 борг = мінус
                .createdAt(now)
                .note("Інвойс №" + invoiceNo)
                .build();

        customerTxRepository.save(tx);

        // 9️⃣ Оновлюємо order
        order.setStatusOrder(7); // INVOICED
        if (request.getShipDate() != null) {
            order.setShipDate(request.getShipDate());
        }

        ordersRepository.save(order);

        return invoiceMapper.toDto(invoice);
    }

    private Long generateTodayCode() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        String result = today.format(formatter) + "001";
        return Long.parseLong(result);
    }

}

