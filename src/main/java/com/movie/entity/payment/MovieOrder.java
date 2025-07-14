package com.movie.entity.payment;

import com.movie.entity.common.BaseTimeEntity;
import com.movie.entity.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "movie_order")
@Getter
@Setter
@ToString
public class MovieOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber; // 주문번호 (ORDER_1234567890)

    @Column(name = "transaction_id")
    private String transactionId; // 거래번호 (TXN_A1B2C3D4)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "movie_title", nullable = false)
    private String movieTitle;

    @Column(name = "cinema_name", nullable = false)
    private String cinemaName;

    @Column(name = "cinema_address")
    private String cinemaAddress;

    @Column(name = "screen_room_name", nullable = false)
    private String screenRoomName;

    @Column(name = "seat", nullable = false)
    private String seat;

    @Column(name = "movie_start")
    private String movieStart;

    @Column(name = "per")
    private Long per; // 예매수

    @Column(name = "payment_amount", nullable = false)
    private Integer paymentAmount;

    @Column(name = "used_point")
    private Integer usedPoint;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // KAKAO_PAY, CARD, POINT

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus; // SUCCESS, FAIL, CANCELLED

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "partner_order_id")
    private String partnerOrderId; // 카카오페이용

    @Column(name = "pg_token")
    private String pgToken; // 카카오페이용

    @PrePersist
    protected void onCreate() {
        if (paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }
    }
} 