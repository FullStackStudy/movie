package com.movie.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SeatDto {
    private Long id;
    private String roomNm;
    private boolean availableSeats;
    private String seatRow; //좌석 행 (A~J까지 10개 행)
    private int seatColumn; //좌석 열 (0~9까지 10개 열))
    private int price; //좌석 가격
    private List<ScreenRoomDto> screenRooms;  // 상영관 정보 리스트
}
