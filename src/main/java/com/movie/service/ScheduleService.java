package com.movie.service;

import com.movie.dto.CinemaDto;
import com.movie.dto.ScheduleDto;
import com.movie.dto.SeatDto;
import com.movie.entity.Cinema;
import com.movie.entity.Movie;
import com.movie.entity.Schedule;
import com.movie.entity.ScreenRoom;
import com.movie.repository.MovieRepository;
import com.movie.repository.ScheduleRepository;
import com.movie.repository.ScreenRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleService {
    private final MovieRepository movieRepository;
    private final ScreenRoomRepository screenRoomRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional
    public void registerFromDto(ScheduleDto dto) {
        Movie movie = movieRepository.findByMovieTitle(dto.getMovieTitle())
                .orElseThrow(() -> new IllegalArgumentException("해당 영화가 존재하지 않습니다: " + dto.getMovieTitle()));

        ScreenRoom room = screenRoomRepository.findByRoomNm(dto.getScreenRoomName())
                .orElseThrow(() -> new IllegalArgumentException("해당 상영관이 존재하지 않습니다: " + dto.getScreenRoomName()));

        Schedule schedule = new Schedule();
        schedule.setMovie(movie);
        schedule.setScreenRoom(room);
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());

        scheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public List<ScheduleDto> getSchedulesByCinemaName(String cinemaName) {
        List<Schedule> schedules = scheduleRepository.findByScreenRoom_Cinema_Name(cinemaName);

        return schedules.stream().map(schedule -> {
            ScheduleDto dto = new ScheduleDto();
            dto.setMovieTitle(schedule.getMovie().getMovieTitle());
            dto.setScreenRoomName(schedule.getScreenRoom().getRoomNm());
            dto.setStartTime(schedule.getStartTime());
            dto.setEndTime(schedule.getEndTime());
            return dto;
        }).collect(Collectors.toList());
    }

}
