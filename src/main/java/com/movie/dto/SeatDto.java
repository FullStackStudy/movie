package com.movie.dto;

import com.movie.dto.reservation.ReservationDto;
import com.movie.entity.Seat;
import com.movie.entity.reservation.Reservation;
import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class SeatDto {
    private Long id;
    private Long room_id;
    private Integer price;
    private char seatRow;
    private int seatCol;

    //A1, B2...
    private String seatName= String.valueOf(seatRow) + seatCol;

    //-----------------modelMapper--------------------//
    private static ModelMapper modelMapper = new ModelMapper();

    public Seat createReservation(){
        return modelMapper.map(this, Seat.class);
    }// dto -> entity

    public static SeatDto of(Seat seat){
        return modelMapper.map(seat, SeatDto.class);
    } //entity -> dto

    public static List<SeatDto> ofList(List<Seat> seats) {
        return seats.stream()
                .map(SeatDto::of)  // 단일 변환 메서드 재사용
                .collect(Collectors.toList());
    }//list

}
