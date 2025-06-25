package com.movie.entity;

import com.movie.dto.ScheduleDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalTime;

@Entity
@Table(name = "schedule")
@Getter
@Setter
@ToString
public class Schedule {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private ScreenRoom screenRoom;

    @ManyToOne
    @JoinColumn(name = "movie_id")
    private Movie movie;

    private LocalTime startTime;
    private LocalTime endTime;

    public static Schedule createSchedule(ScheduleDto dto, Movie movie, ScreenRoom room) {
        Schedule schedule = new Schedule();
        schedule.setMovie(movie);
        schedule.setScreenRoom(room);
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        return schedule;
    }
}
