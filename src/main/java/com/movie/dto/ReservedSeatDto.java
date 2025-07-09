package com.movie.dto;

import com.movie.constant.ReservationStatus;
import com.movie.dto.reservation.ReservationDto;
import com.movie.entity.Member;
import com.movie.entity.ReservedSeat;
import com.movie.entity.Schedule;
import com.movie.entity.Seat;
import com.movie.entity.reservation.Reservation;
import lombok.Getter;
import lombok.Setter;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
public class ReservedSeatDto {
    private Long reservedSeatId;
    private Long reservedId;
    private String memberId;
    private Long seatId;
    private Long scheduleId; // 지금은 이거 나중에 sceduleid로 바꿈

    public Reservation createReservation(Schedule schedule, Member member, ReservationDto reservationDto){
        Reservation reservation = new Reservation();
        reservation.setMember(member);
        reservation.setSchedule(schedule);
        reservation.setSeat_id(reservationDto.getSeatId().toString());
        reservation.setReservationStatus(ReservationStatus.RESERVED);
        reservation.setPayMethod(reservationDto.getPayMethod());
        reservation.setPer(reservationDto.getPer());
        reservation.setPrice(reservationDto.getPrice());
        return reservation;
    }
    //-----------------modelMapper--------------------//
    private static ModelMapper modelMapper = new ModelMapper();

    public ReservedSeat createReservation() {
        return modelMapper.map(this, ReservedSeat.class);
    }// dto -> entity

    public static ReservedSeatDto of(ReservedSeat reservedSeat) {
        return modelMapper.map(reservedSeat, ReservedSeatDto.class);
    } //entity -> dto

    public static List<ReservedSeatDto> ofList(List<ReservedSeat> seats) {
        return seats.stream()
                .map(ReservedSeatDto::of)  // 단일 변환 메서드 재사용
                .collect(Collectors.toList());
    }//list

}
