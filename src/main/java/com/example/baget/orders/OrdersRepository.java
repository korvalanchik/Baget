package com.example.baget.orders;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;


public interface OrdersRepository extends JpaRepository<Orders, Long> {

    @NonNull
    Page<Orders> findAll(@NonNull Pageable pageable);

    // ADMIN бачить усі замовлення
    Page<OrderProjections.AdminOrderView> findAllAdminBy(Pageable pageable);

    // COUNTER бачить замовлення по кількох філіях
    Page<OrderProjections.CounterOrderView> findByBranch_NameIn(Set<String> branchNames, Pageable pageable);

    // USER бачить лише замовлення конкретної філії
    Page<OrderProjections.UserOrderView> findByBranch_Name(String branchName, Pageable pageable);

    // Коротке представлення (наприклад, для дашбордів)
    Page<OrderSummaryView> findAllSummaryBy(Pageable pageable);
    List<Orders> findByRahFacNo(Long rahFacNo);
    boolean existsByRahFacNo(Long rahFacNo);

    //    Optional<OrderPublicSummaryView> findByOrderNo(Long orderNo);
    Optional<OrderProjections.PublicOrderView> findPublicByOrderNo(Long orderNo);
    Optional<OrderProjections.PrivateOrderView> findPrivateByOrderNo(Long orderNo);
    Optional<Orders> findByPublicId(String publicId);
}
