package com.movie.dto.payment;

import lombok.Data;

@Data
public class KakaoPayApprovalRequestDto {
    private String tid;
    private String partner_order_id;
    private String partner_user_id;
    private String pg_token;
} 