package com.movie.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CinemaDto {
    private Long id; //지점 ID
    private String name; //지점 이름
    private double lat; //지점 위도
    private double lng; //지점 경도
    private String address; //지점 주소
    private int screens; //보유한 상영관 개수
    private String startTime; //영업 시작시간
    private String endTime; //영업 종료시간

    private List<ScreenRoomDto> screenRooms;  // 상영관 정보 리스트
}
