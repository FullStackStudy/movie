package com.movie.repository.cinema;

import com.movie.entity.cinema.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByCinema_Name(String cinemaName);
} 