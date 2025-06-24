package com.movie.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "screen_room")
public class ScreenRoom {
    @Id
    @GeneratedValue
    @Column(name = "room_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;

    /*@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;*/

    private String roomNm; //상영관 이름

    private int totalSeats; //전체 좌석 수
    private int availableSeats; //잔여석 -> 총 좌석 수에서 얘매불가를 뺄 예정

}
