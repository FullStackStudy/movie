package com.movie.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KakaoPayApprovalResponseDto {
    @JsonProperty("aid")
    private String aid;
    
    @JsonProperty("tid")
    private String tid;
    
    @JsonProperty("cid")
    private String cid;
    
    @JsonProperty("partner_order_id")
    private String partner_order_id;
    
    @JsonProperty("partner_user_id")
    private String partner_user_id;
    
    @JsonProperty("amount")
    private Amount amount;
    
    @JsonProperty("payment_method_type")
    private String payment_method_type;
    
    @JsonProperty("item_name")
    private String item_name;
    
    @JsonProperty("created_at")
    private String created_at;
    
    @JsonProperty("approved_at")
    private String approved_at;
    
    @Data
    public static class Amount {
        @JsonProperty("total")
        private int total;
        
        @JsonProperty("tax_free")
        private int tax_free;
        
        @JsonProperty("vat")
        private int vat;
        
        @JsonProperty("point")
        private int point;
        
        @JsonProperty("discount")
        private int discount;
    }
} 