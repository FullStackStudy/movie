package com.movie.service;

import com.movie.dto.CinemaDto;
import com.movie.dto.ScreenRoomDto;
import com.movie.entity.Cinema;
import com.movie.entity.ScreenRoom;
import com.movie.repository.CinemaRepository;
import com.movie.repository.ScreenRoomRepository;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CinemaService {
    private final CinemaRepository cinemaRepository;
    private final ScreenRoomRepository screenRoomRepository;
    //private final SeatRepository seatRepository;
    public void saveCinemas(List<CinemaDto> dtoList) {
        List<Cinema> newCinemas = new ArrayList<>();

        for (CinemaDto dto : dtoList) {
            if (!cinemaRepository.existsByNameAndAddress(dto.getName(), dto.getAddress())) {
                Cinema cinema = new Cinema();
                cinema.setName(dto.getName());
                cinema.setAddress(dto.getAddress());
                cinema.setLatitude(dto.getLat());
                cinema.setScreens(dto.getScreens());
                cinema.setLongitude(dto.getLng());
                cinema.setStartTime(LocalTime.parse(dto.getStartTime()));
                cinema.setEndTime(LocalTime.parse(dto.getEndTime()));
                Cinema savedCinema = cinemaRepository.save(cinema);

                for (int i = 1; i <= dto.getScreens(); i++) {
                    ScreenRoom screenRoom = new ScreenRoom();
                    screenRoom.setCinema(savedCinema);
                    screenRoom.setRoomNm(i + "관");
                    screenRoom.setTotalSeats(50);
                    //screenRoom.setAvailableSeats();
                    screenRoomRepository.save(screenRoom);
                }
            }
        }

        cinemaRepository.saveAll(newCinemas);
    }



    public List<Cinema> getAll() {
        return cinemaRepository.findAll();
    }
}
