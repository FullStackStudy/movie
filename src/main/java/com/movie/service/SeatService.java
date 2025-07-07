package com.movie.service;

import com.movie.dto.SeatDto;
import com.movie.dto.reservation.ReservationResponseDto;
import com.movie.entity.Schedule;
import com.movie.entity.ScreenRoom;
import com.movie.entity.Seat;
import com.movie.repository.ScreenRoomRepository;
import com.movie.repository.SeatRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final ScreenRoomRepository screenRoomRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public List<SeatDto> getSeatsByRoomId(Long roomId) {
        Optional<ScreenRoom> room = screenRoomRepository.findById(roomId);

        if (room.isEmpty()) {
            throw new IllegalArgumentException("해당 상영관이 존재하지 않습니다: " + roomId);
        }

        return room.get().getSeats().stream().map(seat -> {
            SeatDto dto = new SeatDto();
            dto.setSeatRow(seat.getSeatRow());
            dto.setSeatColumn(seat.getSeatColumn());
            dto.setPrice(seat.getPrice());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void saveSeat(SeatDto seatDto) {
        Seat seat = new Seat();
        seat.setSeatRow(seatDto.getSeatRow());
        seat.setSeatColumn(seatDto.getSeatColumn());
        seat.setPrice(seatDto.getPrice());

        // 상영관 정보 연결 (필수)
        ScreenRoom room = screenRoomRepository.findById(seatDto.getScreenRoomId())
                .orElseThrow(() -> new IllegalArgumentException("상영관이 존재하지 않음"));

        seat.setScreenRoom(room);

        seatRepository.save(seat);
    }

    //예약하려는 좌석 A1, B1 .. -> id로 바꿔줌
    public Long getSeatId(String row, int col, Long roomId){
        return seatRepository.findSeatIdByColRowRoomId(row, col, roomId);
    }



}
