package com.movie.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScreenRoomDto {
    private String roomNm; //상영관 이름
    private int totalSeats; //상영관 전체 좌석 수
    private int availableSeats; //잔여석 -> 총 좌석 수에서 얘매불가를 뺄 예정
}