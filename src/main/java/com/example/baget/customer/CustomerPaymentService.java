package com.example.baget.customer;

import com.example.baget.branch.Branch;
import com.example.baget.branch.BranchRepository;
import com.example.baget.finance.FinanceCategory;
import com.example.baget.finance.FinanceDirection;
import com.example.baget.finance.FinanceTransaction;
import com.example.baget.finance.FinanceTransactionRepository;
import com.example.baget.invoices.Invoice;
import com.example.baget.invoices.InvoiceEnums;
import com.example.baget.invoices.InvoicePaymentRequest;
import com.example.baget.invoices.InvoiceRepository;
import com.example.baget.ledger.LedgerCategory;
import com.example.baget.ledger.LedgerDirection;
import com.example.baget.ledger.LedgerEntry;
import com.example.baget.ledger.LedgerRepository;
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
    private final FinanceTransactionRepository financeTxRepository;
    private final InvoiceRepository invoiceRepository;
    private final LedgerRepository ledgerRepository;
    private final BranchRepository branchRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public CustomerTransactionDTO registerInvoicePayment(Long invoiceId, InvoicePaymentRequest request, Authentication authentication) {
        String username = authentication.getName();

        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new TransactionException("Користувач з таким ім'ям відсутній: " + username));

        Set<Long> allowedBranchNos = user.getAllowedBranches()
                .stream()
                .map(Branch::getBranchNo)
                .collect(Collectors.toSet());

        if (request.branchNo() == null) {
            throw new TransactionException("Філія не вказана");
        }

        if (!allowedBranchNos.contains(request.branchNo())) {
            throw new TransactionException("Вам заборонено працювати в філії №: " + request.branchNo());
        }

        Branch branch = branchRepository.findById(request.branchNo())
                .orElseThrow(() -> new TransactionException("Філія не знайдена: " + request.branchNo()));

        // 1️⃣ Завантажуємо інвойс разом з клієнтом
        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new TransactionException("Інвойс не знайдено: " + invoiceId));

        Customer customer = invoice.getCustomer(); // вже в persistence context

        // 2️⃣ Перевіряємо суму
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сума повинна бути позитивною");
        }

        BigDecimal debt = calculateDebt(invoice);
        if (request.amount().compareTo(debt) > 0) {
            throw new IllegalArgumentException("Оплата перевищує борг");
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 3️⃣ Створюємо CustomerTransaction
        CustomerTransaction customerTx = customerTxRepository.save(
                CustomerTransaction.builder()
                        .customer(customer)
                        .branch(branch)
                        .invoice(invoice)
                        .type(CustomerTransactionType.PAYMENT)
                        .amount(request.amount().negate()) // мінус
                        .createdAt(now)
                        .note(request.note())
                        .build()
        );

        // 4️⃣ Створюємо FinanceTransaction  ---------------- Застаріло
        financeTxRepository.save(
                FinanceTransaction.builder()
                        .direction(FinanceDirection.IN)
                        .category(FinanceCategory.CUSTOMER_PAYMENT)
                        .amount(request.amount())
                        .createdAt(now)
                        .customerTransactionId(customerTx.getId())
                        .createdBy(user)
                        .reference("INV-" + invoice.getInvoiceNo())
                        .build()
        );

        // 4️⃣ Створюємо LedgerEntry Transaction
        ledgerRepository.save(
                LedgerEntry.builder()
                        .branch(branch)
                        .direction(LedgerDirection.IN)
                        .category(LedgerCategory.CUSTOMER_PAYMENT)
                        .amount(request.amount())
                        .createdAt(now)
                        .createdBy(user)
                        .customerId(customer.getCustNo())
                        .invoiceId(invoice.getId())
                        .reference("INV-" + invoice.getInvoiceNo())
                        .note(request.note())
                        .build()
        );

        // 5️⃣ Оновлюємо статус інвойсу
        BigDecimal remaining = debt.subtract(request.amount());

        if (remaining.compareTo(BigDecimal.ZERO) == 0) {
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

    public BigDecimal calculateDebt(Invoice invoice) {

        BigDecimal total = invoice.getTotalAmount();

        BigDecimal txSum = customerTxRepository.sumTransactionsByInvoice(invoice);

        // txSum буде від’ємним для оплат
        return total.add(txSum);
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

}

