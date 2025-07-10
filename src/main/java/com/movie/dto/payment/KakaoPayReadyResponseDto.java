package com.movie.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class KakaoPayReadyResponseDto {
    @JsonProperty("tid")
    private String tid;
    
    @JsonProperty("next_redirect_pc_url")
    private String next_redirect_pc_url;
    
    @JsonProperty("next_redirect_mobile_url")
    private String next_redirect_mobile_url;
    
    @JsonProperty("next_redirect_app_url")
    private String next_redirect_app_url;
    
    @JsonProperty("android_app_scheme")
    private String android_app_scheme;
    
    @JsonProperty("ios_app_scheme")
    private String ios_app_scheme;
    
    @JsonProperty("created_at")
    private String created_at;
} 