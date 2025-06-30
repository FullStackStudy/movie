package com.movie.repository;

import com.movie.entity.ScreenRoom;
import com.movie.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    //관마다 좌석찾기위한 함수
    List<Seat> findByScreenRoom_Id(Long roomId);



}
