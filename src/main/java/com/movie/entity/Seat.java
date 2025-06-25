package com.movie.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Character seatRow;
    private int seatColumn;
    private int price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ScreenRoom screenRoom;

    @OneToMany(mappedBy = "seat")
    private List<ReservationSeat> reservedSeats = new ArrayList<>();
}
