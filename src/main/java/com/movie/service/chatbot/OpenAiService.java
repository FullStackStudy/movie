package com.movie.service.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.constant.ResponseType;
import com.movie.dto.chatbot.ChatbotResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAiService {


    public ChatbotResponseDto generateResponse(String message) {
        String lower = message.toLowerCase();
        List<String> cinemas = List.of("인천", "성남", "노량진", "부산");
        String systemPrompt = """
                너는 MovieFlex 영화관 웹사이트의 친절한 챗봇이야.
                사용자에게 영화 정보, 시간표, 회원가입, 로그인, 스토어 관련 내용을 도와줘야 해.
                영화관은 인천, 성남, 노량진, 강남, 부산에 있어.
                URL 링크는 실제 ResponseType.LINK 타입을 기반에서 제공하니 키워드만 발췌해서 
                꼭 존댓말로 안내만 해줘 안된다는 말 금지.
                """;

        String fullPrompt = systemPrompt + "\n\n" + message;


        String gptAnswer = ask(fullPrompt);


        /* 시간표 관련 Question */
        if (lower.contains("시간표")) {
            for (String keyword : cinemas) {
                if (lower.contains(keyword)) {
                    String cinemaName = "MovieFlex " + keyword;
                    String encodedName = URLEncoder.encode(cinemaName, StandardCharsets.UTF_8)
                            .replaceAll("\\+", "%20");

                    return ChatbotResponseDto.builder()
                            .response(gptAnswer)
                            .buttonText("🗺️ 상영시간표 보러가기")
                            .buttonUrl("/schedule/" + encodedName)
                            .type(ResponseType.LINK)
                            .build();
                }
            }

            return ChatbotResponseDto.builder()
                    .response(gptAnswer)
                    .buttonText("🗺️ 상영시간표 보러가기")
                    .buttonUrl("/cinema/map")
                    .type(ResponseType.LINK)
                    .build();
        }

        //* 영화관 관련 Question *//*

        if (lower.contains("영화관")) {
            return ChatbotResponseDto.builder()
                    .response(gptAnswer)
                    .buttonText("영화관 찾기")
                    .buttonUrl("/cinema/map")
                    .type(ResponseType.LINK)
                    .build();
        }

        /* 회원가입 관련 Question */
        if (lower.contains("회원가입")) {
            return ChatbotResponseDto.builder()
                    .response(gptAnswer)
                    .buttonText("회원가입 하러가기")
                    .buttonUrl("/members/auth")
                    .type(ResponseType.LINK)
                    .build();
        }

        /* 로그인 관련 Question */
        if (lower.contains("로그인")) {
            return ChatbotResponseDto.builder()
                    .response(gptAnswer)
                    .buttonText("로그인 하러가기")
                    .buttonUrl("/members/auth")
                    .type(ResponseType.LINK)
                    .build();
        }

        /* 스토어 관련 Question */
        if (lower.contains("팝콘") || lower.contains("스토어")) {
            return ChatbotResponseDto.builder()
                    .response(gptAnswer)
                    .buttonText("스토어 가기")
                    .buttonUrl("/store")
                    .type(ResponseType.LINK)
                    .build();
        }

        /* 영화 관련 Question */
        if (lower.contains("영화")) {
            return ChatbotResponseDto.builder()
                    .response(gptAnswer)
                    .buttonText("영화 찾기")
                    .buttonUrl("/movie")
                    .type(ResponseType.LINK)
                    .build();
        }

        /* 마이페이지 관련 Question */
        if (lower.contains("마이페이지") || lower.contains("회원정보")) {
            return ChatbotResponseDto.builder()
                    .response(gptAnswer)
                    .buttonText("마이페이지")
                    .buttonUrl("/mypage")
                    .type(ResponseType.LINK)
                    .build();
        }

        /* 예매 정보 관련 마이페이지 반환 */
        if (lower.contains("예매 내역")) {
            return ChatbotResponseDto.builder()
                    .response(gptAnswer)
                    .buttonText("마이페이지")
                    .buttonUrl("/mypage")
                    .type(ResponseType.LINK)
                    .build();
        }

        if (lower.contains("추천")) {
            return ChatbotResponseDto.builder()
                    .response(gptAnswer)
                    .buttonText("AI 추천")
                    .buttonUrl("/recommend")
                    .type(ResponseType.LINK)
                    .build();
        }


        return ChatbotResponseDto.builder()
                .response(gptAnswer)
                .type(ResponseType.TEXT)
                .build();

    }


    @Value("${chatgpt.api-key}")
    private String apiKey;

    @Value("${chatgpt.gpt-model}")
    private String model;

    @Value("${openai.url.prompt}")
    private String url;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String ask(String prompt) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.7,
                    "max_tokens", 500
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode jsonNode = objectMapper.readTree(response.body());
            JsonNode choicesNode = jsonNode.get("choices");

            if (choicesNode != null && choicesNode.isArray() && choicesNode.size() > 0) {
                return choicesNode.get(0).get("message").get("content").asText().trim();
            } else {
                System.err.println("❌ GPT 응답이 비어있습니다: " + response.body());
                return "GPT 응답 중 오류가 발생했어요.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "GPT 응답 중 오류가 발생했어요.";
        }
    }

}