package com.example.baget.customer;

import java.math.BigDecimal;
import java.util.List;

public record CustomerFinanceDTO(
        String customerName,
        String customerPhone,

        BigDecimal balance,
        BigDecimal totalDebt,

        List<CustomerInvoiceDTO> invoices,
        List<CustomerLedgerDTO> ledger
) {}
