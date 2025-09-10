package com.example.baget.buh;

import com.example.baget.customer.Customer;
import com.example.baget.customer.CustomerRepository;
import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final OrdersRepository ordersRepository;
    private final CustomerRepository customerRepository;

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
//                        double overpayment = transaction.getAmount() - currentDue;
//                        createRefundTransaction(
//                                order.getCustomer(),
//                                overpayment,
//                                "Переплата по замовленню №" + order.getOrderNo()
//                        );
                    }

//                    order.setIncome(Optional.ofNullable(order.getIncome()).orElse(0.0) + transaction.getAmount());
                    updateOrderStatus(order);
                }

                case "REFUND" -> {
                    double currentPaid = Optional.ofNullable(order.getAmountPaid()).orElse(0.0);
                    double refundAmount = transaction.getAmount();

                    if (refundAmount < currentPaid) {
                        // Частковий REFUND
                        order.setAmountPaid(currentPaid - refundAmount);
                        order.setAmountDueN(order.getAmountDueN() + refundAmount);
                        order.setIncome(order.getIncome() - refundAmount);

                    } else {
                        if (refundAmount > currentPaid) {
                            throw new IllegalArgumentException("It is impossible to return more than the amount paid.");
                        }
                        // Повний REFUND → скасування замовлення
                        order.setAmountPaid(0.0);
                        order.setAmountDueN(0.0);

                        order.setStatusOrder(10);
                        transaction.setNote("Повний REFUND → скасування замовлення №" + order.getOrderNo());
                        // Створюємо додаткову транзакцію на клієнта, якщо потрібно
//                        createRefundTransaction(
//                                order.getCustomer(),
//                                refundAmount - currentPaid,
//                                "REFUND напряму клієнту через неможливість виконати замовлення"
//                        );
                    }

                    updateOrderStatus(order);
                }

                case "CANCEL" -> {
                    order.setStatusOrder(5);
                    order.setAmountDueN(0.0);

//                    double paid = Optional.ofNullable(order.getAmountPaid()).orElse(0.0);
//                    createRefundTransaction(
//                            order.getCustomer(),
//                            paid,
//                            "Автоматичне повернення після відміни замовлення"
//                    );
                    order.setAmountPaid(0.0);
                    transaction.setNote("Автоматичне повернення після відміни замовлення №" + order.getOrderNo());
                }

                case "ADJUSTMENT", "CHARGE" ->
                    order.setAmountDueN(
                            Optional.ofNullable(order.getAmountDueN()).orElse(0.0) + transaction.getAmount()
                    );

                case "TRANSFER" -> {
                    // логіка переказу по замовленню
                }

                case "DISCOUNT" ->
                    order.setAmountDueN(
                            Optional.ofNullable(order.getAmountDueN()).orElse(0.0) - transaction.getAmount()
                    );

                case "ADVANCE_PAYMENT" ->
                    order.setAmountPaid(
                            Optional.ofNullable(order.getAmountPaid()).orElse(0.0) + transaction.getAmount()
                    );

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
                    transaction.setNote("Авансовий платіж клієнта");

                // контроль, щоб повернення не перевищило поточного балансу клієнта
                case "REFUND" -> {
                    Double currentBalance = transactionRepository.getCustomerBalance(customer.getCustNo());
                    if (transaction.getAmount() > currentBalance) {
                        throw new IllegalArgumentException("Refund перевищує баланс клієнта");
                    }
                    transaction.setNote("Повернення коштів клієнту");
                }

                case "TRANSFER" ->
                    // Наприклад, переказ між клієнтами
                    transaction.setNote("Переказ між клієнтами");
                    // Тут можна додати логіку пошуку отримувача і створення "дзеркальної" транзакції

                default -> throw new IllegalArgumentException("Transaction type " + typeCode + " requires orderNo");
            }
        } else {
            throw new IllegalArgumentException("Transaction must be linked to Order or Customer");
        }
        transaction.setStatus("Completed");
        return transactionRepository.save(transaction);
    }

    private void createRefundTransaction(Customer customer, double amount, String note) {
        //        if (amount <= 0) return; // нічого не робимо, якщо сума <= 0

        Transaction refund = new Transaction();
        refund.setCustomer(customer);
        refund.setTransactionType(transactionTypeRepository.findByCode("REFUND"));
        refund.setAmount(amount);
        refund.setTransactionDate(OffsetDateTime.now());
        refund.setStatus("Completed");
        refund.setNote(note);

        transactionRepository.save(refund);
    }


    private void updateOrderStatus(Orders order) {
        if (order.getAmountDueN() == 0d) {
            order.setStatusOrder(4); // 4 = оплачено / завершено
        } else {
            order.setStatusOrder(9); // 9 = частково оплачено
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
}
