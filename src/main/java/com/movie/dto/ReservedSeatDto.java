package com.movie.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReservedSeatDto {
    private Long reservedSeat_id;
    private Long reserved_id;
    private String member_id;
    private Long seat_id;
    private Long movie_id; // 지금은 이거 나중에 sceduleid로 바꿈

}
