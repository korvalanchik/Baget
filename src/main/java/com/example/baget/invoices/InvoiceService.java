package com.example.baget.invoices;

import com.example.baget.customer.*;
import com.example.baget.ledger.LedgerCategory;
import com.example.baget.ledger.LedgerDirection;
import com.example.baget.ledger.LedgerEntry;
import com.example.baget.ledger.LedgerRepository;
import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.example.baget.users.User;
import com.example.baget.users.UsersRepository;
import com.example.baget.util.TransactionException;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrdersRepository ordersRepository;
    private final CustomerRepository customerRepository;
    private final CustomerTransactionRepository customerTxRepository;
    private final UsersRepository usersRepository;
    private final InvoiceOrderRepository invoiceOrderRepository;
    private final LedgerRepository ledgerRepository;
    private final InvoiceMapper invoiceMapper;
    private final EntityManager entityManager;

    @Transactional
    public InvoiceDTO createInvoiceForOrders(CustomerIssueInvoiceRequestDTO request, Authentication authentication) {
        String username = authentication.getName();

        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new TransactionException("Користувач з таким ім'ям відсутній: " + username));

        List<Long> orderNos = request.getOrderNos();
        if (orderNos == null || orderNos.isEmpty()) {
            throw new TransactionException("Список замовлень порожній");
        }

        // 1️⃣ Завантажуємо всі замовлення
        List<Orders> orders = ordersRepository.findAllById(orderNos);
        if (orders.size() != orderNos.size()) {
            throw new TransactionException("Деякі замовлення не знайдено");
        }

        // 2️⃣ Визначаємо платника рахунку
        Customer invoiceCustomer;

        if (request.getInvoiceCustomerId() != null) {
            invoiceCustomer = customerRepository.findById(request.getInvoiceCustomerId())
                    .orElseThrow(() -> new TransactionException("Платника рахунку не знайдено"));
        } else {
            Set<Long> customerIds = orders.stream()
                    .map(o -> o.getCustomer().getCustNo())
                    .collect(Collectors.toSet());
            if (customerIds.size() > 1) {
                throw new TransactionException(
                        "MULTIPLE_CUSTOMERS",
                        "Для рахунку з кількома клієнтами потрібно вибрати корпоративного платника"
                );
            }
            invoiceCustomer = orders.get(0).getCustomer();
            if (invoiceCustomer == null) {
                throw new TransactionException("Замовлення не має клієнта");
            }
        }

        // 3️⃣ Перевірка, чи жодне замовлення не в іншому invoice
        boolean alreadyInvoiced = invoiceOrderRepository.existsByOrder_OrderNoIn(orderNos);
        if (alreadyInvoiced) {
            throw new TransactionException("Одне або декілька замовлень вже включені в рахунок");
        }

        // 4️⃣ Створюємо Invoice
        if (orders.stream().anyMatch(o -> o.getAmountDueN() == null)) {
            throw new TransactionException("Замовлення ще не має фінальної суми");
        }
        BigDecimal totalAmount = orders.stream()
                .map(Orders::getAmountDueN)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long invoiceNo = generateTodayCode();
        while (invoiceRepository.existsByInvoiceNo(invoiceNo)) {
            invoiceNo++;
        }

        OffsetDateTime now = OffsetDateTime.now();

        Invoice invoice = Invoice.builder()
                .invoiceNo(invoiceNo)
                .customer(invoiceCustomer)
                .type((orders.size() == 1) ? InvoiceEnums.InvoiceType.SIMPLE : InvoiceEnums.InvoiceType.CONSOLIDATED)
                .status(InvoiceEnums.InvoiceStatus.ISSUED)
                .totalAmount(totalAmount)
                .note(request.getReference())
                .build();

        invoiceRepository.save(invoice);
        entityManager.flush();

        // 5️⃣ Створюємо InvoiceOrder для кожного замовлення
        for (Orders order : orders) {
            InvoiceOrder io = new InvoiceOrder();
            io.setInvoice(invoice);
            io.setOrder(order);
            io.setAmount(order.getAmountDueN());

            // 6️⃣ Оновлюємо order
            order.setStatusOrder(8);
            order.setShipDate(request.getShipDate() != null ? request.getShipDate() : now);

            invoiceOrderRepository.save(io);
        }

        // 🔹 баланс клієнта
        BigDecimal balance = getCustomerBalance(invoiceCustomer.getCustNo());

        if (balance.compareTo(BigDecimal.ZERO) > 0) {

            // скільки можна списати
            BigDecimal amountToApply = balance.min(totalAmount);

            // 🔥 створюємо списання
            ledgerRepository.save(
                    LedgerEntry.builder()
                            .branch(orders.get(0).getBranch()) // або з контексту
                            .direction(LedgerDirection.OUT)
                            .category(LedgerCategory.APPLY_ADVANCE_TO_INVOICE)
                            .amount(amountToApply)
                            .createdAt(now)
                            .createdBy(user)

                            .customerId(invoiceCustomer.getCustNo())
                            .invoiceId(invoice.getId())

                            .reference("APPLY_ADV-" + invoice.getInvoiceNo())
                            .note("Списання авансу")
                            .build()
            );

            customerTxRepository.save(
                    CustomerTransaction.builder()
                            .customer(invoiceCustomer)
                            .branch(orders.get(0).getBranch())
                            .invoice(invoice)
                            .type(CustomerTransactionType.ADVANCE_APPLIED)
                            .amount(amountToApply.negate()) // мінус
                            .createdAt(now)
                            .note("Списання авансу на інвойс №" + invoice.getInvoiceNo())
                            .build()
            );

            // 🔥 оновлюємо статус інвойсу
            BigDecimal remaining = totalAmount.subtract(amountToApply);

            if (remaining.compareTo(BigDecimal.ZERO) == 0) {
                invoice.setStatus(InvoiceEnums.InvoiceStatus.PAID);
            } else {
                invoice.setStatus(InvoiceEnums.InvoiceStatus.PARTIALLY_PAID);
            }
        }

        return invoiceMapper.toDto(invoice);
    }

    public BigDecimal getCustomerBalance(Long customerId) {
        BigDecimal result = ledgerRepository.getCustomerBalance(customerId);
        return result != null ? result : BigDecimal.ZERO;
    }

        public InvoiceDetailsDTO getInvoice(Long invoiceId) {
        return invoiceRepository.findInvoiceDetails(invoiceId)
                .orElseThrow(() -> new TransactionException("Invoice not found"));
    }


    private Long generateTodayCode() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        String result = today.format(formatter) + "001";
        return Long.parseLong(result);
    }
}
