package com.movie.entity;

import jakarta.persistence.*;

/* 실제 예약 좌석 */
@Entity
public class ReserveSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 예약 정보
    /*@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;*/

    // 좌석 정보
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id")
    private Seat seat;
}
