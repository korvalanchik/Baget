package com.example.baget.items;

import com.example.baget.orders.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface ItemsRepository extends JpaRepository<Items, Long> {
    void deleteAllByOrder(Orders order);
    List<Items> findByOrder(Orders order);

    List<Items> findByOrderOrderNo(Long orderNo);
}
