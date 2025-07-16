package com.movie.dto.store;

import com.movie.entity.store.StoreMemberOrderItem;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class StoreMemberOrderItemDto {
    private Long id;
    private String itemNm;
    private String itemComposition;
    private int price;
    private int quantity;
    private LocalTime pickupTime;
    private Long cartItemId;
    private String cinemaName;

    public StoreMemberOrderItemDto(StoreMemberOrderItem entity) {
        this.id = entity.getId();
        this.itemNm = entity.getItemNm();
        this.price = entity.getPrice();
        this.pickupTime = entity.getPickupTime();
        this.itemComposition = entity.getItemComposition();
        this.cinemaName = entity.getCinemaName();
        this.quantity = entity.getQuantity();
    }
}
