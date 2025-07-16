package com.movie.dto.store;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PortOnePaymentResponse {
    private String impUid;
    private String merchantUid;
    private int amount;
    private String status;
    private boolean success;
}
