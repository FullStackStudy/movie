package com.movie.dto.payment;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MovieOrderDto {
    private Long id;
    private String orderNumber;
    private String transactionId;
    private String memberId;
    private String movieTitle;
    private String cinemaName;
    private String cinemaAddress;
    private String screenRoomName;
    private String seat;
    private String movieStart;
    private Long per; // 예매수
    private Integer paymentAmount;
    private Integer usedPoint;
    private String paymentMethod;
    private String paymentStatus;
    private LocalDateTime paymentDate;
    private String partnerOrderId;
    private String pgToken;
} 