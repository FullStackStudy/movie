package com.movie.dto;

import lombok.Data;

@Data
public class CardPaymentResponseDto {
    private boolean success;
    private String transactionId;
    private String message;
    private String orderId;
    private int amount;
    private String paymentDate;
} 