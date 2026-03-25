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
                .orElseThrow(() -> new TransactionException("Користувач не знайдений: " + username));

        List<Long> orderNos = request.getOrderNos();
        if (orderNos == null || orderNos.isEmpty()) {
            throw new TransactionException("Список замовлень порожній");
        }

        // 1️⃣ Завантажуємо замовлення
        List<Orders> orders = ordersRepository.findAllById(orderNos);
        if (orders.size() != orderNos.size()) {
            throw new TransactionException("Деякі замовлення не знайдено");
        }

        // 2️⃣ Визначаємо платника (invoiceCustomer)
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
                        "Для рахунку з кількома клієнтами потрібно вибрати платника"
                );
            }

            invoiceCustomer = orders.get(0).getCustomer();
        }

        // 3️⃣ Перевірка на дублювання
        boolean alreadyInvoiced = invoiceOrderRepository.existsByOrder_OrderNoIn(orderNos);
        if (alreadyInvoiced) {
            throw new TransactionException("Одне або декілька замовлень вже в інвойсі");
        }

        // 4️⃣ Рахуємо суму
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Orders o : orders) {
            if (o.getAmountDueN() == null) {
                throw new TransactionException("Замовлення " + o.getOrderNo() + " без фінальної суми");
            }
            totalAmount = totalAmount.add(o.getAmountDueN());
        }

        // 5️⃣ Створюємо invoice
        Long invoiceNo = generateTodayCode();
        while (invoiceRepository.existsByInvoiceNo(invoiceNo)) {
            invoiceNo++;
        }

        OffsetDateTime now = OffsetDateTime.now();

        Invoice invoice = Invoice.builder()
                .invoiceNo(invoiceNo)
                .customer(invoiceCustomer) // платник
                .type((orders.size() == 1)
                        ? InvoiceEnums.InvoiceType.SIMPLE
                        : InvoiceEnums.InvoiceType.CONSOLIDATED)
                .status(InvoiceEnums.InvoiceStatus.ISSUED)
                .totalAmount(totalAmount)
                .note(request.getReference())
                .build();

        invoiceRepository.save(invoice);
        entityManager.flush();

        // 🔥 6️⃣ InvoiceOrder + створення боргу (ledger OUT)
        for (Orders order : orders) {

            BigDecimal amount = order.getAmountDueN();

            // InvoiceOrder
            InvoiceOrder io = new InvoiceOrder();
            io.setInvoice(invoice);
            io.setOrder(order);
            io.setAmount(amount);
            invoiceOrderRepository.save(io);

            // Оновлення order
            order.setStatusOrder(8);
            order.setShipDate(request.getShipDate() != null ? request.getShipDate() : now);

            // 🔥 BORROW = OUT НА ВЛАСНИКА ЗАМОВЛЕННЯ
            ledgerRepository.save(
                    LedgerEntry.builder()
                            .branch(order.getBranch())
                            .direction(LedgerDirection.OUT)
                            .category(LedgerCategory.INVOICE_ISSUED)
                            .amount(amount)
                            .createdAt(now)
                            .createdBy(user)

                            .customerId(order.getCustomer().getCustNo()) // 🔥 ВАЖЛИВО
                            .orderId(order.getOrderNo())
                            .invoiceId(invoice.getId())

                            .reference("INV-" + invoice.getInvoiceNo())
                            .note("Нарахування по інвойсу")
                            .build()
            );
        }

        // 🔥 7️⃣ Списання авансу (якщо є)
        BigDecimal advanceBalance = getCustomerBalance(invoiceCustomer.getCustNo());

        if (advanceBalance.compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal amountToApply = advanceBalance.min(totalAmount);

            Long txId = customerTxRepository.save(
                    CustomerTransaction.builder()
                            .customer(invoiceCustomer)
                            .branch(orders.get(0).getBranch())
                            .invoice(invoice)
                            .type(CustomerTransactionType.ADVANCE_APPLIED)
                            .amount(amountToApply.negate())
                            .createdAt(now)
                            .note("Списання авансу на інвойс №" + invoice.getInvoiceNo())
                            .build()
            ).getId();

            BigDecimal remaining = amountToApply;

            // 🔥 РОЗКИДАЄМО ПО ЗАМОВЛЕННЯХ
            for (Orders order : orders) {

                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal orderAmount = order.getAmountDueN();
                BigDecimal apply = remaining.min(orderAmount);

                ledgerRepository.save(
                        LedgerEntry.builder()
                                .branch(order.getBranch())
                                .direction(LedgerDirection.OUT)
                                .category(LedgerCategory.APPLY_ADVANCE_TO_INVOICE)
                                .amount(apply)
                                .createdAt(now)
                                .createdBy(user)

                                // 🔥 НА ВЛАСНИКА ЗАМОВЛЕННЯ
                                .customerId(order.getCustomer().getCustNo())
                                .orderId(order.getOrderNo())
                                .invoiceId(invoice.getId())
                                .customerTransactionId(txId)
                                .reference("APPLY_ADV-" + invoice.getInvoiceNo())
                                .note("Списання авансу")
                                .build()
                );

                remaining = remaining.subtract(apply);
            }

            // 🔥 Оновлюємо статус
            if (amountToApply.compareTo(totalAmount) >= 0) {
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
