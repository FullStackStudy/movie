package com.movie.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ScheduleDto {
    private String id;
    private String cinemaNm;
    private String movieTitle;       // 예: "범죄도시4"
    private String screenRoomName;   // 예: "1관"
    private LocalDate showDate;
    private LocalTime startTime;
    private String status;           //상태
    private String description;      //설명
}
