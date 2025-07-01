package com.movie.repository;

import com.movie.dto.SeatDto;
import com.movie.entity.ScreenRoom;
import com.movie.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByScreenRoom(ScreenRoom screenRoom);
}
