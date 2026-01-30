package com.example.baget.invoices;

public class InvoiceEnums {
    public enum InvoiceType {
        SIMPLE,        // рахунок на одне замовлення
        CONSOLIDATED   // рахунок-фактура на кілька замовлень
    }

    public enum InvoiceStatus {
        DRAFT,
        ISSUED,
        PAID,
        CANCELLED
    }

}
