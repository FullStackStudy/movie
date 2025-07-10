package com.movie.dto.cinema;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreenRoomDto {
    private String roomNm; //상영관 이름
    private int totalSeats; //상영관 전체 좌석 수
} 