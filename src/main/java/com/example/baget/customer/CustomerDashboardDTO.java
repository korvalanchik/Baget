package com.example.baget.customer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class CustomerDashboardDTO {

    public record Response(
            List<WithoutInvoice> withoutInvoice,
            List<Debtor> debtors,
            List<Payer> payers
    ) {}

    public record WithoutInvoice(
            Long customerId,
            String company,
            String mobile,
            Integer pendingOrders
    ) {}

    public record Debtor(
            Long customerId,
            String company,
            String mobile,
            BigDecimal balance,
            Integer invoiceCount,
            OffsetDateTime lastPaymentDate
    ) {}

    public record Payer(
            Long customerId,
            String company,
            String mobile,
            Integer consolidatedInvoices,
            BigDecimal totalTurnover
    ) {}
}
