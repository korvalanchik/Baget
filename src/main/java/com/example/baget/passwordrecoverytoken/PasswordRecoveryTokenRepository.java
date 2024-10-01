package com.example.baget.passwordrecoverytoken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordRecoveryTokenRepository extends JpaRepository<PasswordRecoveryToken, Long> {
    Optional<PasswordRecoveryToken> findByToken(String token);

    @Modifying
    @Query("DELETE FROM PasswordRecoveryToken t WHERE t.expiryDate < :now")
    void deleteAllExpiredTokens(@Param("now") LocalDateTime now);

}