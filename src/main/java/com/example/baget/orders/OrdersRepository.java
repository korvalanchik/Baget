package com.example.baget.orders;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;


public interface OrdersRepository extends JpaRepository<Orders, Long> {
    @NonNull
    Page<Orders> findAll(@NonNull Pageable pageable);
}
