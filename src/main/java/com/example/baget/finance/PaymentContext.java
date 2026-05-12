package com.example.baget.finance;

import com.example.baget.branch.Branch;
import com.example.baget.customer.Customer;
import com.example.baget.invoices.Invoice;

public record PaymentContext(
        Branch branch,
        Customer debtor,
        Customer payer,
        Invoice invoice
) {}
