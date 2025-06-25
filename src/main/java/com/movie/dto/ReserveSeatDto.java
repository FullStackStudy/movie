package com.movie.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ReserveSeatDto {
        //private Long scheduleId; // 상영 시간표 ID
        private List<Long> seatIds; // 예약할 좌석 ID 목록
        private String memberId; // 예약한 사용자
        private LocalDateTime reservedAt; // 예약 시간
}
