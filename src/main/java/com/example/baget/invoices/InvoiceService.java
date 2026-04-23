package com.example.baget.invoices;

import com.example.baget.customer.Customer;
import com.example.baget.customer.CustomerRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Transactional
    public InvoiceDTO mergeInvoices(MergeInvoicesRequest request) {

        List<Long> invoiceIds = request.invoiceIds();
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

        // 4️⃣ Збираємо всі orders
        List<InvoiceOrder> allInvoiceOrders =
                invoiceOrderRepository.findByInvoice_IdIn(invoiceIds);

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

        // 7️⃣ Створюємо новий (консолідований) інвойс
        Invoice newInvoice = Invoice.builder()
                .invoiceNo(invoiceNo)
                .customer(invoices.get(0).getCustomer()) // ⚠️ базовий клієнт (для відображення)
                .payer(payer) // 🔥 КЛЮЧОВЕ
                .type(InvoiceEnums.InvoiceType.CONSOLIDATED)
                .status(InvoiceEnums.InvoiceStatus.ISSUED)
                .lifecycle(InvoiceEnums.InvoiceLifecycle.ACTIVE)
                .totalAmount(totalAmount)
                .note(request.note())
                .createdAt(now)
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

        // 9️⃣ Старі інвойси → MERGED + встановлюємо payer
        for (Invoice old : invoices) {
            old.setLifecycle(InvoiceEnums.InvoiceLifecycle.MERGED);
            old.setPayer(payer); // 🔥 ОСНОВНА ЛОГІКА
        }

        // 🔟 Оновлюємо номер рахунку у замовленнях (для UI)
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
