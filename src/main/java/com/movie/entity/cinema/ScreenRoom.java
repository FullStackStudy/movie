package com.movie.entity.cinema;

import com.movie.entity.movie.Movie;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "screen_room")
public class ScreenRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;


    @OneToMany(mappedBy = "screenRoom")
    private List<Schedule> schedules;

    @OneToMany(mappedBy = "screenRoom")
    private List<Seat> seats;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    private String roomNm; //상영관 이름

    private int totalSeats; //전체 좌석 수
} 