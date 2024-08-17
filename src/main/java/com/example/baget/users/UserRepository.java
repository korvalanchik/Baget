package com.example.baget.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUsername(String username);
}
