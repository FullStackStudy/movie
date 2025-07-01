package com.movie.repository;

import com.movie.entity.ReservedSeat;
import com.movie.entity.ScreenRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservedSeatRepository extends JpaRepository<ReservedSeat, Long> {
    List<ReservedSeat> findBySeat_ScreenRoom(ScreenRoom screenRoom);
}
