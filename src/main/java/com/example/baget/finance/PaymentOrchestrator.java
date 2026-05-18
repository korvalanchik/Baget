package com.example.baget.finance;

import com.example.baget.branch.Branch;
import com.example.baget.branch.BranchRepository;
import com.example.baget.customer.*;
import com.example.baget.invoices.Invoice;
import com.example.baget.invoices.InvoicePaymentRequest;
import com.example.baget.invoices.InvoiceRepository;
import com.example.baget.ledger.*;
import com.example.baget.users.User;
import com.example.baget.users.UsersRepository;
import com.example.baget.util.TransactionException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentOrchestrator {

    private final List<PaymentProcessor> processors;
    private final LedgerService ledgerService;
    private final UsersRepository usersRepository;
    private final BranchRepository branchRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final LedgerRepository ledgerRepository;

    private static final List<LedgerCategory> INVOICE_OWNERSHIP_CATEGORIES = List.of(
            LedgerCategory.INVOICE_ISSUED,
            LedgerCategory.INVOICE_MERGE_OUT
    );

    @Transactional
    public List<CustomerTransactionDTO> processPayment(
            InvoicePaymentRequest request,
            Authentication authentication) {

        String username = authentication.getName();

        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new TransactionException("Користувач не знайдений: " + username));

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Сума оплати має бути більше 0");
        }

        OffsetDateTime now = OffsetDateTime.now();

        InvoicePaymentContext ctx = resolveContext(request, user);

        List<PaymentProcessor> matchedProcessors  = processors.stream()
                .filter(p -> p.supports(request))
                .toList();

        if (matchedProcessors .size() != 1) {
            throw new IllegalStateException("Expected exactly 1 processor, got " + matchedProcessors .size());
        }

        PaymentProcessor processor = matchedProcessors.get(0);

        List<CustomerTransaction> txs = processor.process(ctx, request, now);

        // 🔥 Ledger через один API
        for (CustomerTransaction tx : txs) {
            ledgerService.createEntry(buildLedgerRequest(ctx, tx, user, now, request.note()));
        }

        return txs.stream().map(this::toDTO).toList();
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


    private LedgerRequest buildLedgerRequest(
            InvoicePaymentContext ctx,
            CustomerTransaction tx,
            User user,
            OffsetDateTime now,
            String note) {

        LedgerCategory category =
                tx.getType().getLedgerCategory();

        return new LedgerRequest(
                ctx.branch(),
                LedgerDirection.IN,
                category,
                tx.getAmount(),
                now,
                user,

                ctx.debtor().getCustNo(),
                tx.getId(),
                ctx.payer(),
                ctx.invoice() != null ? ctx.invoice().getId() : null,

                buildReference(ctx, tx),
                note
        );
    }


    private String buildReference(InvoicePaymentContext ctx, CustomerTransaction tx) {

        if (tx.getType() == CustomerTransactionType.ADVANCE) {
            return "ADV-" + ctx.debtor().getCustNo();
        }

        return "PAY-" + ctx.invoice().getInvoiceNo();
    }

    private InvoicePaymentContext resolveContext(InvoicePaymentRequest request, User user) {

        if (request.invoiceId() != null) {

            Invoice invoice = invoiceRepository.findById(request.invoiceId())
                    .orElseThrow(() -> new TransactionException("Інвойс не знайдено"));

            Branch branch = resolveInvoiceBranch(invoice.getId());

            validateBranchAccess(user, branch.getBranchNo());

            return new InvoicePaymentContext(
                    user,
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

        return new InvoicePaymentContext(user, branch, customer, customer, null);
    }

    private void validateBranchAccess(User user, Long branchNo) {

        boolean allowed = user.getAllowedBranches()
                .stream()
                .anyMatch(b -> b.getBranchNo().equals(branchNo));

        if (!allowed) {
            throw new TransactionException("Немає доступу до філії: " + branchNo);
        }
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

}