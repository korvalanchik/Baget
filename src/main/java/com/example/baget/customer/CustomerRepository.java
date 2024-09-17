package com.example.baget.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;


public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @NonNull
    Page<Customer> findAll(@NonNull Pageable pageable);

}
