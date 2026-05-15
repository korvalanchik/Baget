package com.example.baget.finance;

import com.example.baget.customer.CustomerTransaction;
import com.example.baget.invoices.InvoicePaymentRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class InvoicePaymentProcessor implements PaymentProcessor {

    @Override
    public boolean supports(InvoicePaymentRequest request) {
        return request.invoiceId() != null;
    }

    @Override
    public List<CustomerTransaction> process(
            PaymentContext ctx,
            InvoicePaymentRequest request,
            OffsetDateTime now
    ) {

        // твоя логіка оплати

        return List.of();
    }
}