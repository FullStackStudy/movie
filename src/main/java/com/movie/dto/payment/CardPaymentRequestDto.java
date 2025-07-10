package com.movie.dto.payment;

import lombok.Data;

@Data
public class CardPaymentRequestDto {
    private String cardNumber;
    private String cardExpiry;
    private String cardCvc;
    private String cardHolderName;
    private int amount;
    private String orderId;
    private String memberId;
    private String movieTitle;
    private String cinemaName;
    private String screenRoomName;
    private String seat;
    private int usePoint;
} 