package com.movie.dto.reservation;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class SeatStatusMessageDto {
    private Long scheduleId;
    private List<Long> seatId;
    private String status;
    private Integer ttl; // 1분 남으면 알림 보내기 위해 추가
    private List<String> seatNames;


    public SeatStatusMessageDto(Long scheduleId, List<Long> seatId, String status) {
        this.status = status;
        this.scheduleId = scheduleId;
        this.seatId = seatId;
    }

    public SeatStatusMessageDto(Long scheduleId, List<Long> seatId, String status, Integer ttl) {
        this.status = status;
        this.scheduleId = scheduleId;
        this.seatId = seatId;
        this.ttl = ttl;
    }

    // seatName까지 받는 생성자 추가
    public SeatStatusMessageDto(Long scheduleId, List<Long> seatId, List<String> seatNames, String status) {
        this.scheduleId = scheduleId;
        this.seatId = seatId;
        this.seatNames = seatNames;
        this.status = status;
    }
}//websocket 실시간 메세지를 위한
