package com.example.baget.customer;

import com.example.baget.finance.InvoiceOperationContext;
import com.example.baget.invoices.InvoiceEnums;
import com.example.baget.invoices.InvoiceFinanceService;
import com.example.baget.invoices.InvoiceOrder;
import com.example.baget.invoices.InvoiceOrderRepository;
import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.example.baget.util.TransactionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdvanceAllocationService {

    private final CustomerTransactionRepository customerTxRepository;
    private final InvoiceFinanceService invoiceFinanceService;
    private final InvoiceOrderRepository invoiceOrderRepository;
    private final OrdersRepository ordersRepository;
    private final CustomerTransactionMapper customerTransactionMapper;

    @Transactional
    public CustomerTransactionDTO allocateAdvance(
            InvoiceOperationContext ctx,
            BigDecimal requestedAmount,
            String note
    ) {

        if (requestedAmount == null
                || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new TransactionException("Сума алокації повинна бути більше 0");
        }

        Customer customer = ctx.invoice().getCustomer();

        BigDecimal availableAdvance = customerTxRepository.calculateAvailableAdvance(customer.getCustNo());

        if (availableAdvance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("У клієнта відсутній доступний аванс");
        }

        if (requestedAmount.compareTo(availableAdvance) > 0) {
            throw new TransactionException("Сума алокації перевищує доступний аванс");
        }

        BigDecimal invoiceDebt = invoiceFinanceService.calculateInvoiceDebt(ctx.invoice());

        if (invoiceDebt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Інвойс вже оплачено");
        }

        BigDecimal allocationAmount = requestedAmount.min(invoiceDebt);

        CustomerTransaction tx =
                customerTxRepository.save(
                        CustomerTransaction.builder()
                                .customer(customer)
                                .branch(ctx.branch())
                                .invoice(ctx.invoice())
                                .type(CustomerTransactionType.ADVANCE_ALLOCATION)
                                .amount(allocationAmount.negate())
                                .note(note)
                                .build()
                );

        // ----------------------------
        // SIMPLE invoice → update order
        // ----------------------------
        List<InvoiceOrder> invoiceOrders =
                invoiceOrderRepository.findByInvoice_Id(ctx.invoice().getId());

        if (ctx.invoice().getType() == InvoiceEnums.InvoiceType.SIMPLE) {

            Orders order = invoiceOrders.get(0).getOrder();

            if (order != null) {

                BigDecimal newAmountPaid = order.getAmountPaid().add(allocationAmount);
                BigDecimal newAmountDueN = order.getAmountDueN().subtract(allocationAmount);

                order.setAmountPaid(newAmountPaid);
                order.setAmountDueN(newAmountDueN);
                order.setIncome(order.getIncome().add(allocationAmount));

                if (newAmountDueN.compareTo(BigDecimal.ZERO) <= 0) {
                    order.setStatusOrder(4);
                } else {
                    order.setStatusOrder(9);
                }

                ordersRepository.save(order);
            }
        }

        invoiceFinanceService.refreshInvoiceStatus(ctx.invoice());

        return customerTransactionMapper.toDTO(tx);
    }
}
