package com.movie.service;

import com.movie.dto.CinemaDto;
import com.movie.entity.Cinema;
import com.movie.repository.CinemaRepository;
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

    // CinemaService.java
    public void saveCinemas(List<CinemaDto> dtoList) {
        List<Cinema> newCinemas = new ArrayList<>();

        for (CinemaDto dto : dtoList) {
            if (!cinemaRepository.existsByNameAndAddress(dto.getName(), dto.getAddress())) {
                Cinema cinema = new Cinema();
                cinema.setName(dto.getName());
                cinema.setAddress(dto.getAddress());
                cinema.setScreens(dto.getScreens());
                cinema.setLatitude(dto.getLat());
                cinema.setLongitude(dto.getLng());
                cinema.setStartTime(LocalTime.parse(dto.getStartTime()));
                cinema.setEndTime(LocalTime.parse(dto.getEndTime()));
                newCinemas.add(cinema);
            }
        }

        cinemaRepository.saveAll(newCinemas);
    }



    public List<Cinema> getAll() {
        return cinemaRepository.findAll();
    }
}
