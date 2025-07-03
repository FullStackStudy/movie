package com.movie.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/* 그냥 좌석만 */
@Entity
@Table(name = "seat")
@Getter
@Setter
@ToString
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long id;

    private String seatRow;
    private int seatColumn;
    private int price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ScreenRoom screenRoom;

    // Seat.java (임시 필드 또는 DTO에서 사용)
    @Transient
    private boolean occupied; // DB에는 없지만 UI 렌더링용으로 사용

    /*@OneToMany(mappedBy = "seat")
    private List<ReservedSeat> reservedSeats = new ArrayList<>();*/
    @OneToMany(mappedBy = "seat")
    private List<ReservedSeat> reservedSeats = new ArrayList<>();



}
