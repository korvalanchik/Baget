package com.example.baget.customer;

import com.example.baget.branch.Branch;
import com.example.baget.branch.BranchRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerPaymentService {

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
    public List<CustomerTransactionDTO> registerInvoicePayment(
            InvoicePaymentRequest request,
            Authentication authentication) {

        String username = authentication.getName();

        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new TransactionException("Користувач не знайдений: " + username));

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Сума оплати має бути більше 0");
        }

        OffsetDateTime now = OffsetDateTime.now();
        BigDecimal paymentAmount = request.amount();

        Invoice invoice = null;
        Branch branch;
        Customer debtor;
        Customer payer;

        // ----------------------------
        // 1️⃣ Визначаємо debtor і payer
        // ----------------------------
        if (request.invoiceId() != null) {
            invoice = invoiceRepository.findById(request.invoiceId())
                    .orElseThrow(() -> new TransactionException("Інвойс не знайдено"));

            branch = resolveInvoiceBranch(invoice.getId());

            debtor = invoice.getCustomer();
            payer = invoice.getEffectivePayer();

        } else {
            branch = branchRepository.findByBranchNo(request.branchNo())
                    .orElseThrow(() -> new TransactionException("Філію не вказано"));

            if (request.customerId() == null) {
                throw new TransactionException("Для авансу потрібно вказати клієнта");
            }

            debtor = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new TransactionException("Клієнт не знайдений"));

            payer = debtor; // для авансу платник = клієнт
        }

        List<CustomerTransactionDTO> result = new ArrayList<>();

        // ----------------------------
        // 2️⃣ ADVANCE (без інвойсу)
        // ----------------------------
        if (invoice == null) {

            CustomerTransaction advanceTx = customerTxRepository.save(
                    CustomerTransaction.builder()
                            .branch(branch)
                            .customer(debtor)
                            .type(CustomerTransactionType.ADVANCE)
                            .amount(paymentAmount)
                            .createdAt(now)
                            .note("Авансовий платіж")
                            .build()
            );

            result.add(toDTO(advanceTx));

        } else {

            // ----------------------------
            // 3️⃣ РАХУЄМО БОРГ
            // ----------------------------
            BigDecimal totalDebt = calculateInvoiceDebt(invoice.getId());

            List<InvoiceOrder> invoiceOrders =
                    invoiceOrderRepository.findByInvoice_Id(invoice.getId());

            if (invoiceOrders.isEmpty()) {
                throw new TransactionException("Інвойс не містить замовлень");
            }

            // ----------------------------
            // 4️⃣ ЯКЩО БОРГУ НЕМАЄ → ADVANCE
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

                result.add(toDTO(advanceTx));

            } else {

                BigDecimal paid = paymentAmount.min(totalDebt);
                BigDecimal overpay = paymentAmount.subtract(paid);

                // ----------------------------
                // 5️⃣ PAYMENT
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

                result.add(toDTO(paymentTx));


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
                // 7️⃣ ADVANCE (переплата)
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

                    result.add(toDTO(advanceTx));
                }

                // ----------------------------
                // 8️⃣ СТАТУС ІНВОЙСУ
                // ----------------------------
                BigDecimal remainingDebt = totalDebt.subtract(paid);

                if (remainingDebt.compareTo(BigDecimal.ZERO) <= 0) {
                    invoice.setStatus(InvoiceEnums.InvoiceStatus.PAID);
                } else {
                    invoice.setStatus(InvoiceEnums.InvoiceStatus.PARTIALLY_PAID);
                }
            }
        }

        // ----------------------------
        // 9️⃣ LEDGER (ОДИН ЗАПИС)
        // ----------------------------
        ledgerRepository.save(
                LedgerEntry.builder()
                        .branch(branch)
                        .direction(LedgerDirection.IN)
                        .category(LedgerCategory.PAYMENT_RECEIVED)
                        .amount(paymentAmount)
                        .createdAt(now)
                        .createdBy(user)
                        .customerId(debtor.getCustNo())   // боржник
                        .payer(payer)                     // 🔥 платник
                        .invoiceId(invoice != null ? invoice.getId() : null)
                        .reference(invoice != null
                                ? "PAY-" + invoice.getInvoiceNo()
                                : "ADV-" + now.toEpochSecond())
                        .note(request.note())
                        .build()
        );
        return result;
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

    @Transactional
    public CustomerTransactionDTO registerAdvancePayment(
            Long customerId,
            InvoicePaymentRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();

        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new TransactionException("Користувач не знайдений: " + username));

        // 1️⃣ Перевірка філії
        if (request.branchNo() == null) {
            throw new TransactionException("Філія не вказана");
        }

        Set<Long> allowedBranchNos = user.getAllowedBranches()
                .stream()
                .map(Branch::getBranchNo)
                .collect(Collectors.toSet());

        if (!allowedBranchNos.contains(request.branchNo())) {
            throw new TransactionException("Немає доступу до філії: " + request.branchNo());
        }

        Branch branch = branchRepository.findById(request.branchNo())
                .orElseThrow(() -> new TransactionException("Філія не знайдена"));

        // 2️⃣ Завантажуємо клієнта
        if (customerId == null) {
            throw new TransactionException("Клієнта не вказано");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new TransactionException("Клієнт не знайдений"));

        // 3️⃣ Перевірка суми
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сума повинна бути позитивною");
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 4️⃣ CustomerTransaction (опціонально)
        CustomerTransaction customerTx = customerTxRepository.save(
                CustomerTransaction.builder()
                        .customer(customer)
                        .branch(branch)
                        .type(CustomerTransactionType.ADVANCE) // 🔥 новий тип
                        .amount(request.amount()) // 🔥 ПЛЮС (це баланс клієнта)
                        .createdAt(now)
                        .note(request.note())
                        .build()
        );

        // 5️⃣ LedgerEntry (ГОЛОВНЕ)
        ledgerRepository.save(
                LedgerEntry.builder()
                        .branch(branch)
                        .direction(LedgerDirection.IN)
                        .category(LedgerCategory.CUSTOMER_ADVANCE) // 🔥 окрема категорія
                        .amount(request.amount())
                        .createdAt(now)
                        .createdBy(user)

                        .customerId(customer.getCustNo())
                        // ❗ НЕ ставимо invoiceId
                        // ❗ НЕ ставимо orderId
                        .customerTransactionId(customerTx.getId())
                        .reference("ADV-" + customer.getCustNo())
                        .note(request.note())
                        .build()
        );

        // 6️⃣ DTO
        return CustomerTransactionDTO.builder()
                .id(customerTx.getId())
                .invoiceId(null) // 🔥 немає інвойсу
                .amount(customerTx.getAmount())
                .createdAt(customerTx.getCreatedAt())
                .note(customerTx.getNote())
                .reference("ADV-" + customer.getCustNo())
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

    public List<CustomerPaymentDTO> getPaymentsByInvoice(Long invoiceId) {
        return customerTxRepository.findPaymentsByInvoiceId(invoiceId);
    }

    public CustomerFinanceDTO getCustomerFinance(Long customerId) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow();

        BigDecimal balance =
                ledgerRepository.getCustomerBalance(customerId);

        List<CustomerInvoiceDTO> invoices =
                invoiceRepository.findOpenInvoices(customerId);

        BigDecimal totalDebt = invoices.stream()
                .map(CustomerInvoiceDTO::debt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CustomerLedgerDTO> ledger =
                ledgerRepository.findCustomerLedger(customerId);

        return new CustomerFinanceDTO(
                customer.getCompany(),
                customer.getMobile(),
                balance,
                totalDebt,
                invoices,
                ledger
        );
    }

    public BigDecimal calculateInvoiceDebt(Long invoiceId) {

        return ledgerRepository.calculateInvoiceDebt(invoiceId); // борг
    }

}

