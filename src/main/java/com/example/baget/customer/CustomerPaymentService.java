package com.example.baget.customer;

import com.example.baget.branch.Branch;
import com.example.baget.branch.BranchRepository;
import com.example.baget.invoices.*;
import com.example.baget.ledger.LedgerCategory;
import com.example.baget.ledger.LedgerDirection;
import com.example.baget.ledger.LedgerEntry;
import com.example.baget.ledger.LedgerRepository;
import com.example.baget.orders.Orders;
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


    @Transactional
    public List<CustomerTransactionDTO> registerInvoicePayment(
            InvoicePaymentRequest request, Authentication authentication) {

        String username = authentication.getName();

        Branch branch = branchRepository.findByBranchNo(request.branchNo())
                .orElseThrow(() -> new TransactionException("Філію не вказано"));

        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new TransactionException("Користувач не знайдений: " + username));

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Сума оплати має бути більше 0");
        }

        OffsetDateTime now = OffsetDateTime.now();
        BigDecimal paymentAmount = request.amount();

        Invoice invoice = null;
        Customer payer;

        // ----------------------------
        // 1️⃣ Визначаємо payer
        // ----------------------------
        if (request.invoiceId() != null) {
            invoice = invoiceRepository.findById(request.invoiceId())
                    .orElseThrow(() -> new TransactionException("Інвойс не знайдено"));

            payer = invoice.getCustomer();
        } else {
            if (request.customerId() == null) {
                throw new TransactionException("Для авансу потрібно вказати клієнта");
            }

            payer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new TransactionException("Платник не знайдений"));
        }

        List<CustomerTransactionDTO> result = new ArrayList<>();

        // ----------------------------
        // 2️⃣ ЛОГІКА БЕЗ ІНВОЙСУ (ADVANCE)
        // ----------------------------
        if (invoice == null) {

            CustomerTransaction advanceTx = customerTxRepository.save(
                    CustomerTransaction.builder()
                            .branch(branch)
                            .customer(payer)
                            .type(CustomerTransactionType.ADVANCE)
                            .amount(paymentAmount)
                            .createdAt(now)
                            .note("Авансовий платіж")
                            .build()
            );

            result.add(toDTO(advanceTx));

        } else {

            // ----------------------------
            // 3️⃣ ЛОГІКА З ІНВОЙСОМ
            // ----------------------------

            BigDecimal totalDebt = calculateInvoiceDebt(invoice.getId());

            List<InvoiceOrder> invoiceOrders =
                    invoiceOrderRepository.findByInvoice_Id(invoice.getId());

            if (invoiceOrders.isEmpty()) {
                throw new TransactionException("Інвойс не містить замовлень");
            }

            if (totalDebt.compareTo(BigDecimal.ZERO) <= 0) {

                // все в аванс
                CustomerTransaction advanceTx = customerTxRepository.save(
                        CustomerTransaction.builder()
                                .branch(branch)
                                .customer(payer)
                                .type(CustomerTransactionType.ADVANCE)
                                .amount(paymentAmount)
                                .createdAt(now)
                                .note("Інвойс вже оплачено → аванс")
                                .build()
                );

                result.add(toDTO(advanceTx));

            } else {

                BigDecimal allocationAmount = paymentAmount.min(totalDebt);
                BigDecimal overpay = paymentAmount.subtract(allocationAmount);

                // PAYMENT
                CustomerTransaction paymentTx = customerTxRepository.save(
                        CustomerTransaction.builder()
                                .branch(branch)
                                .customer(payer)
                                .invoice(invoice)
                                .type(CustomerTransactionType.PAYMENT)
                                .amount(allocationAmount)
                                .createdAt(now)
                                .note(request.note())
                                .build()
                );

                result.add(toDTO(paymentTx));

                // ALLOCATION
                BigDecimal remaining = allocationAmount;

                for (InvoiceOrder io : invoiceOrders) {
                    if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                    Orders order = io.getOrder();
                    BigDecimal orderDebt = calculateOrderDebt(order.getOrderNo());

                    if (orderDebt.compareTo(BigDecimal.ZERO) <= 0) continue;

                    BigDecimal apply = remaining.min(orderDebt);

                    CustomerTransaction allocationTx = customerTxRepository.save(
                            CustomerTransaction.builder()
                                    .branch(branch)
                                    .customer(payer)
                                    .invoice(invoice)
                                    .order(order)
                                    .type(CustomerTransactionType.ALLOCATION)
                                    .amount(apply)
                                    .parentTransactionId(paymentTx.getId())
                                    .createdAt(now)
                                    .note("Розподіл оплати")
                                    .build()
                    );

                    result.add(toDTO(allocationTx));
                    remaining = remaining.subtract(apply);
                }

                // ADVANCE (переплата)
                if (overpay.compareTo(BigDecimal.ZERO) > 0) {
                    CustomerTransaction advanceTx = customerTxRepository.save(
                            CustomerTransaction.builder()
                                    .branch(branch)
                                    .customer(payer)
                                    .type(CustomerTransactionType.ADVANCE)
                                    .amount(overpay)
                                    .createdAt(now)
                                    .note("Переплата інвойсу " + invoice.getInvoiceNo())
                                    .build()
                    );

                    result.add(toDTO(advanceTx));
                }

                // статус інвойсу (без повторного calculate)
                BigDecimal remainingDebt = totalDebt.subtract(paymentAmount);

                if (remainingDebt.compareTo(BigDecimal.ZERO) <= 0) {
                    invoice.setStatus(InvoiceEnums.InvoiceStatus.PAID);
                } else {
                    invoice.setStatus(InvoiceEnums.InvoiceStatus.PARTIALLY_PAID);
                }
            }
        }

        // ----------------------------
        // 4️⃣ LEDGER (ЗАВЖДИ)
        // ----------------------------
        String reference = (invoice != null)
                ? "PAY-" + invoice.getInvoiceNo()
                : "ADV-" + now.toEpochSecond();

        ledgerRepository.save(
                LedgerEntry.builder()
                        .branch(branch)
                        .direction(LedgerDirection.IN)
                        .category(LedgerCategory.PAYMENT_RECEIVED)
                        .amount(paymentAmount)
                        .createdAt(now)
                        .createdBy(user)
                        .customerId(payer.getCustNo())
                        .invoiceId(invoice != null ? invoice.getId() : null)
                        .reference(reference)
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

    public BigDecimal calculateOrderDebt(Long orderId) {

        return ledgerRepository.calculateOrderDebt(orderId);
    }

}

