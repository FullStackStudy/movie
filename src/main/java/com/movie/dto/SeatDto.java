package com.movie.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatDto {
    private Long id;
    private String seatRow;     // A~J
    private int seatColumn;     // 1~9
    private int price;
    private boolean available;  // 예약 가능 여부
    private Long screenRoomId;  // ← 이게 있어야 위 코드가 가능함
}
