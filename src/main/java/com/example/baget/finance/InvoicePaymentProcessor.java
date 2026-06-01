package com.example.baget.finance;

import com.example.baget.branch.Branch;
import com.example.baget.customer.Customer;
import com.example.baget.customer.CustomerTransaction;
import com.example.baget.customer.CustomerTransactionRepository;
import com.example.baget.customer.CustomerTransactionType;
import com.example.baget.invoices.*;
import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.example.baget.util.TransactionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoicePaymentProcessor implements PaymentProcessor {

    private final CustomerTransactionRepository customerTxRepository;
    private final InvoiceOrderRepository invoiceOrderRepository;
    private final OrdersRepository ordersRepository;

    @Override
    public boolean supports(InvoicePaymentRequest request) {
        return request.invoiceId() != null;
    }

    @Override
    @Transactional
    public List<CustomerTransaction> process(
            InvoicePaymentContext ctx,
            InvoicePaymentRequest request,
            OffsetDateTime now
    ) {

        BigDecimal paymentAmount = request.amount();

        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Сума оплати має бути більше 0");
        }

        Invoice invoice = ctx.invoice();
        Branch branch = ctx.branch();
        Customer debtor = ctx.debtor();

        BigDecimal totalDebt = calculateInvoiceDebt(invoice);

        List<InvoiceOrder> invoiceOrders =
                invoiceOrderRepository.findByInvoice_Id(invoice.getId());

        if (invoiceOrders.isEmpty()) {
            throw new TransactionException("Інвойс не містить замовлень");
        }

        List<CustomerTransaction> result = new ArrayList<>();

        // ----------------------------
        // Invoice already paid
        // ----------------------------
        if (totalDebt.compareTo(BigDecimal.ZERO) <= 0) {

            CustomerTransaction advanceTx = customerTxRepository.save(
                    CustomerTransaction.builder()
                            .branch(branch)
                            .customer(debtor)
                            .type(CustomerTransactionType.ADVANCE)
                            .amount(paymentAmount)
                            .createdAt(now)
                            .note("Інвойс вже оплачено → аванс")
                            .build()
            );

            result.add(advanceTx);

        } else {

            BigDecimal paid = paymentAmount.min(totalDebt);
            BigDecimal overpay = paymentAmount.subtract(paid);

            // ----------------------------
            // PAYMENT
            // ----------------------------
            CustomerTransaction paymentTx = customerTxRepository.save(
                    CustomerTransaction.builder()
                            .branch(branch)
                            .customer(debtor)
                            .invoice(invoice)
                            .type(CustomerTransactionType.PAYMENT)
                            .amount(paid)
                            .createdAt(now)
                            .note(request.note())
                            .build()
            );

            result.add(paymentTx);

            // ----------------------------
            // SIMPLE invoice → update order
            // ----------------------------
            if (invoice.getType() == InvoiceEnums.InvoiceType.SIMPLE) {

                Orders order = invoiceOrders.get(0).getOrder();

                if (order != null) {

                    BigDecimal newAmountPaid = order.getAmountPaid().add(paid);
                    BigDecimal newAmountDueN = order.getAmountDueN().subtract(paid);

                    order.setAmountPaid(newAmountPaid);
                    order.setAmountDueN(newAmountDueN);
                    order.setIncome(order.getIncome().add(paid));

                    if (newAmountDueN.compareTo(BigDecimal.ZERO) <= 0) {
                        order.setStatusOrder(4);
                    } else {
                        order.setStatusOrder(9);
                    }

                    ordersRepository.save(order);
                }
            }

            // ----------------------------
            // OVERPAY → ADVANCE
            // ----------------------------
            if (overpay.compareTo(BigDecimal.ZERO) > 0) {

                CustomerTransaction advanceTx = customerTxRepository.save(
                        CustomerTransaction.builder()
                                .branch(branch)
                                .customer(debtor)
                                .type(CustomerTransactionType.ADVANCE)
                                .amount(overpay)
                                .createdAt(now)
                                .note("Переплата інвойсу " + invoice.getInvoiceNo())
                                .build()
                );

                result.add(advanceTx);
            }

            // ----------------------------
            // Invoice status
            // ----------------------------
            BigDecimal remainingDebt = totalDebt.subtract(paid);

            if (remainingDebt.compareTo(BigDecimal.ZERO) <= 0) {
                invoice.setStatus(InvoiceEnums.InvoiceStatus.PAID);
            } else {
                invoice.setStatus(InvoiceEnums.InvoiceStatus.PARTIALLY_PAID);
            }
        }

        return result;
    }

    public BigDecimal calculateInvoiceDebt(Invoice invoice) {
        Long invoiceId = invoice.getId();

        BigDecimal paid = customerTxRepository.totalPayments(invoiceId);

        BigDecimal allocated = customerTxRepository.totalAdvanceAllocated(invoiceId);

        return invoice.getTotalAmount()
                .subtract(paid)
                .subtract(allocated)
                .max(BigDecimal.ZERO);
    }

}