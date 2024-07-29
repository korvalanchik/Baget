package com.example.baget.items;

import com.example.baget.orders.Orders;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ItemsRepository extends JpaRepository<Items, Long> {
    void deleteAllByOrder(Orders orders);
}
