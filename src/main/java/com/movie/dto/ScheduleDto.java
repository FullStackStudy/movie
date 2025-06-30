package com.movie.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class ScheduleDto {
    private Long scheduleId; //예약에 scheduleId 필요해서 추가
    private String cinemaName;
    private String movieTitle;       // 예: "범죄도시4"
    private String screenRoomName;   // 예: "1관"
    private LocalDate showDate;
    private LocalTime startTime;
    private String status;           //상태
    private String description;      //설명
}

