package com.movie.dto.reservation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.movie.dto.SeatDto;
import com.movie.entity.Schedule;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;


@Getter
@Setter
@ToString
public class ReservationResponseDto extends ReservationDto {

    private String movieName;

    private String cinemaName;

    private String screenName;

    private Long per;

    private List<String> seatName;

    private LocalDate showDate; //이건 스케쥴의 영화상영시간

    private LocalTime startTime; //스케줄의 영화상영 날

    private Boolean cancellable; //취소가능한지

    private LocalDateTime movieDateTime;

}