package com.movie.service.recommend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class RecommendScheduler {
    private final RecommendService recommendService;

    //스케줄러로 하루 한번씩 11시에 lightfm으로 영화 정보 보내는 함수
    @Scheduled(cron = "0 00 10 * * *", zone = "Asia/Seoul")
    public void sendMovieInfo() throws JsonProcessingException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("interactions", recommendService.getInteraction());
        requestBody.put("movies", recommendService.getMovieInfo());
        RestTemplate restTemplate = new RestTemplate();
        String pythonApiUrl = "http://127.0.0.1:5000/update-vectors"; //파이선 주소

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);


        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(requestBody); // Map -> JSON 문자열 변환

        HttpEntity<String> requestEntity = new HttpEntity<>(json, headers);
        System.out.println("요청 JSON = " + requestEntity);
        ResponseEntity<String> response =restTemplate.postForEntity(pythonApiUrl, requestEntity, String.class);

        System.out.println("flack 응답: " +response.getBody());
    }
}
