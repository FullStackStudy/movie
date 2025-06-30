package com.movie.controller;
import com.movie.constant.ResponseType;
import com.movie.dto.ChatbotResponseDto;
import com.movie.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @GetMapping
    public ResponseEntity<ChatbotResponseDto> getResponse(@RequestParam String message) {
        return ResponseEntity.ok(chatbotService.generateResponse(message));
    }

}