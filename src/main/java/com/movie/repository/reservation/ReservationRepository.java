package com.movie.repository.reservation;

import com.movie.entity.reservation.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findAllByMember_memberIdOrderByReservedAtDesc(String memberId); //내가 본 영화 목록
    int countBySchedule_Id(Long scheduleId);

    Optional<Reservation> findTopByMember_MemberIdAndReservedAtBetweenOrderByReservedAtDesc(
            String memberId, LocalDateTime start, LocalDateTime end
    );


}
