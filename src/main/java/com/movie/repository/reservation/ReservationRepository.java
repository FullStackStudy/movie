package com.movie.repository.reservation;

import com.movie.entity.reservation.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findAllByMember_memberIdOrderByReservedAtDesc(String memberId); //내가 본 영화 목록
}
