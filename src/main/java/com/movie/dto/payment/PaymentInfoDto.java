package com.movie.dto.payment;

import lombok.Data;

@Data
public class PaymentInfoDto {
        private String movieStart;
        private String cinemaName;
        private String cinemaAddress;
        private String screenRoomName;
        private String seat; // 예: "A5"
        private String memberId;
        private String movieTitle;
        private int moviePrice;
        private Long per; // 예매수
} 