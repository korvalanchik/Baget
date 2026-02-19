package com.example.baget.customer;

import com.example.baget.invoices.InvoicePaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers")
public class CustomerPaymentResource {

    private final CustomerPaymentService customerPaymentService;
    private final CustomerTransactionService customerTransactionService;

    @PostMapping("/invoices/{id}/payments")
    public ResponseEntity<CustomerTransactionDTO> addPayment(
            @PathVariable Long id,
            @RequestBody InvoicePaymentRequest request,
            Authentication authentication
    ) {
        // делегуємо всю логіку в сервіс
        CustomerTransactionDTO dto = customerPaymentService.registerInvoicePayment(id, request, authentication);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/balance")
    public BigDecimal getBalance(@PathVariable Long id) {
        return customerTransactionService.getCustomerBalance(id);
    }

    @GetMapping("/payments")
    public List<CustomerPaymentDTO> getPayments(
            @RequestParam Long invoiceId
    ) {
        return customerPaymentService.getPaymentsByInvoice(invoiceId);
    }
}
