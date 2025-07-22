package com.movie.dto.payment;

import lombok.Data;

@Data
public class KakaoPayCancelRequestDto {
    private String tid;           // 결제 고유번호
    private Integer cancel_amount; // 취소 금액
    private Integer cancel_tax_free_amount; // 취소 비과세 금액
    private String cancel_reason; // 취소 사유
} 