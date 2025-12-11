package com.example.baget.buh;

import com.example.baget.customer.Customer;
import com.example.baget.customer.CustomerRepository;
import com.example.baget.orders.OrderPaySummaryDTO;
import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import com.example.baget.util.TransactionException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final OrdersRepository ordersRepository;
    private final CustomerRepository customerRepository;


    @Transactional
    public Transaction processTransaction(Transaction txInput) {

        Customer customer = txInput.getCustomer();
        Orders order = txInput.getOrder();
        String typeCode = txInput.getTransactionType().getCode();
        double amount = txInput.getAmount();

        // встановлюємо дату транзакції
        if (txInput.getTransactionDate() == null) {
            txInput.setTransactionDate(OffsetDateTime.now());
        }

        // 1️⃣ ВАЛІДАЦІЯ
        if (customer == null) {
            throw new TransactionException("Transaction must have customerId");
        }

        if (amount <= 0) {
            throw new TransactionException("Amount must be positive");
        }

        // 2️⃣ ОТРИМАННЯ БАЛАНСУ
        double balance = Optional.ofNullable(
                transactionRepository.getCustomerBalance(customer.getCustNo())
        ).orElse(0.0);

        // 3️⃣ ЛОГІКА ДЛЯ ВНЕСЕННЯ КОШТІВ (PAYMENT)
        if (typeCode.equals("PAYMENT")) {

            // Це завжди означає, що клієнт вніс гроші.
            txInput.setNote("Внесення коштів клієнтом");

            // Створюємо транзакцію надходження на баланс
            Transaction incoming = new Transaction();
            incoming.setTransactionDate(txInput.getTransactionDate());
            incoming.setTransactionType(txInput.getTransactionType()); // PAYMENT
            incoming.setCustomer(customer);
            incoming.setAmount(amount); // +amount (поповнення)
            incoming.setStatus("Completed");
            incoming.setReference(txInput.getReference());
            incoming.setNote("Поповнення балансу клієнта №" + customer.getCustNo());

            transactionRepository.save(incoming);

            // Оновлюємо баланс
            balance += amount;

            // Якщо замовлення не вибрано → просто поповнення
            if (order == null) {
                return incoming;
            }

            // 4️⃣ ОПЛАТА ЗАМОВЛЕННЯ ІЗ БАЛАНСУ
            double orderDue = Optional.ofNullable(order.getAmountDueN()).orElse(0.0);
            double orderPaid = Optional.ofNullable(order.getAmountPaid()).orElse(0.0);
            double orderIncome = Optional.ofNullable(order.getIncome()).orElse(0.0);

            if (orderDue <= 0) {
                // замовлення вже оплачене
                Transaction noop = new Transaction();
                noop.setTransactionDate(txInput.getTransactionDate());
                noop.setTransactionType(txInput.getTransactionType());
                noop.setCustomer(customer);
                noop.setOrder(order);
                noop.setAmount(0.0);
                noop.setStatus("Completed");
                noop.setNote("Замовлення вже оплачено. Кошти додано на баланс.");
                return transactionRepository.save(noop);
            }

            // сума, яку реально можна списати
            double toPay = Math.min(balance, orderDue);

            // створюємо транзакцію списання
            Transaction deduction = new Transaction();
            deduction.setTransactionDate(txInput.getTransactionDate());
            deduction.setTransactionType(transactionTypeRepository.findByCode("ORDER_PAYMENT")
                    .orElseThrow(() -> new IllegalArgumentException("Missing ORDER_PAYMENT type")));
            deduction.setCustomer(customer);
            deduction.setOrder(order);
            deduction.setAmount(-toPay); // списання = мінус
            deduction.setStatus("Completed");
            deduction.setNote("Списання з балансу на оплату замовлення №" + order.getOrderNo());

            transactionRepository.save(deduction);

            // оновлюємо баланс після списання
            balance -= toPay;

            // 5️⃣ ОНОВЛЕННЯ ЗАМОВЛЕННЯ
            order.setAmountPaid(orderPaid + toPay);
            order.setAmountDueN(orderDue - toPay);
            order.setIncome(orderIncome + toPay);

            updateOrderStatusFromPayments(order);
            ordersRepository.save(order);

            return deduction;
        }

        if(typeCode.equals("INVOICE")) {
            if (order == null) {
                throw new TransactionException("Не встановлено № замовлення для інвойсу.");
            }

            Transaction invoiceTx = new Transaction();
            invoiceTx.setTransactionDate(txInput.getTransactionDate());
            invoiceTx.setTransactionType(txInput.getTransactionType()); // INVOICE
            invoiceTx.setCustomer(customer);
            invoiceTx.setOrder(order);
            invoiceTx.setAmount(0.0); // інвойс — без реальних грошей
            invoiceTx.setStatus("Issued"); // відрізняємо від Completed
            invoiceTx.setReference(txInput.getReference());
            invoiceTx.setNote("Виставлено рахунок на замовлення №" + order.getOrderNo());

            return transactionRepository.save(invoiceTx);
        }

        if(typeCode.equals("REFUND")) {
            if (order != null) {
                // Повернення за конкретне замовлення
                double paid = Optional.ofNullable(order.getAmountPaid()).orElse(0.0);
                double refundAmount = amount;

                if (refundAmount > paid) {
                    throw new TransactionException("Неможливо повернути більше, ніж сплачено за замовлення.");
                }

                Transaction refundTx = new Transaction();
                refundTx.setTransactionDate(txInput.getTransactionDate());
                refundTx.setTransactionType(txInput.getTransactionType());
                refundTx.setCustomer(customer);
                refundTx.setOrder(order);
                refundTx.setAmount(-refundAmount); // списуємо як негативну транзакцію
                refundTx.setStatus("Completed");
                refundTx.setReference(txInput.getReference());
                refundTx.setNote("Повернення коштів за замовлення №" + order.getOrderNo());
                transactionRepository.save(refundTx);

                // Оновлюємо замовлення
                order.setAmountPaid(paid - refundAmount);
                order.setAmountDueN(Optional.ofNullable(order.getAmountDueN()).orElse(0.0) + refundAmount);
                order.setIncome(Optional.ofNullable(order.getIncome()).orElse(0.0) - refundAmount);
                updateOrderStatusFromPayments(order);
                ordersRepository.save(order);
                return refundTx;

            } else {
                // Повернення загального балансу клієнта
                if (amount > balance) {
                    throw new TransactionException("Повернення перевищує баланс клієнта.");
                }

                Transaction refundTx = new Transaction();
                refundTx.setTransactionDate(txInput.getTransactionDate());
                refundTx.setTransactionType(txInput.getTransactionType());
                refundTx.setCustomer(customer);
                refundTx.setAmount(-amount);
                refundTx.setStatus("Completed");
                refundTx.setReference(txInput.getReference());
                refundTx.setNote("Повернення коштів клієнту");
                return transactionRepository.save(refundTx);
            }
        }

        if(typeCode.equals("DISCOUNT")) {

            if (order == null) throw new TransactionException("Не встановлено № замовлення для дисконту.");

            double due = Optional.ofNullable(order.getAmountDueN()).orElse(0.0);
            if (amount > due) throw new TransactionException("Сума дисконту перевищує залишок до оплати.");

            order.setAmountDueN(due - amount);

            Transaction discountTx = new Transaction();
            discountTx.setTransactionDate(txInput.getTransactionDate());
            discountTx.setTransactionType(txInput.getTransactionType());
            discountTx.setCustomer(customer);
            discountTx.setOrder(order);
            discountTx.setAmount(-amount);
            discountTx.setStatus("Completed");
            discountTx.setReference(txInput.getReference());
            discountTx.setNote("Дисконт до замовлення №" + order.getOrderNo());
            ordersRepository.save(order);

            return transactionRepository.save(discountTx);

        }

        if(typeCode.equals("CANCEL")) {
            if (order == null) throw new TransactionException("Не встановлено № замовлення для відміни.");

            double due = Optional.ofNullable(order.getAmountDueN()).orElse(0.0);
            double paid = Optional.ofNullable(order.getAmountPaid()).orElse(0.0);

            if (Math.round(amount*100) != Math.round(due*100)) {
                throw new TransactionException("Сума повинна збігатися з залишком авансу (" + due + ")");
            }

            // Закриваємо замовлення
            order.setAmountDueN(0.0);
            order.setAmountPaid(0.0);
            order.setStatusOrder(5); // відміна
            updateOrderStatusFromPayments(order);
            ordersRepository.save(order);

            Transaction cancelTx = new Transaction();
            cancelTx.setTransactionDate(txInput.getTransactionDate());
            cancelTx.setTransactionType(txInput.getTransactionType());
            cancelTx.setCustomer(customer);
            cancelTx.setOrder(order);
            cancelTx.setAmount(due); // записуємо як дохід
            cancelTx.setStatus("Completed");
            cancelTx.setReference(txInput.getReference());
            cancelTx.setNote("Списано як дохід при відмові від замовлення №" + order.getOrderNo());
            return transactionRepository.save(cancelTx);
        }

        throw new TransactionException("Unsupported transaction type: " + typeCode);
    }




/*
    private Transaction processTransaction(Transaction transaction) {
        // Встановлюємо дату, якщо не задано
        if (transaction.getTransactionDate() == null) {
            transaction.setTransactionDate(OffsetDateTime.now());
        }

        String typeCode = transaction.getTransactionType().getCode();

        // Якщо є замовлення
        if (transaction.getOrder() != null && transaction.getOrder().getOrderNo() != null) {
            Orders order = transaction.getOrder();
            switch (typeCode) {
                case "INVOICE" -> {
                    transaction.setAmount(-transaction.getAmount());
                    transaction.setStatus("Completed");
                    transaction.setNote("Виставлено рахунок до замовлення №" + order.getOrderNo());
                    order.setStatusOrder(7); // До оплати
                }
                case "PAYMENT" -> {
                    double currentPaid = Optional.ofNullable(order.getAmountPaid()).orElse(0.0);
                    double currentDue  = Optional.ofNullable(order.getAmountDueN()).orElse(0.0);
                    double currentIncome  = Optional.ofNullable(order.getIncome()).orElse(0.0);

                    double newPaid = currentPaid + transaction.getAmount();

                    if (transaction.getAmount() <= currentDue) {
                        // Немає переплати
                        order.setAmountPaid(newPaid);
                        order.setAmountDueN(currentDue - transaction.getAmount());
                        order.setIncome(currentIncome + transaction.getAmount());
                        if (transaction.getAmount() < currentDue) {
                            transaction.setNote("Часткова оплата замовлення №" + order.getOrderNo());
                        } else {
                            transaction.setNote("Оплата замовлення №" + order.getOrderNo());
                        }
                    } else {
                        // Є переплата
                        order.setAmountPaid(currentPaid + currentDue); // замовлення закриваємо
                        order.setAmountDueN(0.0);
                        order.setIncome(currentIncome + currentDue);
                        transaction.setNote("Оплата замовлення №" + order.getOrderNo() + " з поповненням балансу");
                    }

                    updateOrderStatus(order);
                }

                case "REFUND" -> {
                    double currentPaid = Optional.ofNullable(order.getAmountPaid()).orElse(0.0);
                    double refundAmount = transaction.getAmount();

                    if (refundAmount <= currentPaid) {
                        // Частковий REFUND
                        order.setAmountPaid(currentPaid - refundAmount);
                        order.setAmountDueN(order.getAmountDueN() + refundAmount);
                        order.setIncome(order.getIncome() - refundAmount);
                        transaction.setAmount(-refundAmount);
                        transaction.setNote(
                                (refundAmount < currentPaid ? "Часткове " : "") +
                                        "Повернення коштів за замовлення №" + order.getOrderNo()
                        );
                    } else {
                        throw new TransactionException("Не можливо повернути за замовлення більше ніж сплачено.");
                    }

                    updateOrderStatus(order);
                }

                case "CANCEL" -> {
                    int aCents = (int) Math.round(transaction.getAmount() * 100);
                    int bCents = (int) Math.round(order.getAmountDueN() * 100);

                    if (aCents != bCents) {
                        throw new TransactionException(
                                "Сума повинна збігатися з авансом (" + order.getAmountDueN() + ")."
                        );
                    }
                    transaction.setAmount(order.getAmountDueN());
                    order.setStatusOrder(5);
                    order.setAmountDueN(0.0);
                    order.setAmountPaid(0.0);
                    transaction.setNote("Кошти списано як дохід при відмові від замовлення №" + order.getOrderNo());
                }

                case "ADJUSTMENT", "CHARGE" -> {
//                    order.setAmountDueN(Optional.ofNullable(order.getAmountDueN()).orElse(0.0) + transaction.getAmount());
                }

                case "TRANSFER" -> {
                    // логіка переказу по замовленню
                }

                case "DISCOUNT" -> {
                    double currentDue = Optional.ofNullable(order.getAmountDueN()).orElse(0.0);
                    double discountAmount = transaction.getAmount();

                    if (discountAmount <= currentDue) {
                        order.setAmountDueN(currentDue - discountAmount);
                        transaction.setNote("Дисконт до замовлення №" + order.getOrderNo());
                    } else {
                        throw new TransactionException("Занадто великий дисконт");
                    }
                }

                case "ADVANCE_PAYMENT" ->
                    transaction.setNote("Поповнення балансу клієнта: " + order.getCustomer().getCompany());

                default ->
                    throw new IllegalArgumentException("Unknown transaction type: " + typeCode);
            }
            ordersRepository.save(order);

        } else if (transaction.getCustomer() != null) {
            // --- Логіка для клієнта ---
            Customer customer = transaction.getCustomer();

            switch (typeCode) {
                // клієнт поповнив баланс (аванс)
                // баланс рахується через репозиторій, тому просто зберігаємо транзакцію
                case "ADVANCE_PAYMENT" ->
                    transaction.setNote("Авансовий платіж клієнта: " + customer.getCompany());

                // контроль, щоб повернення не перевищило поточного балансу клієнта
                case "REFUND" -> {
                    Double currentBalance = transactionRepository.getCustomerBalance(customer.getCustNo());
                    if (transaction.getAmount() > currentBalance) {
                        throw new TransactionException("Refund перевищує баланс клієнта");
                    }
                    transaction.setNote("Повернення коштів клієнту");
                }

                case "TRANSFER" ->
                    // Наприклад, переказ між клієнтами
                    transaction.setNote("Переказ між клієнтами");
                    // Тут можна додати логіку пошуку отримувача і створення "дзеркальної" транзакції

                default -> throw new TransactionException("Transaction type " + typeCode + " requires orderNo");
            }
        } else {
            throw new IllegalArgumentException("Transaction must be linked to Order or Customer");
        }
        transaction.setStatus("Completed");
        return transactionRepository.save(transaction);
    }
*/
/*
    private void updateOrderStatus(Orders order) {
        if (order.getAmountDueN() == 0d) {
            order.setStatusOrder(4); // 4 = оплачено / завершено
        } else {
            order.setStatusOrder(9); // 9 = частково оплачено
        }
    }
*/

    private void updateOrderStatusFromPayments(Orders order) {

        double paid = transactionRepository.getOrderPaymentsSum(order.getOrderNo());
        double total = order.getAmountPaid();

        if (paid >= total) {
            order.setStatusOrder(4); // Оплачено
        } else if (paid > 0) {
            order.setStatusOrder(9); // Частково оплачено
        } else {
            order.setStatusOrder(7); // До оплати
        }
    }


    @Transactional
    public TransactionDTO createTransaction(TransactionDTO dto) {
        Transaction transaction = toEntity(dto);

        transaction.setTransactionType(
                transactionTypeRepository.findById(dto.getTransactionTypeId())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown type: " + dto.getTransactionTypeId()))
        );

        transaction.setCustomer(
                customerRepository.findById(dto.getCustomerId())
                        .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + dto.getCustomerId()))
        );

        if (dto.getOrderNo() != null) {
            Orders order = ordersRepository.findById(dto.getOrderNo())
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + dto.getOrderNo()));
            transaction.setOrder(order);
        }

        // --- універсальна обробка ---
        Transaction saved = processTransaction(transaction);

        return toDTO(saved);
    }


    @Transactional
    public List<TransactionDTO> createInvoiceTransactions(Long invoiceNo, double amount) {
        List<Orders> orders = ordersRepository.findByRahFacNo(invoiceNo);
        if (orders.isEmpty()) {
            throw new EntityNotFoundException("Invoice " + invoiceNo + " not found");
        }

        Set<Long> customerNos = orders.stream()
                .map(o -> o.getCustomer().getCustNo())
                .collect(Collectors.toSet());

        double totalDue = orders.stream()
                .mapToDouble(Orders::getAmountDueN)
                .sum();

        if (customerNos.size() > 1 && amount > totalDue) {
            throw new IllegalArgumentException("Оплата перевищує суму інвойсу для кількох клієнтів");
        }

        List<TransactionDTO> results = new ArrayList<>();
        double remaining = amount;

        Long paymentTypeId = transactionTypeRepository.findByCode("PAYMENT")
                .orElseThrow(() -> new IllegalArgumentException("Missing PAYMENT type"))
                .getTypeId();
        Long advanceTypeId = transactionTypeRepository.findByCode("ADVANCE_PAYMENT")
                .orElseThrow(() -> new IllegalArgumentException("Missing ADVANCE_PAYMENT type"))
                .getTypeId();


        for (Orders order : orders) {
            if (remaining <= 0) break;

            double due = order.getAmountDueN();
            if (due > 0) {
                double payment = Math.min(remaining, due);

                TransactionDTO dto = new TransactionDTO();
                dto.setTransactionTypeId(paymentTypeId);
                dto.setOrderNo(order.getOrderNo());
                dto.setCustomerId(order.getCustomer().getCustNo());
                dto.setAmount(payment);
                dto.setNote("Оплата замовлення " + order.getOrderNo() + " за інвойсом " + invoiceNo);
                dto.setTransactionDate(OffsetDateTime.now());

                TransactionDTO txResult = createTransaction(dto);
                results.add(txResult);

                remaining -= payment;
            }
        }

        // переплата → аванс
        if (remaining > 0 && customerNos.size() == 1) {
            Long custNo = customerNos.iterator().next();

            TransactionDTO dto = new TransactionDTO();
            dto.setTransactionTypeId(advanceTypeId);
            dto.setCustomerId(custNo);
            dto.setAmount(remaining);
            dto.setNote("Авансовий платіж за інвойсом " + invoiceNo);
            dto.setTransactionDate(OffsetDateTime.now());

            TransactionDTO txResult = createTransaction(dto);
            results.add(txResult);
        }

        return results;
    }


    @Transactional
    public Long createCollectiveInvoice(List<Long> orderNos) {

        Long invoiceNo = generateTodayCode();
        while (ordersRepository.existsByRahFacNo(invoiceNo)) {
            invoiceNo++;
        }
        List<Orders> orders = ordersRepository.findAllById(orderNos);
        for (Orders order : orders) {
            order.setStatusOrder(8); // До оплати
            order.setRahFacNo(invoiceNo);
        }
        ordersRepository.saveAll(orders);
        return invoiceNo;
    }

    public TransactionCollectiveInvoiceDTO getCollectiveInvoice(Long invoiceNo) {

        // Отримуємо замовлення
        List<Orders> orders = ordersRepository.findByRahFacNo(invoiceNo);
        if (orders.isEmpty()) {
            throw new EntityNotFoundException("Invoice " + invoiceNo + " not found");
        }

        // Формуємо зведення по замовленнях
        List<OrderPaySummaryDTO> orderSummaries = orders.stream()
                .map(order -> {
                    double billed = order.getAmountDueN() + order.getAmountPaid();
                    double paid   = order.getAmountPaid();
                    double due    = order.getAmountDueN();
                    return new OrderPaySummaryDTO(order.getOrderNo(), billed, paid, due);
                })
                .toList();

        // Підсумкові значення
        double totalBilled = orderSummaries.stream().mapToDouble(OrderPaySummaryDTO::billed).sum();
        double totalPaid   = orderSummaries.stream().mapToDouble(OrderPaySummaryDTO::paid).sum();
        double totalDue    = orderSummaries.stream().mapToDouble(OrderPaySummaryDTO::due).sum();

        // Баланс по всіх клієнтах цього інвойсу
        double totalCustomerBalance = orders.stream()
                .map(o -> o.getCustomer().getCustNo())
                .distinct()
                .mapToDouble(transactionRepository::getCustomerBalance)
                .sum();

        return new TransactionCollectiveInvoiceDTO(
                invoiceNo,
                orderSummaries,
                totalBilled,
                totalPaid,
                totalDue,
                totalCustomerBalance
        );
    }

    public List<TransactionHistoryView> getTransactionsHistoryByInvoice(Long invoiceNo) {
        return transactionRepository.findByOrder_RahFacNoOrderByTransactionDateDesc(invoiceNo);
    }


    @Transactional
    public List<TransactionDTO> createBatchPayments(List<Long> orderNos) {

        List<TransactionDTO> createdInvoices = new ArrayList<>();

        Long invoiceNo = generateTodayCode();
        while (ordersRepository.existsByRahFacNo(invoiceNo)) {
            invoiceNo++;
        }
        List<Orders> orders = ordersRepository.findAllById(orderNos);

        List<Transaction> transactionsToSave = new ArrayList<>();
        List<Orders> ordersToSave = new ArrayList<>();

        for (Orders order : orders) {

            Transaction invoice = new Transaction();
            invoice.setOrder(order);
            invoice.setTransactionType(
                    transactionTypeRepository.findById(10L)
                            .orElseThrow(() -> new IllegalArgumentException("Unknown transaction's type"))
            );
            invoice.setCustomer(
                    customerRepository.findById(order.getCustomer().getCustNo())
                            .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + order.getCustomer().getCustNo()))
            );
            invoice.setAmount(order.getAmountDueN());
            invoice.setStatus("Completed");
            invoice.setTransactionDate(OffsetDateTime.now());
            invoice.setNote("Рахунок-фактура №" + invoiceNo + " до замовлення №" + order.getOrderNo());

            transactionsToSave.add(invoice);

            order.setStatusOrder(8); // До оплати
            order.setRahFacNo(invoiceNo);
            ordersToSave.add(order);
        }

        ordersRepository.saveAll(ordersToSave);
        transactionRepository.saveAll(transactionsToSave);

        for (Transaction invoice : transactionsToSave) {
            createdInvoices.add(toDTO(invoice));
        }

        return createdInvoices;
    }






    public TransactionInfoDTO getTransactionInfo(Long orderNo) {
        Orders order = ordersRepository.findById(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        Double balance = transactionRepository.getCustomerBalance(order.getCustomer().getCustNo());

        return new TransactionInfoDTO(
                order.getOrderNo(),
                order.getCustomer().getCustNo(),
                order.getItemsTotal(),
                order.getAmountPaid(),
                order.getAmountDueN(),
                balance
        );
    }


    public List<TransactionDTO> getTransactionsByOrder(Long orderNo) {
        List<Transaction> transactions = transactionRepository.findByOrder_OrderNo(orderNo);

        return transactions.stream()
                .map(this::toDTO)
                .toList();
    }




    public List<TransactionTypeProjection> getAllTransactionTypes() {
        return transactionTypeRepository.findAllProjectedBy();
    }


    public void completeTransaction(Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        tx.setStatus("Completed");
        transactionRepository.save(tx);
    }

    public void cancelTransaction(Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        tx.setStatus("Canceled");
        transactionRepository.save(tx);
    }


    public TransactionDTO toDTO(Transaction entity) {
        if (entity == null) {
            return null;
        }

        return TransactionDTO.builder()
                .transactionId(entity.getTransactionId())
                .customerId(entity.getCustomer() != null ? entity.getCustomer().getCustNo() : null)
                .orderNo(entity.getOrder() != null ? entity.getOrder().getOrderNo() : null)
                .transactionTypeId(entity.getTransactionType() != null ? entity.getTransactionType().getTypeId() : null)
                .transactionTypeCode(entity.getTransactionType() != null ? entity.getTransactionType().getCode() : null)
                .transactionTypeDescription(entity.getTransactionType() != null ? entity.getTransactionType().getDescription() : null)
                .transactionDate(entity.getTransactionDate())
                .amount(entity.getAmount())
                .reference(entity.getReference())
                .status(entity.getStatus())
                .note(entity.getNote())
                .build();
    }

    public Transaction toEntity(TransactionDTO dto) {
        if (dto == null) {
            return null;
        }

        return Transaction.builder()
                .transactionId(dto.getTransactionId())
                .transactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : OffsetDateTime.now())
                .amount(dto.getAmount())
                .reference(dto.getReference())
                .status(dto.getStatus())
                .note(dto.getNote())
                .build();

    }

    public Long generateTodayCode() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        String result = today.format(formatter) + "001";
        return Long.parseLong(result);
    }

}
