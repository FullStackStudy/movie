package com.movie.entity;

import com.movie.dto.ScheduleDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "schedule")
@Getter
@Setter
@ToString
@AllArgsConstructor // ✅ 이거 추가!
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long Id;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private ScreenRoom screenRoom;

    @ManyToOne
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @ManyToOne
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;

    private LocalDate showDate; //상영날짜

    private LocalTime startTime; //상영시작시각

    private String status; //상태

    private int availableSeat; //잔여석

    private String description; //주의사항

    protected Schedule() {
    }//JPA용 기본 생성자

    public static Schedule createSchedule(ScheduleDto dto, Cinema cinema, Movie movie, ScreenRoom room) {
        return Schedule.builder()
                .cinema(cinema)
                .movie(movie)
                .screenRoom(room)
                .showDate(dto.getShowDate())
                .startTime(dto.getStartTime())
                .status(dto.getStatus())
                .availableSeat(room.getTotalSeats())
                .description(dto.getDescription())
                .build();
    }

}