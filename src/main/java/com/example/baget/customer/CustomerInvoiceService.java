package com.example.baget.customer;

import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.example.baget.util.TransactionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CustomerInvoiceService {

    private final CustomerTransactionRepository customerTxRepository;
    private final OrdersRepository ordersRepository;

    @Transactional
    public CustomerTransactionDTO issueInvoice(Long orderNo, CustomerIssueInvoiceRequestDTO request) {

        Orders order = ordersRepository.findById(orderNo)
                .orElseThrow(() -> new TransactionException("Замовлення не знайдено: " + orderNo));

        Customer customer = order.getCustomer();
        if (customer == null) {
            throw new TransactionException("Замовлення не має клієнта");
        }

        // 1️⃣ Оновлюємо order (статус + дата відвантаження)
        order.setStatusOrder(7); // INVOICED
        if (request.getShipDate() != null) {
            order.setShipDate(request.getShipDate());
        }

        // 2️⃣ Сума рахунку
        BigDecimal amount = request.getAmount() != null
                ? request.getAmount()
                : order.getAmountDueN();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Некоректна сума рахунку: " + amount);
        }

        // 3️⃣ Перевірка дублювання інвойсу
        boolean invoiceExists = customerTxRepository
                .existsByOrder_OrderNoAndTypeAndActiveTrue(orderNo, CustomerTransactionType.INVOICE);

        if (invoiceExists) {
            throw new TransactionException("Рахунок уже виставлено для замовлення №" + orderNo);
        }

        // 4️⃣ Створюємо customer transaction
        CustomerTransaction tx = CustomerTxFactory.invoice(customer, order, amount, request.getReference());

        customerTxRepository.save(tx);

        // 5️⃣ Зберігаємо order
        ordersRepository.save(order);

        return toDto(tx);
    }

    public static CustomerTransactionDTO toDto(CustomerTransaction tx) {
        return CustomerTransactionDTO.builder()
                .id(tx.getId())
                .customerId(tx.getCustomer().getCustNo())
                .orderNo(tx.getOrder() != null ? tx.getOrder().getOrderNo() : null)
                .type(tx.getType().name())
                .amount(tx.getAmount())
                .createdAt(tx.getCreatedAt())
                .reference(tx.getReference())
                .note(tx.getNote())
                .build();
    }


}

