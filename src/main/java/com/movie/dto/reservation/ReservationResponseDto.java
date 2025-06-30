package com.movie.dto.reservation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@ToString
public class ReservationResponseDto extends ReservationDto{

    private String movieName;


    private String cinemaName;

    private String screenName;

    private Long per;

    private List<String> seatName;

    private LocalTime startTime;
}
