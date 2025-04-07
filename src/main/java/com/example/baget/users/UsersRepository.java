package com.example.baget.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;


public interface UsersRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String userName);
    boolean existsByEmail(String email);
    Optional<User> findByTelegramId(Long telegramId);
    @Query("SELECT u.id, u.username FROM User u")
    List<Object[]> findAllUserIdAndUsername();

}
