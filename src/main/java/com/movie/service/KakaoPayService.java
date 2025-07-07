package com.movie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.dto.KakaoPayApprovalRequestDto;
import com.movie.dto.KakaoPayApprovalResponseDto;
import com.movie.dto.KakaoPayReadyRequestDto;
import com.movie.dto.KakaoPayReadyResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoPayService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kakao.pay.admin-key}")
    private String adminKey;

    @Value("${kakao.pay.ready-url}")
    private String readyUrl;

    @Value("${kakao.pay.approval-url}")
    private String approvalUrl;

    public KakaoPayReadyResponseDto kakaoPayReady(KakaoPayReadyRequestDto requestDto) {
        // 카카오페이 요청 양식
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("cid", "TC0ONETIME");
        parameters.add("partner_order_id", requestDto.getPartner_order_id());
        parameters.add("partner_user_id", requestDto.getPartner_user_id());
        parameters.add("item_name", requestDto.getItem_name());
        parameters.add("quantity", String.valueOf(requestDto.getQuantity()));
        parameters.add("total_amount", String.valueOf(requestDto.getTotal_amount()));
        parameters.add("tax_free_amount", String.valueOf(requestDto.getTax_free_amount()));
        parameters.add("approval_url", requestDto.getApproval_url());
        parameters.add("cancel_url", requestDto.getCancel_url());
        parameters.add("fail_url", requestDto.getFail_url());

        // 파라미터, 헤더
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, this.getHeaders());

        try {
            KakaoPayReadyResponseDto responseDto = restTemplate.postForObject(
                    readyUrl, requestEntity, KakaoPayReadyResponseDto.class);
            log.info("카카오페이 준비 응답: {}", responseDto);
            return responseDto;
        } catch (Exception e) {
            log.error("카카오페이 준비 요청 실패", e);
            throw new RuntimeException("카카오페이 준비 요청에 실패했습니다.", e);
        }
    }

    public KakaoPayApprovalResponseDto kakaoPayApprove(KakaoPayApprovalRequestDto requestDto) {
        // 카카오 요청 양식
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("cid", "TC0ONETIME");
        parameters.add("tid", requestDto.getTid());
        parameters.add("partner_order_id", requestDto.getPartner_order_id());
        parameters.add("partner_user_id", requestDto.getPartner_user_id());
        parameters.add("pg_token", requestDto.getPg_token());

        // 파라미터, 헤더
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, this.getHeaders());

        try {
            KakaoPayApprovalResponseDto responseDto = restTemplate.postForObject(
                    approvalUrl, requestEntity, KakaoPayApprovalResponseDto.class);
            log.info("카카오페이 승인 응답: {}", responseDto);
            return responseDto;
        } catch (Exception e) {
            log.error("카카오페이 승인 요청 실패", e);
            throw new RuntimeException("카카오페이 승인 요청에 실패했습니다.", e);
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Authorization", "KakaoAK " + adminKey);
        httpHeaders.set("Content-Type", MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        return httpHeaders;
    }
} 