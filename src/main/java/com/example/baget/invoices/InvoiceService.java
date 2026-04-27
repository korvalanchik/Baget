package com.example.baget.invoices;

import com.example.baget.branch.Branch;
import com.example.baget.branch.BranchRepository;
import com.example.baget.customer.*;
import com.example.baget.ledger.LedgerCategory;
import com.example.baget.ledger.LedgerDirection;
import com.example.baget.ledger.LedgerEntry;
import com.example.baget.ledger.LedgerRepository;
import com.example.baget.orders.OrderPaySummaryDTO;
import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.example.baget.util.InvoiceServiceUtil;
import com.example.baget.util.TransactionException;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceOrderRepository invoiceOrderRepository;
    private final LedgerRepository ledgerRepository;
    private final InvoiceMapper invoiceMapper;
    private final EntityManager entityManager;
    private final InvoiceServiceUtil invoiceServiceUtil;
    private final OrdersRepository ordersRepository;
    private final BranchRepository branchRepository;
    private final CustomerTransactionRepository customerTransactionRepository;

    @Transactional
    public InvoiceDTO mergeInvoices(MergeInvoicesRequest request) {

        List<Long> invoiceIds = request.invoiceIds();
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            throw new TransactionException("Список інвойсів порожній");
        }

        Set<Long> uniqueIds = new HashSet<>(invoiceIds);
        if (uniqueIds.size() != invoiceIds.size()) {
            throw new TransactionException("Список інвойсів містить дублікати");
        }

// 1️⃣ Завантажуємо інвойси
        List<Invoice> invoices = invoiceRepository.findAllById(invoiceIds);

        if (invoices.size() != invoiceIds.size()) {
            throw new TransactionException("Деякі інвойси не знайдено");
        }

// 2️⃣ Перевірки
        for (Invoice inv : invoices) {

            if (inv.getLifecycle() != InvoiceEnums.InvoiceLifecycle.ACTIVE) {
                throw new TransactionException("Інвойс " + inv.getInvoiceNo() + " вже не активний");
            }

            if (inv.getStatus() != InvoiceEnums.InvoiceStatus.ISSUED) {
                throw new TransactionException("Можна об'єднувати тільки неоплачені");
            }
        }

// 3️⃣ Визначаємо payer
        Customer payer;

        if (request.payerId() != null) {
            payer = customerRepository.findById(request.payerId())
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

        OffsetDateTime now = OffsetDateTime.now();

// 4️⃣ Рахуємо борг, який переносимо
        BigDecimal totalDebtToTransfer = BigDecimal.ZERO;

        Map<Long, BigDecimal> invoiceDebts = new HashMap<>();

        for (Invoice inv : invoices) {
            BigDecimal debt = invoiceRepository.calculateInvoiceDebt(inv.getId());

            if (debt.compareTo(BigDecimal.ZERO) < 0) {
                throw new TransactionException("Інвойс " + inv.getInvoiceNo() + " має переплату");
            }

            invoiceDebts.put(inv.getId(), debt);
            totalDebtToTransfer = totalDebtToTransfer.add(debt);
        }

        if (totalDebtToTransfer.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransactionException("Немає боргу для об'єднання");
        }

// 5️⃣ Генеруємо номер
        Long invoiceNo = invoiceServiceUtil.generateTodayCode();
        while (invoiceRepository.existsByInvoiceNo(invoiceNo)) {
            invoiceNo++;
        }

// 6️⃣ Створюємо новий інвойс
        Invoice newInvoice = Invoice.builder()
                .invoiceNo(invoiceNo)
                .customer(payer)
                .payer(payer)
                .type(InvoiceEnums.InvoiceType.CONSOLIDATED)
                .status(InvoiceEnums.InvoiceStatus.ISSUED)
                .lifecycle(InvoiceEnums.InvoiceLifecycle.ACTIVE)
                .totalAmount(totalDebtToTransfer) // 🔥 ВАЖЛИВО: debt, а не original amount
                .note(request.note())
                .createdAt(now)
                .build();

        invoiceRepository.save(newInvoice);
        entityManager.flush();

// 7️⃣ Копіюємо InvoiceOrder
        List<InvoiceOrder> allInvoiceOrders =
                invoiceOrderRepository.findByInvoice_IdIn(invoiceIds);

        if (allInvoiceOrders.isEmpty()) {
            throw new TransactionException("Інвойси не містять замовлень");
        }

        for (InvoiceOrder oldIo : allInvoiceOrders) {

            InvoiceOrder newIo = new InvoiceOrder();
            newIo.setInvoice(newInvoice);
            newIo.setOrder(oldIo.getOrder());
            newIo.setAmount(oldIo.getAmount());

            invoiceOrderRepository.save(newIo);
        }

// 8️⃣ Ledger: закриваємо старі інвойси (IN)

        Map<Long, Branch> invoiceBranchMap = allInvoiceOrders.stream()
                .collect(Collectors.toMap(
                        io -> io.getInvoice().getId(),
                        io -> io.getOrder().getBranch(),
                        (existing, replacement) -> {
                            if (!existing.equals(replacement)) {
                                throw new TransactionException("Інвойс має кілька філій");
                            }
                            return existing;
                        }
                ));

        Map<Long, Orders> invoiceOrderMap = allInvoiceOrders.stream()
                .collect(Collectors.toMap(
                        io -> io.getInvoice().getId(),
                        InvoiceOrder::getOrder,
                        (existing, replacement) -> existing
                ));

        for (Invoice old : invoices) {

            BigDecimal debt = invoiceDebts.get(old.getId());

            if (debt.compareTo(BigDecimal.ZERO) <= 0) continue;

            Branch oldBranch = invoiceBranchMap.get(old.getId());

            if (oldBranch == null) {
                throw new TransactionException("Не знайдено branch для інвойсу " + old.getInvoiceNo());
            }

            ledgerRepository.save(
                    LedgerEntry.builder()
                            .branch(oldBranch)
                            .direction(LedgerDirection.IN)
                            .category(LedgerCategory.INVOICE_MERGE_IN)
                            .amount(debt)
                            .createdAt(now)
                            .customerId(old.getCustomer().getCustNo())
                            .payer(payer)
                            .invoiceId(old.getId())
                            .reference("MERGE->" + invoiceNo)
                            .note("Перенос боргу в інвойс " + invoiceNo)
                            .build()
            );

// CustomerTransactions: закриваємо борг по старому інвойсу
            customerTransactionRepository.save(
                    CustomerTransaction.builder()
                            .branch(oldBranch)
                            .customer(old.getCustomer())
                            .invoice(old)
                            .order(invoiceOrderMap.get(old.getId()))
                            .type(CustomerTransactionType.ADJUSTMENT)
                            .amount(debt)
                            .createdAt(now)
                            .note("Перенос боргу в інвойс " + invoiceNo)
                            .build()
            );
        }

        Branch branch = branchRepository.findByBranchNo(request.branchNo())
                .orElseThrow(() -> new TransactionException("Філію не вказано"));

// 9️⃣ Ledger: відкриваємо борг на новому інвойсі (OUT)
        ledgerRepository.save(
                LedgerEntry.builder()
                        .branch(branch)
                        .direction(LedgerDirection.OUT)
                        .category(LedgerCategory.INVOICE_MERGE_OUT)
                        .amount(totalDebtToTransfer)
                        .createdAt(now)
                        .customerId(newInvoice.getCustomer().getCustNo())
                        .payer(payer)
                        .invoiceId(newInvoice.getId())
                        .reference("MERGE-FROM-" + invoiceIds)
                        .note("Об'єднання інвойсів")
                        .build()
        );

// CustomerTransactions: відкриваємо борг на новому інвойсі
        customerTransactionRepository.save(
                CustomerTransaction.builder()
                        .branch(branch)
                        .customer(newInvoice.getCustomer())
                        .invoice(newInvoice)
                        .type(CustomerTransactionType.INVOICE)
                        .amount(totalDebtToTransfer.negate())
                        .createdAt(now)
                        .note("Об'єднання інвойсів: " + invoiceIds)
                        .build()
        );

// 🔟 Старі інвойси → MERGED
        for (Invoice old : invoices) {
            old.setLifecycle(InvoiceEnums.InvoiceLifecycle.MERGED);
            old.setPayer(payer);
        }

// 1️⃣1️⃣ Оновлюємо orders (UI)
        final Long invoiceNoFinal = invoiceNo;

        List<Orders> ordersToUpdate = allInvoiceOrders.stream()
                .map(InvoiceOrder::getOrder)
                .peek(o -> o.setRahFacNo(invoiceNoFinal))
                .collect(Collectors.toList());

        ordersRepository.saveAll(ordersToUpdate);

        return invoiceMapper.toDto(newInvoice);

    }
    public InvoiceDetailsDTO getInvoice(Long invoiceId) {
        return invoiceRepository.findInvoiceDetails(invoiceId)
                .orElseThrow(() -> new TransactionException("Invoice not found"));
    }

    public CollectiveInvoiceDTO getCollectiveInvoice(Long invoiceNo) {

        // 1️⃣ Invoice
        Invoice invoice = invoiceRepository
                .findByInvoiceNoAndLifecycle(invoiceNo, InvoiceEnums.InvoiceLifecycle.ACTIVE)
                .orElseThrow(() -> new TransactionException("Invoice " + invoiceNo + " not found"));

        // 2️⃣ InvoiceOrders
        List<InvoiceOrder> invoiceOrders =
                invoiceOrderRepository.findByInvoice_Id(invoice.getId());

        if (invoiceOrders.isEmpty()) {
            throw new IllegalStateException("Invoice has no orders");
        }

        // 3️⃣ Збираємо orderIds
        List<Long> orderIds = invoiceOrders.stream()
                .map(io -> io.getOrder().getOrderNo())
                .toList();

        // 4️⃣ 🔥 ОДИН SQL ДО LEDGER
        List<Object[]> ledgerData = ledgerRepository.sumInOutByOrders(orderIds);

        // 5️⃣ Перетворюємо в Map для швидкого доступу
        Map<Long, BigDecimal> inMap = new HashMap<>();
        Map<Long, BigDecimal> outMap = new HashMap<>();

        for (Object[] row : ledgerData) {
            Long orderId = (Long) row[0];
            BigDecimal in = (BigDecimal) row[1];
            BigDecimal out = (BigDecimal) row[2];

            inMap.put(orderId, in);
            outMap.put(orderId, out);
        }

        // 6️⃣ Формуємо summaries
        List<OrderPaySummaryDTO> orderSummaries = invoiceOrders.stream()
                .map(io -> {

                    Long orderId = io.getOrder().getOrderNo();

                    BigDecimal billed = io.getAmount();

                    BigDecimal in = inMap.getOrDefault(orderId, BigDecimal.ZERO);
                    BigDecimal out = outMap.getOrDefault(orderId, BigDecimal.ZERO);

                    BigDecimal due = out.subtract(in);

                    return new OrderPaySummaryDTO(orderId, billed, in, due);
                })
                .toList();

        // 7️⃣ totals
        BigDecimal totalBilled = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalDue = BigDecimal.ZERO;

        for (OrderPaySummaryDTO o : orderSummaries) {
            totalBilled = totalBilled.add(o.billed());
            totalPaid   = totalPaid.add(o.paid());
            totalDue    = totalDue.add(o.due());
        }

        // 8️⃣ баланс платника (тільки payer!)
        BigDecimal totalCustomerBalance =
                ledgerRepository.getCustomerBalance(invoice.getCustomer().getCustNo());

        return new CollectiveInvoiceDTO(
                invoiceNo,
                orderSummaries,
                totalBilled,
                totalPaid,
                totalDue,
                totalCustomerBalance
        );
    }


}
