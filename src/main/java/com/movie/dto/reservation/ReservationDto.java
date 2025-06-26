package com.movie.dto.reservation;

import com.movie.constant.ReservationStatus;
import com.movie.entity.reservation.Reservation;
import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ReservationDto {
    private String member_id;
    private Long movie_id;
    private List<String> seat_id;
    private LocalDateTime reserved_at;
    private ReservationStatus reservationStatus;

    private Integer price;
    private String pay_method;

    //-----------------modelMapper--------------------//
    private static ModelMapper modelMapper = new ModelMapper();

    public Reservation createReservation(){
        return modelMapper.map(this, Reservation.class);
    }// dto -> entity

    public static ReservationDto of(Reservation reservation){
        return modelMapper.map(reservation, ReservationDto.class);
    } //entity -> dto
}
