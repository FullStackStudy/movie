package com.movie.dto;

import com.movie.entity.Seat;
import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class SeatDto {
    private Long id;
    private String roomNm;
    private boolean availableSeats;
    private Long roomId;
    private Integer price;
    private String seatRow;
    private int seatColumn;
    private List<ScreenRoomDto> screenRooms;  // 상영관 정보 리스트


    private boolean available;  // 예약 가능 여부
    private Long screenRoomId;  // ← 이게 있어야 위 코드가 가능함

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
