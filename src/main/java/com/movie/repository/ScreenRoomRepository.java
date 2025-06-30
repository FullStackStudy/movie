package com.movie.repository;

import com.movie.entity.ScreenRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScreenRoomRepository extends JpaRepository<ScreenRoom, Long> {
    Optional<ScreenRoom> findByRoomNmAndCinema_Name(String roomNm, String cinemaName);

    //
    //Optional<ScreenRoom> findByAvailableSeats(int availableSeats);
}