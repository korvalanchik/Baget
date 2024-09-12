package com.example.baget.parts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;


public interface PartsRepository extends JpaRepository<Parts, Long> {
    @NonNull
    Page<Parts> findAll(@NonNull Pageable pageable);

}
