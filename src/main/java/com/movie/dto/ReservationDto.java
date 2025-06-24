package com.movie.dto;

import com.movie.constant.ReservationStatus;
import com.movie.entity.Reservation;
import jakarta.persistence.*;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ReservationDto {
    private Long id;
    private String user_id;
    private Long movie_id;
    private List<String> seat_id;
    private LocalDateTime reserved_at;
    private ReservationStatus reservationStatus;

    //-----------------modelMapper--------------------//
    private static ModelMapper modelMapper = new ModelMapper();

    public Reservation createReservation(){
        return modelMapper.map(this, Reservation.class);
    }// dto -> entity

    public static ReservationDto of(Reservation reservation){
        return modelMapper.map(reservation, ReservationDto.class);
    } //entity -> dto
}
