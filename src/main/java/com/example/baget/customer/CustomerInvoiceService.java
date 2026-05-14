package com.example.baget.customer;

import com.example.baget.invoices.*;
import com.example.baget.ledger.*;
import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.example.baget.orders.OrdersService;
import com.example.baget.users.User;
import com.example.baget.users.UsersRepository;
import com.example.baget.util.InvoiceServiceUtil;
import com.example.baget.util.TransactionException;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CustomerInvoiceService {

    private final CustomerTransactionRepository customerTxRepository;
    private final OrdersRepository ordersRepository;
    private final UsersRepository usersRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceOrderRepository invoiceOrderRepository;
    private final EntityManager entityManager;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceServiceUtil invoiceServiceUtil;
    private final OrdersService ordersService;
    private final LedgerService ledgerService;

    @Transactional
    public InvoiceDTO issueInvoice(Long orderNo, IssueInvoiceFullRequest request, Authentication authentication) {

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
        if (invoiceOrderRepository.existsByOrder_OrderNoAndInvoice_Lifecycle(orderNo, InvoiceEnums.InvoiceLifecycle.ACTIVE)) {
            throw new TransactionException("Замовлення вже в активному інвойсі");
        }

        // 3️⃣ Сума
        BigDecimal amount = request.getInvoice().getAmount() != null
                ? request.getInvoice().getAmount()
                : order.getAmountDueN();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Некоректна сума рахунку: " + amount);
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 4️⃣ Генеруємо номер інвойсу
        Long invoiceNo = invoiceServiceUtil.generateTodayCode();
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
                .note(request.getInvoice().getReference())
                .build();

        invoiceRepository.save(invoice);
        entityManager.flush();

        // 6️⃣ Створюємо InvoiceOrder
        InvoiceOrder io = new InvoiceOrder();
        io.setInvoice(invoice);
        io.setOrder(order);
        io.setAmount(amount);

        invoiceOrderRepository.save(io);

        CustomerTransactionType type = CustomerTransactionType.INVOICE;

        // 7️⃣ CustomerTransaction (для UI/історії)
        CustomerTransaction tx = saveAndFlush(
             CustomerTransaction.builder()
                .customer(customer)
                .order(order)
                .branch(order.getBranch())
                .invoice(invoice)
                .type(type)
                .amount(amount.negate()) // 🔥 борг = мінус
                .createdAt(now)
                .note("Інвойс №" + invoiceNo)
                .build()
        );

        // 8️⃣ Ledger → борг клієнта
        ledgerService.createEntry(
                new LedgerRequest(
                        order.getBranch(),
                        type.getDirection(),          // 🔥 з enum
                        type.getLedgerCategory(),     // 🔥 з enum
                        amount,
                        now,
                        user,

                        customer.getCustNo(),
                        tx.getId(),
                        null,
                        invoice.getId(),

                        "INV-" + invoiceNo,
                        "Виставлення інвойсу"
                )
        );
//        ledgerRepository.save(
//                LedgerEntry.builder()
//                        .branch(order.getBranch())
//                        .direction(LedgerDirection.OUT) // 🔥 борг
//                        .category(LedgerCategory.INVOICE_ISSUED)
//                        .amount(amount)
//                        .createdAt(now)
//                        .createdBy(user)
//                        .customerId(customer.getCustNo())
//                        .customerTransactionId(tx.getId())
//                        .orderId(order.getOrderNo())
//                        .invoiceId(invoice.getId())
//                        .reference("INV-" + invoiceNo)
//                        .note("Виставлення інвойсу")
//                        .build()
//        );

        // 9️⃣ Оновлюємо order
        ordersService.update(orderNo, request.getOrder());

        return invoiceMapper.toDto(invoice);
    }

    private CustomerTransaction saveAndFlush(CustomerTransaction tx) {
        customerTxRepository.save(tx);
        entityManager.flush();
        return tx;
    }

}

