package com.movie.repository;

import com.movie.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    
    Optional<EmailVerificationToken> findByToken(String token);
    
    Optional<EmailVerificationToken> findByEmailAndUsedFalse(String email);
    
    Optional<EmailVerificationToken> findByEmail(String email);
    
    void deleteByEmail(String email);
} 