package com.example.baget.invoices;

import com.example.baget.customer.*;
import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.example.baget.util.TransactionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
    private final InvoiceOrderRepository invoiceOrderRepository;
    private final CustomerTransactionRepository customerTransactionRepository;
    private final InvoiceMapper invoiceMapper;

    @Transactional
    public InvoiceDTO createInvoiceForOrders(CustomerIssueInvoiceRequestDTO request) {

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
                throw new TransactionException("Для рахунку з кількома клієнтами потрібно вибрати корпоративного платника");
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
        BigDecimal totalAmount = orders.stream()
                .map(Orders::getAmountDueN)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Long invoiceNo = generateTodayCode();
        while (invoiceRepository.existsByInvoiceNo(invoiceNo)) {
            invoiceNo++;
        }

        Invoice invoice = Invoice.builder()
                .invoiceNo(invoiceNo)
                .customer(invoiceCustomer)
                .type((orders.size() == 1) ? InvoiceEnums.InvoiceType.SIMPLE : InvoiceEnums.InvoiceType.CONSOLIDATED)
                .status(InvoiceEnums.InvoiceStatus.ISSUED)
                .totalAmount(totalAmount)
                .note(request.getReference())
                .build();

        invoiceRepository.save(invoice);

        // 5️⃣ Створюємо InvoiceOrder для кожного замовлення
        for (Orders order : orders) {
            InvoiceOrder io = new InvoiceOrder();
            io.setInvoice(invoice);
            io.setOrder(order);
            io.setAmount(order.getAmountDueN());
            invoiceOrderRepository.save(io);

            // 6️⃣ Оновлюємо order
            order.setStatusOrder(8);
            order.setShipDate(request.getShipDate() != null ? request.getShipDate() : OffsetDateTime.now());

            ordersRepository.save(order);

            // 7️⃣ CustomerTransaction
            CustomerTransaction tx = CustomerTxFactory.invoice(
                    invoiceCustomer,
                    order,
                    order.getAmountDueN(),
                    String.valueOf(invoice.getInvoiceNo())
            );
            customerTransactionRepository.save(tx);
        }

        return invoiceMapper.toDto(invoice);
    }

    private Long generateTodayCode() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        String result = today.format(formatter) + "001";
        return Long.parseLong(result);
    }
}
