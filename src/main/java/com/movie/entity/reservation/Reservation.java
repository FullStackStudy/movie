package com.movie.entity.reservation;

import com.movie.constant.ReservationStatus;
import com.movie.entity.common.BaseEntity;
import com.movie.entity.common.BaseTimeEntity;
import com.movie.entity.member.Member;
import com.movie.entity.cinema.Schedule;
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
    private LocalDateTime reservedAt;

    @Enumerated(EnumType.STRING)
    private ReservationStatus reservationStatus;

    private LocalDateTime cancelAt; // 예약취소한 시간

    //결제 관한
    private Integer price;
    private String payMethod;
}
