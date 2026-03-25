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
    public CustomerTransactionDTO registerInvoicePayment(Long invoiceId, InvoicePaymentRequest request, Authentication authentication) {

        String username = authentication.getName();

        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new TransactionException("Користувач не знайдений: " + username));

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Сума оплати має бути більше 0");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new TransactionException("Інвойс не знайдено"));

        Customer payer = customerRepository.findById(invoice.getCustomer().getCustNo())
                .orElseThrow(() -> new TransactionException("Платник не знайдений"));

        OffsetDateTime now = OffsetDateTime.now();

        // 🔥 1️⃣ Отримуємо всі orders інвойсу
        List<InvoiceOrder> invoiceOrders = invoiceOrderRepository.findByInvoice_Id(invoice.getId());

        if (invoiceOrders.isEmpty()) {
            throw new TransactionException("Інвойс не містить замовлень");
        }

        // 🔥 2️⃣ Рахуємо залишок боргу по інвойсу
        BigDecimal totalDebt = calculateInvoiceDebt(invoice.getId());

        if (totalDebt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Інвойс вже оплачено");
        }

        BigDecimal paymentAmount = request.amount().min(totalDebt);

        // 🔥 3️⃣ Створюємо CUSTOMER TX
        CustomerTransaction customerTx = customerTxRepository.save(
                CustomerTransaction.builder()
                        .customer(payer)
                        .branch(invoiceOrders.get(0).getOrder().getBranch())
                        .invoice(invoice)
                        .type(CustomerTransactionType.PAYMENT)
                        .amount(paymentAmount)
                        .createdAt(now)
                        .note(request.note())
                        .build()
        );

        // 🔥 4️⃣ Створюємо IN (гроші прийшли)
        ledgerRepository.save(
                LedgerEntry.builder()
                        .branch(invoiceOrders.get(0).getOrder().getBranch())
                        .direction(LedgerDirection.IN)
                        .category(LedgerCategory.PAYMENT_RECEIVED)
                        .amount(paymentAmount)
                        .createdAt(now)
                        .createdBy(user)

                        .customerId(payer.getCustNo()) // 🔥 платник
                        .invoiceId(invoice.getId())

                        .customerTransactionId(customerTx.getId())

                        .reference("PAY-" + invoice.getInvoiceNo())
                        .note("Оплата інвойсу")
                        .build()
        );

        // 🔥 5️⃣ ALLOCATION (гасимо борг по orders)
        BigDecimal remaining = paymentAmount;

        for (InvoiceOrder io : invoiceOrders) {

            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            Orders order = io.getOrder();

            // 🔥 залишок боргу по цьому order
            BigDecimal orderDebt = calculateOrderDebt(order.getOrderNo());

            if (orderDebt.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal apply = remaining.min(orderDebt);

            ledgerRepository.save(
                    LedgerEntry.builder()
                            .branch(order.getBranch())
                            .direction(LedgerDirection.OUT)
                            .category(LedgerCategory.PAYMENT_ALLOCATION)
                            .amount(apply)
                            .createdAt(now)
                            .createdBy(user)

                            // 🔥 ВАЖЛИВО
                            .customerId(order.getCustomer().getCustNo()) // боржник
                            .orderId(order.getOrderNo())
                            .invoiceId(invoice.getId())

                            .customerTransactionId(customerTx.getId())

                            .reference("ALLOC-" + invoice.getInvoiceNo())
                            .note("Розподіл оплати")
                            .build()
            );

            remaining = remaining.subtract(apply);
        }

        // 🔥 6️⃣ Оновлюємо статус інвойсу
        BigDecimal remainingDebt = calculateInvoiceDebt(invoice.getId());

        if (remainingDebt.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(InvoiceEnums.InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceEnums.InvoiceStatus.PARTIALLY_PAID);
        }

        // 6️⃣ Повертаємо DTO для фронтенду
        return CustomerTransactionDTO.builder()
                .id(customerTx.getId())
                .invoiceId(invoice.getId())
                .amount(customerTx.getAmount())
                .createdAt(customerTx.getCreatedAt())
                .note(customerTx.getNote())
                .reference("INV-" + invoice.getInvoiceNo())
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
            throw new TransactionException("Клієнт не вказаний");
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
                customerTxRepository.getCustomerBalance(customerId);

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

        BigDecimal in = ledgerRepository.sumInByInvoice(invoiceId);
        BigDecimal out = ledgerRepository.sumOutByInvoice(invoiceId);

        return out.subtract(in); // борг
    }

    public BigDecimal calculateOrderDebt(Long orderId) {

        BigDecimal in = ledgerRepository.sumInByOrder(orderId);
        BigDecimal out = ledgerRepository.sumOutByOrder(orderId);

        return out.subtract(in);
    }

}

