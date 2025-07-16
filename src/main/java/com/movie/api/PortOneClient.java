package com.movie.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.dto.store.PortOnePaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortOneClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    // 실제 가맹점 발급 정보
    private final String impKey = "7251445755405710";
    private final String impSecret = "uZbHJBrDF307TXxASqqljRkXokcOlmelhlbTUGlx1zQ87haLvH5i1YhOxHmQM2wGVGuNbdQ2rJdgREyx";

    public PortOnePaymentResponse verifyPayment(String impUid) {
        try {
            String accessToken = getAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.iamport.kr/payments/" + impUid))
                    .header("Authorization", accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode node = objectMapper.readTree(response.body());
            JsonNode responseNode = node.get("response");

            return PortOnePaymentResponse.builder()
                    .impUid(responseNode.get("imp_uid").asText())
                    .merchantUid(responseNode.get("merchant_uid").asText())
                    .amount(responseNode.get("amount").asInt())
                    .status(responseNode.get("status").asText())
                    .success("paid".equals(responseNode.get("status").asText()))
                    .build();

        } catch (Exception e) {
            log.error("포트원 결제 검증 실패", e);
            return null;
        }
    }

    private String getAccessToken() throws IOException, InterruptedException {
        Map<String, String> params = new HashMap<>();
        params.put("imp_key", impKey);
        params.put("imp_secret", impSecret);

        String body = objectMapper.writeValueAsString(params);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.iamport.kr/users/getToken"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode node = objectMapper.readTree(response.body());
        return node.get("response").get("access_token").asText();
    }
}
