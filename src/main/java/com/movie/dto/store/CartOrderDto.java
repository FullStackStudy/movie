package com.movie.dto.store;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class CartOrderDto {
    private Long cartItemId;
    private int quantity;
    private LocalTime pickupTime;
}
