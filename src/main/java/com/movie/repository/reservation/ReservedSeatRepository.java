package com.movie.repository.reservation;

import com.movie.entity.reservation.ReservedSeat;
import com.movie.entity.cinema.ScreenRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservedSeatRepository extends JpaRepository<ReservedSeat, Long> {
    List<ReservedSeat> findBySchedule_Id(Long schedule_id); //스케쥴마다 예약된 좌석 가져오기
    int countBySchedule_Id(Long scheduleId);

    void deleteAllByReservation_id(Long reservationId); // 예약에 관련된 저장된 좌석 지우기
    List<ReservedSeat> findBySeat_ScreenRoom(ScreenRoom screenRoom);
} 