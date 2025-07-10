package com.movie.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.dto.payment.KakaoPayApprovalRequestDto;
import com.movie.dto.payment.KakaoPayApprovalResponseDto;
import com.movie.dto.payment.KakaoPayReadyRequestDto;
import com.movie.dto.payment.KakaoPayReadyResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoPayService {

    private final RestTemplate paymentRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kakao.pay.secret-key}")
    private String secretKey;

    @Value("${kakao.pay.cid}")
    private String cid;

    public KakaoPayReadyResponseDto kakaoPayReady(KakaoPayReadyRequestDto requestDto) {
        // 카카오페이 요청 양식 (Map 사용 - MultiValueMap 대신)
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("cid", cid);
        parameters.put("partner_order_id", requestDto.getPartner_order_id());
        parameters.put("partner_user_id", requestDto.getPartner_user_id());
        parameters.put("item_name", requestDto.getItem_name());
        parameters.put("quantity", "1");
        parameters.put("total_amount", String.valueOf(requestDto.getTotal_amount()));
        parameters.put("vat_amount", String.valueOf((int)(requestDto.getTotal_amount() * 0.1))); // 부가세 10%
        parameters.put("tax_free_amount", "0");
        parameters.put("approval_url", requestDto.getApproval_url());
        parameters.put("cancel_url", requestDto.getCancel_url());
        parameters.put("fail_url", requestDto.getFail_url());

        // 파라미터, 헤더
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(parameters, this.getHeaders());

        try {
            KakaoPayReadyResponseDto responseDto = paymentRestTemplate.postForObject(
                    "https://open-api.kakaopay.com/online/v1/payment/ready",
                    requestEntity, KakaoPayReadyResponseDto.class);
            log.info("카카오페이 준비 응답: {}", responseDto);
            return responseDto;
        } catch (Exception e) {
            log.error("카카오페이 준비 요청 실패", e);
            throw new RuntimeException("카카오페이 준비 요청에 실패했습니다.", e);
        }
    }

    public KakaoPayApprovalResponseDto kakaoPayApprove(KakaoPayApprovalRequestDto requestDto) {
        // 카카오 요청 양식
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("cid", cid);
        parameters.put("tid", requestDto.getTid());
        parameters.put("partner_order_id", requestDto.getPartner_order_id());
        parameters.put("partner_user_id", requestDto.getPartner_user_id());
        parameters.put("pg_token", requestDto.getPg_token());

        // 파라미터, 헤더
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(parameters, this.getHeaders());

        try {
            KakaoPayApprovalResponseDto responseDto = paymentRestTemplate.postForObject(
                    "https://open-api.kakaopay.com/online/v1/payment/approve",
                    requestEntity, KakaoPayApprovalResponseDto.class);
            log.info("카카오페이 승인 응답: {}", responseDto);
            return responseDto;
        } catch (Exception e) {
            log.error("카카오페이 승인 요청 실패", e);
            throw new RuntimeException("카카오페이 승인 요청에 실패했습니다.", e);
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        String auth = "SECRET_KEY " + secretKey;
        httpHeaders.set("Authorization", auth);
        httpHeaders.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        return httpHeaders;
    }
} 