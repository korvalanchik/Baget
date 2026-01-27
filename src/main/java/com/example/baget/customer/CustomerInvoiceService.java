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
    public CustomerTransactionDTO issueInvoice(Long orderNo, String reference) {

        Orders order = ordersRepository.findById(orderNo)
                .orElseThrow(() -> new TransactionException("Замовлення не знайдено: " + orderNo));

        Customer customer = order.getCustomer();
        if (customer == null) {
            throw new IllegalStateException("Замовлення не має клієнта");
        }

        BigDecimal amount = order.getAmountDueN();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Некоректна сума замовлення: " + amount);
        }

        boolean invoiceExists = customerTxRepository
                .existsByOrder_OrderNoAndTypeAndActiveTrue(orderNo, CustomerTransactionType.INVOICE);

        if (invoiceExists) {
            throw new TransactionException("Рахунок уже виставлено для замовлення №" + orderNo);
        }

        CustomerTransaction tx = CustomerTxFactory.invoice(customer, order, amount, reference);

        return toDto(customerTxRepository.save(tx));
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

