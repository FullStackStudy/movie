package com.movie.dto;

import com.movie.dto.reservation.ReservationDto;
import com.movie.entity.Schedule;
import com.movie.entity.Seat;
import com.movie.entity.reservation.Reservation;
import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ScheduleDto {
    private Long scheduleId; //예약에 scheduleId 필요해서 추가
    private String cinemaName;
    private String movieTitle;       // 예: "범죄도시4"
    private String screenRoomName;   // 예: "1관"
    private LocalTime startTime;
    private LocalTime endTime;

    private Long roomId;


    //-----------------modelMapper--------------------//
    private static ModelMapper modelMapper = new ModelMapper();

    public Schedule createReservation(){
        return modelMapper.map(this, Schedule.class);
    }// dto -> entity

    public static ScheduleDto of(Schedule schedule){
        return modelMapper.map(schedule, ScheduleDto.class);
    } //entity -> dto

    public static List<ScheduleDto> ofList(List<Schedule> schedules) {
        return schedules.stream()
                .map(ScheduleDto::of)  // 단일 변환 메서드 재사용
                .collect(Collectors.toList());
    }//list entity -> dto
}

