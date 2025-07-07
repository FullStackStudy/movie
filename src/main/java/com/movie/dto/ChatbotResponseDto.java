package com.movie.dto;

import com.movie.constant.ResponseType;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Data
public class ChatbotResponseDto {
    //private Long id; 나중에 추가 예정
    private String response; //텍스트 기반 응답
    private String buttonText; //버튼 표시 텍스트
    private String buttonUrl; //버튼 클릭시 이동할 Url
    private ResponseType type;
}