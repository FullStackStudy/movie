package com.movie.dto;

import com.movie.constant.Menu;
import com.movie.entity.Item;
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
    private Menu menu;
    public ItemDto(Menu menu,Item item){
    this.menu = item.getMenu();
    }
}
