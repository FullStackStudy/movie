package com.movie.repository.chatbot;

import com.movie.entity.chatbot.ChatbotResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatbotRepository extends JpaRepository<ChatbotResponse, Long> {
} 