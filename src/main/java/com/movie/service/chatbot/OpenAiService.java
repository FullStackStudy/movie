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
import java.util.*;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    @Value("${chatgpt.api-key}")
    private String apiKey;

    @Value("${chatgpt.gpt-model}")
    private String model;

    @Value("${openai.url.prompt}")
    private String url;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final List<String> CINEMA_LOCATIONS = List.of("인천", "성남", "노량진", "부산", "강남");

    private static final String SYSTEM_PROMPT = """
            너는 MovieFlex 영화관 웹사이트의 친절한 챗봇이야.
            사용자에게 영화 정보, 시간표, 회원가입, 로그인, 스토어 관련 내용을 도와줘야 해.
            영화관은 인천, 성남, 노량진, 강남, 부산에 있어.
            URL 링크는 실제 ResponseType.LINK 타입을 기반에서 제공하니 키워드만 발췌해서 
            꼭 존댓말로 안내만 해줘 안된다는 말 금지.
            """;

    private static final Map<String, ChatbotLinkInfo> KEYWORD_RESPONSE_MAP = Map.ofEntries(
            entry("회원가입", new ChatbotLinkInfo("회원가입 하러가기", "/members/auth")),
            entry("로그인", new ChatbotLinkInfo("로그인 하러가기", "/members/auth")),
            entry("영화관", new ChatbotLinkInfo("영화관 찾기", "/cinema/map")),
            entry("팝콘", new ChatbotLinkInfo("스토어 가기", "/store")),
            entry("스토어", new ChatbotLinkInfo("스토어 가기", "/store")),
            entry("영화", new ChatbotLinkInfo("영화 찾기", "/movie")),
            entry("마이페이지", new ChatbotLinkInfo("마이페이지", "/mypage")),
            entry("회원정보", new ChatbotLinkInfo("마이페이지", "/mypage")),
            entry("예매 내역", new ChatbotLinkInfo("마이페이지", "/mypage")),
            entry("추천", new ChatbotLinkInfo("AI 추천", "/recommend"))
    );

    public ChatbotResponseDto generateResponse(String message) {
        String lower = message.toLowerCase();
        String prompt = SYSTEM_PROMPT + "\n\n" + message;
        String gptAnswer = ask(prompt);

        if (lower.contains("시간표")) {
            return buildScheduleResponse(lower, gptAnswer);
        }

        for (Map.Entry<String, ChatbotLinkInfo> entry : KEYWORD_RESPONSE_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return createLinkResponse(gptAnswer, entry.getValue());
            }
        }

        return ChatbotResponseDto.builder()
                .response(gptAnswer)
                .type(ResponseType.TEXT)
                .build();
    }

    private ChatbotResponseDto buildScheduleResponse(String lower, String gptAnswer) {
        for (String location : CINEMA_LOCATIONS) {
            if (lower.contains(location)) {
                String encoded = getCinemaUrl(location);
                return createLinkResponse(gptAnswer,
                        new ChatbotLinkInfo("🗺️ 상영시간표 보러가기", "/schedule/" + encoded));
            }
        }
        return createLinkResponse(gptAnswer,
                new ChatbotLinkInfo("🗺️ 상영시간표 보러가기", "/cinema/map"));
    }

    private String getCinemaUrl(String name) {
        String full = "MovieFlex " + name;
        return URLEncoder.encode(full, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private ChatbotResponseDto createLinkResponse(String response, ChatbotLinkInfo info) {
        return ChatbotResponseDto.builder()
                .response(response)
                .buttonText(info.buttonText())
                .buttonUrl(info.buttonUrl())
                .type(ResponseType.LINK)
                .build();
    }

    public String ask(String prompt) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
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
            JsonNode choices = jsonNode.get("choices");

            if (choices != null && choices.isArray() && choices.size() > 0) {
                return choices.get(0).get("message").get("content").asText().trim();
            }

            System.err.println("❌ GPT 응답이 비어있습니다: " + response.body());
            return "GPT 응답 중 오류가 발생했어요.";

        } catch (Exception e) {
            e.printStackTrace();
            return "GPT 응답 중 오류가 발생했어요.";
        }
    }

    private static Map.Entry<String, ChatbotLinkInfo> entry(String keyword, ChatbotLinkInfo info) {
        return Map.entry(keyword, info);
    }

    private record ChatbotLinkInfo(String buttonText, String buttonUrl) {}
}
