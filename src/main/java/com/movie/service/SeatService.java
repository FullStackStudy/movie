package com.movie.service;

import com.movie.dto.SeatDto;
import com.movie.entity.ScreenRoom;
import com.movie.repository.ScreenRoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final ScreenRoomRepository screenRoomRepository;
    //private final ReservationRepository reservationRepository;

    @Transactional
    public List<SeatDto> getAvailableSeats() {
        List<ScreenRoom> screenRooms = screenRoomRepository.findAll();

        return screenRooms.stream().map(room -> {
            //int reserved = reservationRepository.countByScreenRoom(room); // 예약된 좌석 수
            //int available = room.getTotalSeats() - reserved;

            SeatDto dto = new SeatDto();
            dto.setRoomNm(room.getRoomNm());
            //dto.setAvailableSeats(available);
            return dto;
        }).collect(Collectors.toList());
    }
}
