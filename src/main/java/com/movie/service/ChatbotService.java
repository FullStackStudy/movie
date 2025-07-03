package com.movie.service;

import com.movie.constant.ResponseType;
import com.movie.dto.ChatbotResponseDto;
import com.movie.entity.ChatbotResponse;
import com.movie.repository.ChatbotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatbotRepository chatbotRepository;

    public ChatbotResponseDto generateResponse(String message) {
        String lower = message.toLowerCase();
        List<String> cinemas = List.of("인천", "성남", "노량진", "부산");

        /* 시간표 관련 Question */
        if (lower.contains("시간표")) {
            for (String keyword : cinemas) {
                if (lower.contains(keyword)) {
                    String cinemaName = "MovieFlex " + keyword;
                    String encodedName = URLEncoder.encode(cinemaName, StandardCharsets.UTF_8)
                            .replaceAll("\\+", "%20");

                    return ChatbotResponseDto.builder()
                            .response("하단의 버튼을 누르면 MovieFlex 인천점의 상영시간표로 이동합니다")
                            .buttonText("🗺️ 상영시간표 보러가기")
                            .buttonUrl("/schedule/" + encodedName)
                            .type(ResponseType.LINK)
                            .build();
                }
            }

            return ChatbotResponseDto.builder()
                    .response("시간표는 영화관 찾기에서 영화관 별 상영시간표를 찾을 수 있습니다.")
                    .buttonText("🗺️ 상영시간표 보러가기")
                    .buttonUrl("/cinema/map")
                    .type(ResponseType.LINK)
                    .build();
        }

        /* 영화관 관련 Question */

        if (lower.contains("영화관")) {
            return ChatbotResponseDto.builder()
                    .response("영화관을 찾고싶으시다면 다음 버튼을 눌러주세요.")
                    .buttonText("영화관 찾기")
                    .buttonUrl("/cinema/map")
                    .type(ResponseType.LINK)
                    .build();
        }

        /* 회원가입 관련 Question */
        if (lower.contains("회원가입")) {
            return ChatbotResponseDto.builder()
                    .response("회원가입을 하시려면 다음 버튼을 눌러주세요.")
                    .buttonText("회원가입 하러가기")
                    .buttonUrl("/members/new")
                    .type(ResponseType.LINK)
                    .build();
        }

        /* 로그인 관련 Question */
        if (lower.contains("로그인")) {
            return ChatbotResponseDto.builder()
                    .response("로그인을 하시면 티켓과 F&B를 구매하실 수 있습니다.")
                    .buttonText("로그인 하러가기")
                    .buttonUrl("/members/login")
                    .type(ResponseType.LINK)
                    .build();
        }

        /* 스토어 관련 Question */
        if (lower.contains("팝콘")) {
            return ChatbotResponseDto.builder()
                    .response("팝콘 등 음식을 구매하고 싶으시다면 아래 버튼을 눌러주세요.")
                    .buttonText("스토어 가기")
                    .buttonUrl("/store")
                    .type(ResponseType.LINK)
                    .build();
        }

        /* 영화 관련 Question */
        if (lower.contains("영화")) {
            return ChatbotResponseDto.builder()
                    .response("상영중인 영화를 찾고 싶으시다면 아래 버튼을 눌러주세요.")
                    .buttonText("영화 찾기")
                    .buttonUrl("/movie")
                    .type(ResponseType.LINK)
                    .build();
        }


        /* 키워드에 해당하는 Question이 없는 경우 */
        return ChatbotResponseDto.builder()
                .response("죄송합니다. 더 구체적으로 말씀해 주시면 도움을 드릴 수 있습니다.")
                .type(ResponseType.TEXT)
                .build();
    }
}