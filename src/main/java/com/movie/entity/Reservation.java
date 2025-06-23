package com.movie.entity;

import com.movie.constant.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@ToString
public class Reservation extends BaseEntity {
    @Id
    @Column(name = "reservation_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    private Long id;

    private Long user_id;
    private Long movie_id;
    private Long seat_id;

    @CreatedDate
    @Column(name = "reserved_at")
    private LocalDateTime reserved_at;

    @Enumerated(EnumType.STRING)
    private ReservationStatus reservationStatus;
}
