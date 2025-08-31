package com.example.baget.buh;

import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import jakarta.persistence.EntityNotFoundException;
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


    private Transaction processTransaction(Transaction transaction) {
        // Встановлюємо дату, якщо не задано
        if (transaction.getTransactionDate() == null) {
            transaction.setTransactionDate(OffsetDateTime.now());
        }

        // Залежно від типу транзакції
        String typeCode = transaction.getTransactionType().getCode();

        if (transaction.getOrder().getOrderNo() != null) {
            Orders order = ordersRepository.findById(transaction.getOrder().getOrderNo())
                    .orElseThrow(() -> new RuntimeException("Order not found"));

            switch (typeCode) {
                case "INVOICE":
                    // Просто створюємо рахунок – не змінюємо AmountPaid
                    order.setStatusOrder(1); // наприклад, 1 = створено
                    break;

                case "PAYMENT":
                    order.setAmountPaid(
                            Optional.ofNullable(order.getAmountPaid()).orElse(0.0) + transaction.getAmount()
                    );
                    updateOrderStatus(order);
                    break;

                case "REFUND":
                    order.setAmountPaid(
                            Optional.ofNullable(order.getAmountPaid()).orElse(0.0) - transaction.getAmount()
                    );
                    updateOrderStatus(order);
                    break;

                case "ADJUSTMENT", "CHARGE":
                    order.setAmountDueN(
                            Optional.ofNullable(order.getAmountDueN()).orElse(0.0) + transaction.getAmount()
                    );
                    break;

                case "TRANSFER":
                    // Логіка переказу – можливо між рахунками/клієнтами
                    break;

                case "DISCOUNT":
                    order.setAmountDueN(
                            Optional.ofNullable(order.getAmountDueN()).orElse(0.0) - transaction.getAmount()
                    );
                    break;

                case "CANCEL":
                    order.setStatusOrder(0); // наприклад, 0 = скасовано
                    break;

                case "ADVANCE_PAYMENT":
                    order.setAmountPaid(
                            Optional.ofNullable(order.getAmountPaid()).orElse(0.0) + transaction.getAmount()
                    );
                    break;

                default:
                    throw new IllegalArgumentException("Unknown transaction type: " + typeCode);
            }

            ordersRepository.save(order);
        }

        transaction.setStatus("Completed");
        return transactionRepository.save(transaction);
    }

    private void updateOrderStatus(Orders order) {
        if (order.getAmountPaid() >= order.getItemsTotal() + order.getFreight() + order.getTaxRate()) {
            order.setStatusOrder(4); // 4 = оплачено / завершено
        } else {
            order.setStatusOrder(9); // 9 = частково оплачено
        }
    }



    @Transactional
    public TransactionDTO createTransaction(TransactionDTO dto) {
        // 1. шукаємо замовлення
        Orders order = ordersRepository.findById(dto.getOrderNo())
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + dto.getOrderNo()));

        // 2. шукаємо тип транзакції
        TransactionType transactionType = transactionTypeRepository.findById(dto.getTransactionTypeId())
                .orElseThrow(() -> new IllegalArgumentException("TransactionType not found with id: " + dto.getTransactionTypeId()));

        // 3. мапимо DTO -> Entity
        Transaction transaction = toEntity(dto);
        transaction.setOrder(order);
        transaction.setTransactionType(transactionType);

        // 4. зберігаємо
        Transaction saved = transactionRepository.save(transaction);

        // 5. повертаємо DTO
        return toDto(saved);
    }


    public List<TransactionDTO> getTransactionsByOrder(Long orderNo) {
        List<Transaction> transactions = transactionRepository.findByOrder_OrderNo(orderNo);

        return transactions.stream()
                .map(this::toDto)
                .toList();
    }


    @Transactional
    public void createOrderWithBalancePayment(Orders order) {
        // 1. Поточний баланс
        Double balance = transactionRepository.getCustomerBalance(order.getCustomer().getCustNo());

        double orderTotal = order.getItemsTotal() + order.getTaxRate() + order.getFreight();

        if (balance >= orderTotal) {
            // 2. Повністю оплачено
            order.setAmountPaid(orderTotal);
            order.setStatusOrder(4); // 4 = Оплачено
            ordersRepository.save(order);

            // 3. Транзакція списання
            Transaction debit = new Transaction();
            debit.setCustomer(order.getCustomer()); // ✅ замість customerId
            debit.setOrder(order); // ✅ замість orderNo
            debit.setTransactionType(transactionTypeRepository.findByCode("PAYMENT")); // ✅
            debit.setAmount(orderTotal);
            debit.setTransactionDate(OffsetDateTime.now());
            debit.setStatus("Completed");
            transactionRepository.save(debit);

        } else {
            // 4. Часткова оплата
            order.setAmountPaid(balance);
            order.setStatusOrder(9); // Частково оплачено
            ordersRepository.save(order);

            if (balance > 0) {
                Transaction partial = new Transaction();
                partial.setCustomer(order.getCustomer()); // ✅
                partial.setOrder(order); // ✅
                partial.setTransactionType(transactionTypeRepository.findByCode("PAYMENT")); // ✅
                partial.setAmount(balance);
                partial.setTransactionDate(OffsetDateTime.now());
                partial.setStatus("Completed");
                transactionRepository.save(partial);
            }
        }
    }






    @Transactional
    public TransactionDTO addTransaction(Long orderNo, TransactionDTO transactionDTO) {
        Orders order = ordersRepository.findById(orderNo)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id " + orderNo));

        TransactionType type = transactionTypeRepository.findById(transactionDTO.getTransactionTypeId())
                .orElseThrow(() -> new EntityNotFoundException("TransactionType not found with id " + transactionDTO.getTransactionTypeId()));

        Transaction transaction = toEntity(transactionDTO);
        transaction.setOrder(order);
        transaction.setTransactionType(type);

        Transaction saved = transactionRepository.save(transaction);

        return toDto(saved);
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


    public TransactionDTO toDto(Transaction entity) {
        if (entity == null) {
            return null;
        }

        return TransactionDTO.builder()
                .transactionId(entity.getTransactionId())
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
