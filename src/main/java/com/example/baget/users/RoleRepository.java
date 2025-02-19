package com.example.baget.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRepository  extends JpaRepository<Role, Long> {
    List<Role> findAllByNameIn(List<String> roles);
}
