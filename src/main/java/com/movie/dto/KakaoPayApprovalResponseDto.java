package com.movie.dto;

import lombok.Data;

@Data
public class KakaoPayApprovalResponseDto {
    private String aid;
    private String tid;
    private String cid;
    private String partner_order_id;
    private String partner_user_id;
    private Amount amount;
    private String payment_method_type;
    private String item_name;
    private String created_at;
    private String approved_at;
    
    @Data
    public static class Amount {
        private int total;
        private int tax_free;
        private int vat;
        private int point;
        private int discount;
    }
} 