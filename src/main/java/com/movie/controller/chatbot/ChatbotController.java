package com.movie.controller.chatbot;
import com.movie.constant.ResponseType;
import com.movie.dto.chatbot.ChatbotResponseDto;
import com.movie.service.chatbot.OpenAiService;
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

    /*private final ChatbotService chatbotService;

    @GetMapping
    public ResponseEntity<ChatbotResponseDto> getResponse(@RequestParam String message) {
        return ResponseEntity.ok(chatbotService.generateResponse(message));
    }*/

    /*private final OpenAiService openAiService;

    @GetMapping
    public ChatbotResponseDto chatbot(@RequestParam String message) {
        //GPT API 호출
        String gptResponse = openAiService.ask(message);

        return ChatbotResponseDto.builder()
                .response(gptResponse)
                .type(ResponseType.TEXT)
                .build();
    }*/

    private final OpenAiService openAiService;

    @GetMapping
    public ChatbotResponseDto chatbot(@RequestParam String message) {
        return openAiService.generateResponse(message);
    }

} 