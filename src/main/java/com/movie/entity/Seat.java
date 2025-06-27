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
    private Long id;

    private String seatRow;
    private int seatColumn;
    private int price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ScreenRoom screenRoom;

    @OneToMany(mappedBy = "seat")
    private List<ReservedSeat> reservedSeats = new ArrayList<>();
}
