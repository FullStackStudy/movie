package com.movie.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ItemDto {
    private Long id;
    private String itemNm;
    private Integer price;
    private String itemDetail;
    private String itemComposition;
    private String sellStatus;
    private LocalDateTime regTime;
    private LocalDateTime updateTime;
}
