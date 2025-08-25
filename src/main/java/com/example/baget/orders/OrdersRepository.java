package com.example.baget.orders;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Set;


public interface OrdersRepository extends JpaRepository<Orders, Long> {
    @NonNull
    Page<Orders> findAll(@NonNull Pageable pageable);

    Page<Orders> findByBranch_Name(String branchName, Pageable pageable);
    Page<Orders> findByBranchNameIn(Set<String> branchNames, Pageable pageable);

    Page<OrderSummaryView> findSummaryByBranch_Name(String branchName, Pageable pageable);
}
