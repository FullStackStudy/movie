package com.movie.repository;

import com.movie.entity.ChatbotResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatbotRepository extends JpaRepository<ChatbotResponse, Long> {
}