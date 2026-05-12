package com.example.baget.finance;

import com.example.baget.branch.Branch;
import com.example.baget.branch.BranchRepository;
import com.example.baget.customer.*;
import com.example.baget.invoices.*;
import com.example.baget.ledger.LedgerCategory;
import com.example.baget.ledger.LedgerDirection;
import com.example.baget.ledger.LedgerEntry;
import com.example.baget.ledger.LedgerRepository;
import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.example.baget.users.User;
import com.example.baget.users.UsersRepository;
import com.example.baget.util.TransactionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinanceTransactionService {

    private final CustomerTransactionRepository customerTxRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceOrderRepository invoiceOrderRepository;
    private final LedgerRepository ledgerRepository;
    private final BranchRepository branchRepository;
    private final UsersRepository usersRepository;
    private final OrdersRepository ordersRepository;

    private static final List<LedgerCategory> INVOICE_OWNERSHIP_CATEGORIES = List.of(
            LedgerCategory.INVOICE_ISSUED,
            LedgerCategory.INVOICE_MERGE_OUT
    );

    @Transactional
    public List<CustomerTransactionDTO> registerPayment(
            InvoicePaymentRequest request,
            Authentication authentication) {

        String username = authentication.getName();

        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new TransactionException("Користувач не знайдений: " + username));

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Сума оплати має бути більше 0");
        }

        OffsetDateTime now = OffsetDateTime.now();

        PaymentContext ctx = resolveContext(request, user);

        List<CustomerTransaction> transactions = new ArrayList<>();

        if (ctx.invoice() == null) {
            // 🔥 ADVANCE ONLY
            transactions.add(createAdvance(ctx, request.amount(), now, request.note()));
        } else {
            // 🔥 PAYMENT FLOW
            transactions.addAll(handleInvoicePayment(ctx, request.amount(), now, request.note()));
        }

        // 🔥 LEDGER (ВАЖЛИВО: розбиваємо)
        createLedgerEntries(ctx, transactions, request.amount(), now, user, request.note());

        return transactions.stream().map(this::toDTO).toList();
    }


    private CustomerTransactionDTO toDTO(CustomerTransaction tx) {
        return CustomerTransactionDTO.builder()
                .id(tx.getId())
                .customerId(tx.getCustomer().getCustNo())
                .invoiceId(tx.getInvoice() != null ? tx.getInvoice().getId() : null)
                .orderNo(tx.getOrder() != null ? tx.getOrder().getOrderNo() : null)
                .amount(tx.getAmount())
                .type(tx.getType())
                .parentTransactionId(tx.getParentTransactionId()) // <-- додаємо тут
                .note(tx.getNote())
                .createdAt(tx.getCreatedAt())
                .build();
    }


    private Branch resolveInvoiceBranch(Long invoiceId) {
        return ledgerRepository
                .findTopByInvoiceIdAndDirectionAndCategoryInOrderByCreatedAtDescIdDesc(
                        invoiceId,
                        LedgerDirection.OUT,
                        INVOICE_OWNERSHIP_CATEGORIES
                )
                .map(LedgerEntry::getBranch)
                .orElseThrow(() -> new TransactionException(
                        "Не знайдено ownership OUT для інвойсу " + invoiceId
                ));
    }


    private PaymentContext resolveContext(InvoicePaymentRequest request, User user) {

        if (request.invoiceId() != null) {

            Invoice invoice = invoiceRepository.findById(request.invoiceId())
                    .orElseThrow(() -> new TransactionException("Інвойс не знайдено"));

            Branch branch = resolveInvoiceBranch(invoice.getId());

            validateBranchAccess(user, branch.getBranchNo());

            return new PaymentContext(
                    branch,
                    invoice.getCustomer(),
                    invoice.getEffectivePayer(),
                    invoice
            );
        }

        if (request.branchNo() == null || request.customerId() == null) {
            throw new TransactionException("Для авансу потрібні branch + customer");
        }

        validateBranchAccess(user, request.branchNo());

        Branch branch = branchRepository.findById(request.branchNo())
                .orElseThrow(() -> new TransactionException("Філія не знайдена"));

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new TransactionException("Клієнт не знайдений"));

        return new PaymentContext(branch, customer, customer, null);
    }



    private void updateInvoiceAndOrder(
            PaymentContext ctx,
            BigDecimal paid,
            BigDecimal totalDebtBeforePayment) {

        Invoice invoice = ctx.invoice();

        // ----------------------------
        // 1️⃣ Оновлюємо Orders (тільки SIMPLE)
        // ----------------------------
        if (invoice.getType() == InvoiceEnums.InvoiceType.SIMPLE) {

            List<InvoiceOrder> invoiceOrders =
                    invoiceOrderRepository.findByInvoice_Id(invoice.getId());

            if (invoiceOrders.isEmpty()) {
                throw new TransactionException("Інвойс не містить замовлень");
            }

            Orders order = invoiceOrders.get(0).getOrder();

            if (order != null) {

                BigDecimal newAmountPaid = safe(order.getAmountPaid()).add(paid);
                BigDecimal newAmountDue = safe(order.getAmountDueN()).subtract(paid);

                order.setAmountPaid(newAmountPaid);
                order.setAmountDueN(newAmountDue);
                order.setIncome(safe(order.getIncome()).add(paid));

                // 🔥 статус замовлення
                if (newAmountDue.compareTo(BigDecimal.ZERO) <= 0) {
                    order.setStatusOrder(4); // CLOSED
                } else {
                    order.setStatusOrder(9); // PARTIAL
                }

                ordersRepository.save(order);
            }
        }

        // ----------------------------
        // 2️⃣ Оновлюємо Invoice
        // ----------------------------
        BigDecimal remainingDebt = totalDebtBeforePayment.subtract(paid);

        if (remainingDebt.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(InvoiceEnums.InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceEnums.InvoiceStatus.PARTIALLY_PAID);
        }

        // ⚠️ save не обов’язковий якщо є persistence context,
        // але краще явно для читабельності
        invoiceRepository.save(invoice);
    }


    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    public BigDecimal calculateInvoiceDebt(Long invoiceId) {

        return ledgerRepository.calculateInvoiceDebt(invoiceId); // борг
    }


    private List<CustomerTransaction> handleInvoicePayment(
            PaymentContext ctx,
            BigDecimal amount,
            OffsetDateTime now,
            String note) {

        List<CustomerTransaction> result = new ArrayList<>();

        BigDecimal totalDebt = calculateInvoiceDebt(ctx.invoice().getId());

        BigDecimal paid = amount.min(totalDebt);
        BigDecimal overpay = amount.subtract(paid);

        // ✅ PAYMENT
        if (paid.compareTo(BigDecimal.ZERO) > 0) {
            result.add(createPayment(ctx, paid, now, note));
            updateInvoiceAndOrder(ctx, paid, totalDebt);
        }

        // ✅ OVERPAY → ADVANCE
        if (overpay.compareTo(BigDecimal.ZERO) > 0) {
            result.add(createAdvance(ctx, overpay, now,
                    "Переплата інвойсу " + ctx.invoice().getInvoiceNo()));
        }

        return result;
    }


    private CustomerTransaction createPayment(
            PaymentContext ctx,
            BigDecimal amount,
            OffsetDateTime now,
            String note) {

        return customerTxRepository.save(
                CustomerTransaction.builder()
                        .branch(ctx.branch())
                        .customer(ctx.debtor())
                        .invoice(ctx.invoice())
                        .type(CustomerTransactionType.PAYMENT)
                        .amount(amount)
                        .createdAt(now)
                        .note(note)
                        .build()
        );
    }


    private CustomerTransaction createAdvance(
            PaymentContext ctx,
            BigDecimal amount,
            OffsetDateTime now,
            String note) {

        return customerTxRepository.save(
                CustomerTransaction.builder()
                        .branch(ctx.branch())
                        .customer(ctx.debtor())
                        .type(CustomerTransactionType.ADVANCE)
                        .amount(amount)
                        .createdAt(now)
                        .note(note)
                        .build()
        );
    }


    private void createLedgerEntries(
            PaymentContext ctx,
            List<CustomerTransaction> txs,
            BigDecimal totalAmount,
            OffsetDateTime now,
            User user,
            String note) {

        for (CustomerTransaction tx : txs) {

            LedgerCategory category =
                    tx.getType() == CustomerTransactionType.ADVANCE
                            ? LedgerCategory.CUSTOMER_ADVANCE
                            : LedgerCategory.PAYMENT_RECEIVED;

            ledgerRepository.save(
                    LedgerEntry.builder()
                            .branch(ctx.branch())
                            .direction(LedgerDirection.IN)
                            .category(category)
                            .amount(tx.getAmount())
                            .createdAt(now)
                            .createdBy(user)

                            .customerId(ctx.debtor().getCustNo())
                            .payer(ctx.payer())

                            .invoiceId(ctx.invoice() != null ? ctx.invoice().getId() : null)
                            .customerTransactionId(tx.getId()) // 🔥 тепер є зв’язок

                            .reference(buildReference(ctx, tx))
                            .note(note)
                            .build()
            );
        }
    }


    private String buildReference(PaymentContext ctx, CustomerTransaction tx) {

        if (tx.getType() == CustomerTransactionType.ADVANCE) {
            return "ADV-" + ctx.debtor().getCustNo();
        }

        return "PAY-" + ctx.invoice().getInvoiceNo();
    }


    private void validateBranchAccess(User user, Long branchNo) {

        boolean allowed = user.getAllowedBranches()
                .stream()
                .anyMatch(b -> b.getBranchNo().equals(branchNo));

        if (!allowed) {
            throw new TransactionException("Немає доступу до філії: " + branchNo);
        }
    }



}
