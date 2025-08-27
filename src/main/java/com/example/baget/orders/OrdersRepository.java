package com.example.baget.orders;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Set;


public interface OrdersRepository extends JpaRepository<Orders, Long> {
    @NonNull
    Page<Orders> findAll(@NonNull Pageable pageable);

//    Page<Orders> findByBranch_Name(String branchName, Pageable pageable);
//    Page<Orders> findByBranchNameIn(Set<String> branchNames, Pageable pageable);

    Page<OrderSummaryView> findSummaryAllBy(Pageable pageable);

    // Admin — бачить усі замовлення
    Page<OrderProjections.AdminOrderView> findAllBy(Pageable pageable);

    // Counter — бачить тільки дозволені філії
    Page<OrderProjections.CounterOrderView> findByBranch_NameIn(Set<String> branchNames, Pageable pageable);

    // User — лише для конкретної філії
    Page<OrderProjections.UserOrderView> findByBranch_Name(String branchName, Pageable pageable);

}
