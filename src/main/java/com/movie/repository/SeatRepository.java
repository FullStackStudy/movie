package com.movie.repository;

import com.movie.dto.SeatDto;
import com.movie.entity.ScreenRoom;
import com.movie.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    //관마다 좌석찾기위한 함수
    List<Seat> findByScreenRoom_Id(Long roomId);

    List<Seat> findByScreenRoom(ScreenRoom screenRoom);

    //col row roomId 합쳐서 seatId 찾기 이다은
    @Query("SELECT s.id FROM Seat s WHERE s.seatColumn = :col AND s.seatRow = :row ANd s.screenRoom.id = :roomId ")
    Long findSeatIdByColRowRoomId(@Param("row") String row, @Param("col") int col, @Param("roomId") Long roomId);
}
