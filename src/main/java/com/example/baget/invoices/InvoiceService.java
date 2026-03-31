package com.example.baget.invoices;

import com.example.baget.customer.*;
import com.example.baget.ledger.LedgerCategory;
import com.example.baget.ledger.LedgerDirection;
import com.example.baget.ledger.LedgerEntry;
import com.example.baget.ledger.LedgerRepository;
import com.example.baget.users.User;
import com.example.baget.users.UsersRepository;
import com.example.baget.util.InvoiceServiceUtil;
import com.example.baget.util.TransactionException;
import jakarta.persistence.EntityManager;
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
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final CustomerTransactionRepository customerTxRepository;
    private final UsersRepository usersRepository;
    private final InvoiceOrderRepository invoiceOrderRepository;
    private final LedgerRepository ledgerRepository;
    private final InvoiceMapper invoiceMapper;
    private final EntityManager entityManager;
    private final InvoiceServiceUtil invoiceServiceUtil;

    @Transactional
    public InvoiceDTO mergeInvoices(MergeInvoicesRequest request, Authentication authentication) {

        String username = authentication.getName();

        User user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new TransactionException("Користувач не знайдений: " + username));

        List<Long> invoiceIds = request.getInvoiceIds();
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            throw new TransactionException("Список інвойсів порожній");
        }

        // 1️⃣ Завантажуємо інвойси
        List<Invoice> invoices = invoiceRepository.findAllById(invoiceIds);

        if (invoices.size() != invoiceIds.size()) {
            throw new TransactionException("Деякі інвойси не знайдено");
        }

        // 2️⃣ Перевірки
        for (Invoice inv : invoices) {

            if (inv.getLifecycle() != InvoiceEnums.InvoiceLifecycle.ACTIVE) {
                throw new TransactionException("Інвойс " + inv.getInvoiceNo() + " вже об'єднаний");
            }

            if (inv.getStatus() != InvoiceEnums.InvoiceStatus.ISSUED) {
                throw new TransactionException("Можна об'єднувати тільки неоплачені інвойси");
            }
        }

        // 3️⃣ Визначаємо платника
        Customer payer;

        if (request.getPayerId() != null) {
            payer = customerRepository.findById(request.getPayerId())
                    .orElseThrow(() -> new TransactionException("Платника не знайдено"));
        } else {
            Set<Long> customerIds = invoices.stream()
                    .map(i -> i.getCustomer().getCustNo())
                    .collect(Collectors.toSet());

            if (customerIds.size() > 1) {
                throw new TransactionException(
                        "MULTIPLE_CUSTOMERS",
                        "Для рахунку з кількома клієнтами потрібно вибрати платника"
                );
            }

            payer = invoices.get(0).getCustomer();
        }

        // 4️⃣ Збираємо всі orders
        List<InvoiceOrder> allInvoiceOrders = invoiceOrderRepository.findByInvoice_IdIn(invoiceIds);

        if (allInvoiceOrders.isEmpty()) {
            throw new TransactionException("Інвойси не містять замовлень");
        }

        // 5️⃣ Рахуємо суму
        BigDecimal totalAmount = allInvoiceOrders.stream()
                .map(InvoiceOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 6️⃣ Генеруємо номер
        Long invoiceNo = invoiceServiceUtil.generateTodayCode();
        while (invoiceRepository.existsByInvoiceNo(invoiceNo)) {
            invoiceNo++;
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 7️⃣ Створюємо новий інвойс
        Invoice newInvoice = Invoice.builder()
                .invoiceNo(invoiceNo)
                .customer(payer)
                .type(InvoiceEnums.InvoiceType.CONSOLIDATED)
                .status(InvoiceEnums.InvoiceStatus.ISSUED)
                .lifecycle(InvoiceEnums.InvoiceLifecycle.ACTIVE)
                .totalAmount(totalAmount)
                .note(request.getNote())
                .build();

        invoiceRepository.save(newInvoice);
        entityManager.flush();

        // 8️⃣ Копіюємо InvoiceOrder
        for (InvoiceOrder oldIo : allInvoiceOrders) {

            InvoiceOrder newIo = new InvoiceOrder();
            newIo.setInvoice(newInvoice);
            newIo.setOrder(oldIo.getOrder());
            newIo.setAmount(oldIo.getAmount());

            invoiceOrderRepository.save(newIo);
        }

        // 9️⃣ Старі інвойси → MERGED
        for (Invoice old : invoices) {
            old.setLifecycle(InvoiceEnums.InvoiceLifecycle.MERGED);
//            old.setParentInvoiceId(newInvoice.getId()); // якщо додав
        }

        // 🔟 APPLY ADVANCE (опціонально)
        BigDecimal advance = getCustomerBalance(payer.getCustNo());

        if (advance.compareTo(BigDecimal.ZERO) > 0) {

            BigDecimal toApply = advance.min(totalAmount);

            Long txId = customerTxRepository.save(
                    CustomerTransaction.builder()
                            .customer(payer)
                            .invoice(newInvoice)
                            .type(CustomerTransactionType.ADVANCE_APPLIED)
                            .amount(toApply.negate())
                            .createdAt(now)
                            .note("Списання авансу на інвойс №" + newInvoice.getInvoiceNo())
                            .build()
            ).getId();

            BigDecimal remaining = toApply;

            for (InvoiceOrder io : allInvoiceOrders) {

                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal apply = remaining.min(io.getAmount());

                ledgerRepository.save(
                        LedgerEntry.builder()
                                .branch(io.getOrder().getBranch())
                                .direction(LedgerDirection.OUT)
                                .category(LedgerCategory.APPLY_ADVANCE_TO_INVOICE)
                                .amount(apply)
                                .createdAt(now)
                                .createdBy(user)

                                .customerId(io.getOrder().getCustomer().getCustNo())
                                .orderId(io.getOrder().getOrderNo())
                                .invoiceId(newInvoice.getId())
                                .customerTransactionId(txId)

                                .reference("APPLY_ADV-" + newInvoice.getInvoiceNo())
                                .note("Списання авансу")
                                .build()
                );

                remaining = remaining.subtract(apply);
            }

            if (toApply.compareTo(totalAmount) >= 0) {
                newInvoice.setStatus(InvoiceEnums.InvoiceStatus.PAID);
            } else {
                newInvoice.setStatus(InvoiceEnums.InvoiceStatus.PARTIALLY_PAID);
            }
        }

        return invoiceMapper.toDto(newInvoice);
    }

    public BigDecimal getCustomerBalance(Long customerId) {
        BigDecimal result = ledgerRepository.getCustomerBalance(customerId);
        return result != null ? result : BigDecimal.ZERO;
    }

        public InvoiceDetailsDTO getInvoice(Long invoiceId) {
        return invoiceRepository.findInvoiceDetails(invoiceId)
                .orElseThrow(() -> new TransactionException("Invoice not found"));
    }

}
