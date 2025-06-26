package com.movie.repository;

import com.movie.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByScreenRoom_Cinema_Name(String cinemaName);
}
