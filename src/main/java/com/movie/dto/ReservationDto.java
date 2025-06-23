package com.movie.dto;

import com.movie.constant.ReservationStatus;
import jakarta.persistence.*;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReservationDto {
    private Long id;
    private Long user_id;
    private Long movie_id;
    private Long seat_id;
    private LocalDateTime reserved_at;
    private ReservationStatus reservationStatus;
}
