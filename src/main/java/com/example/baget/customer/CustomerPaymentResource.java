package com.example.baget.customer;

import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.example.baget.users.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/customers")
public class CustomerPaymentResource {

    private final CustomerPaymentService customerPaymentService;
    private final CustomerTransactionService customerTransactionService;
    private final CustomerRepository customerRepository;
    private final OrdersRepository ordersRepository;

    public CustomerPaymentResource(CustomerPaymentService customerPaymentService, CustomerTransactionService customerTransactionService, CustomerRepository customerRepository, OrdersRepository ordersRepository) {
        this.customerPaymentService = customerPaymentService;
        this.customerTransactionService = customerTransactionService;
        this.customerRepository = customerRepository;
        this.ordersRepository = ordersRepository;
    }

    @PostMapping("/payment")
    public ResponseEntity<Void> registerPayment(
            @RequestBody CustomerPaymentRequest dto,
            @AuthenticationPrincipal User user
    ) {

        Customer customer = customerRepository.findById(dto.customerId())
                .orElseThrow();

        Orders order = dto.orderNo() != null
                ? ordersRepository.findById(dto.orderNo()).orElse(null)
                : null;

        customerPaymentService.registerPayment(
                customer,
                order,
                dto.amount(),
                dto.reference(),
                user
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/balance")
    public BigDecimal getBalance(@PathVariable Long id) {
        return customerTransactionService.getCustomerBalance(id);
    }

}
