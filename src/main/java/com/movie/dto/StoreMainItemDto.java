package com.movie.dto;

import com.movie.constant.Menu;
import com.movie.entity.Item;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreMainItemDto {
    private Long id;
    private String itemNm;
    private String itemDetail;
    private String imgUrl;
    private Integer price;
    private String itemComposition;
    private Menu menu;
    private Item item;

    @QueryProjection
    public StoreMainItemDto(Long id,String itemNm,String itemDetail,String imgUrl,Integer price,String itemComposition,Menu menu){
        this.id = id;
        this.itemNm = itemNm;
        this.itemDetail = itemDetail;
        this.imgUrl = imgUrl;
        this.price = price;
        this.itemComposition = itemComposition;
        this.menu = menu;
    }
}
