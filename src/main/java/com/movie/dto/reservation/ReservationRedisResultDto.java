package com.movie.dto.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Data
@AllArgsConstructor
public class ReservationRedisResultDto {
    List<Long> successList;
    List<Long> failList;
}
