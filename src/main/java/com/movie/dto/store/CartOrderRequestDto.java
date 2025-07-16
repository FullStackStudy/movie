package com.movie.dto.store;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CartOrderRequestDto {
    private String memberId;
    private String cinemaName;
    @JsonProperty("cartOrderDtoList")  // JSON 필드명 명시
    private List<CartOrderDto> cartItems;
}
