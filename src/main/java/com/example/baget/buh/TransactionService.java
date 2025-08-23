package com.example.baget.buh;

import com.example.baget.orders.Orders;
import com.example.baget.orders.OrdersRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;
    private final OrdersRepository ordersRepository;


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

    public List<TransactionTypeDTO> getAllTransactionTypes() {
        return transactionTypeRepository.findAll()
                .stream()
                .map(this::toDtoType)
                .toList();
    }

    private TransactionTypeDTO toDtoType(TransactionType type) {
        TransactionTypeDTO dto = new TransactionTypeDTO();
        dto.setTypeId(type.getTypeId());
        dto.setDescription(type.getDescription()); // або description, якщо так називається
        return dto;
    }


    public TransactionDTO toDto(Transaction entity) {
        if (entity == null) {
            return null;
        }

        return TransactionDTO.builder()
                .transactionId(entity.getTransactionId())
                .orderNo(entity.getOrder().getOrderNo())

                .transactionTypeId(entity.getTransactionType().getTypeId())
                .transactionTypeCode(entity.getTransactionType().getCode())
                .transactionTypeDescription(entity.getTransactionType().getDescription())

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

        Transaction tx = new Transaction();
        tx.setTransactionId(dto.getTransactionId());

        tx.setTransactionDate(dto.getTransactionDate() != null ? dto.getTransactionDate() : OffsetDateTime.now());
        tx.setAmount(dto.getAmount());
        tx.setReference(dto.getReference());
        tx.setStatus(dto.getStatus());
        tx.setNote(dto.getNote());

        return tx;
    }
}
