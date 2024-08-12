package com.example.baget.items;

import com.example.baget.orders.Orders;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ItemsRepository extends JpaRepository<Items, ItemId> {
    void deleteAllByOrder(Orders order);
    void deleteById(@NotNull ItemId itemId);
    List<Items> findByOrder(Orders order);

    List<Items> findByOrderOrderNo(Long orderNo);
}
