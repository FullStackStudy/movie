package com.movie.dto;

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
    List<String> successList;
    List<String> failList;
}
