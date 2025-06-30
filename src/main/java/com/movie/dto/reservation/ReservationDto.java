package com.movie.dto.reservation;

import com.movie.constant.ReservationStatus;
import com.movie.entity.Member;
import com.movie.entity.Schedule;
import com.movie.entity.Seat;
import com.movie.entity.reservation.Reservation;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@ToString
public class ReservationDto {
    private String memberId;

    private Long scheduleId;

    private List<Long> seatId;
    private Long per;

    private LocalDateTime reservedAt;
    private ReservationStatus reservationStatus;

    private Integer price;
    private String payMethod;

    //-----------------modelMapper--------------------//
    private static ModelMapper modelMapper = new ModelMapper();

    public Reservation createReservation() {
        return modelMapper.map(this, Reservation.class);
    }// dto -> entity

    public static ReservationDto of(Reservation reservation) {
        return modelMapper.map(reservation, ReservationDto.class);
    } //entity -> dto

    public Reservation createReservation(Schedule schedule, Member member, ReservationDto reservationDto){
        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setSchedule(schedule);
        reservation.setSeat_id(reservationDto.getSeatId().toString());
        reservation.setReservationStatus(ReservationStatus.RESERVED);
        reservation.setPay_method(reservationDto.getPayMethod());
        reservation.setPer(reservationDto.getPer());
        reservation.setPrice(reservationDto.getPrice());
        return reservation;
    }
}


