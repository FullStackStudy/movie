package com.movie.entity.reservation;

import com.movie.constant.ReservationStatus;
import com.movie.entity.BaseEntity;
import com.movie.entity.BaseTimeEntity;
import com.movie.entity.Member;
import com.movie.entity.Schedule;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@ToString
@EnableJpaAuditing
@EntityListeners(AuditingEntityListener.class)
public class Reservation {
    @Id
    @Column(name = "reservation_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;


    private String seat_id;

    private Long per; //예약몇명인지

    @CreatedDate
    @Column(name = "reserved_at")
    private LocalDateTime reserved_at;

    @Enumerated(EnumType.STRING)
    private ReservationStatus reservationStatus;

    //결제 관한
    private Integer price;
    private String pay_method;
}
