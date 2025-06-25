package com.movie.repository;

import com.movie.entity.ScreenRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScreenRoomRepository extends JpaRepository<ScreenRoom, String> {
    Optional<ScreenRoom> findByRoomNm(String name);
    //
    //Optional<ScreenRoom> findByAvailableSeats(int availableSeats);
}
