package com.example.baget.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;


public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @NonNull
    Page<Customer> findAll(@NonNull Pageable pageable);

    List<Customer> findByMobileContaining(String prefix);
    Optional<Customer> findFirstByMobileContainingOrderByCustNoAsc(String phone);

//    Customer findPhoneByCustNo(Long custNo);

}
