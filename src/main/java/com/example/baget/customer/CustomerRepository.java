package com.example.baget.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface CustomerRepository extends JpaRepository<Customer, Long> {
    @NonNull
    Page<Customer> findAll(@NonNull Pageable pageable);
    List<CustomerSelectView> findAllBy();
    List<Customer> findByMobileContaining(String prefix);
    Optional<Customer> findFirstByMobileContainingOrderByCustNoAsc(String phone);
    Optional<Customer> findTopByCompanyStartingWithOrderByCustNoDesc(String prefix);
    @SuppressWarnings("JpaQlInspection")
    @Query("""
        select new com.example.baget.customer.CustomerBalanceDTO(
             c.custNo,
             coalesce(c.company, c.contact),
             c.mobile,
             count(distinct o.orderNo),
             coalesce((
                 select sum(ct.amount)
                 from CustomerTransaction ct
                 where ct.customer = c
                   and ct.active = true
                   and (:dateEnd is null or ct.createdAt <= :dateEnd)
             ), 0),
             (
                 select max(ct.createdAt)
                 from CustomerTransaction ct
                 where ct.customer = c
                   and ct.active = true
                   and (:dateEnd is null or ct.createdAt <= :dateEnd)
             )
         )
         from Customer c
         join c.orders o
         where o.branch.branchNo in :branchNos
         group by c.custNo, c.company, c.contact, c.mobile
         having count(o.orderNo) > 0
            and (
                :debtOnly = false or
                coalesce((
                    select sum(ct.amount)
                    from CustomerTransaction ct
                    where ct.customer = c
                      and ct.active = true
                      and (:dateEnd is null or ct.createdAt <= :dateEnd)
                ), 0) > 0
            )
         order by coalesce((
             select sum(ct.amount)
             from CustomerTransaction ct
             where ct.customer = c
               and ct.active = true
               and (:dateEnd is null or ct.createdAt <= :dateEnd)
         ), 0) desc
                                                     
    """)
    List<CustomerBalanceDTO> findClientBalances(
            @Param("branchNos") Collection<Long> branchNos,
            @Param("debtOnly") boolean debtOnly,
            @Param("dateEnd") OffsetDateTime dateEnd
    );

}
