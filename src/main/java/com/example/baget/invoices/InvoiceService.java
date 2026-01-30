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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrdersRepository ordersRepository;
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

        // 2️⃣ Беремо клієнта першого замовлення
        Customer customer = orders.get(0).getCustomer();
        if (customer == null) {
            throw new TransactionException("Замовлення не має клієнта");
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
                .customer(customer)
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
            order.setStatusOrder(7);
            if (request.getShipDate() != null) {
                order.setShipDate(request.getShipDate());
            }
            ordersRepository.save(order);

            // 7️⃣ CustomerTransaction
            CustomerTransaction tx = CustomerTxFactory.invoice(
                    customer,
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
