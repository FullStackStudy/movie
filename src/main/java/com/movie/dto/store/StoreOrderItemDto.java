package com.movie.dto.store;

import com.movie.entity.store.CartItem;

import com.movie.entity.store.Item;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StoreOrderItemDto {
    private Long id;
    private String itemNm;
    private String itemComposition;
    private int price;
    private int quantity;
    private String pickupTime;
    private Long cartItemId;
    private String cinemaName;
    public StoreOrderItemDto(Item item, String cinemaName) {
        this.id = item.getId();
        this.itemNm = item.getItemNm();
        this.price = item.getPrice();
        this.pickupTime = null;
        this.itemComposition = item.getItemComposition();
        this.cinemaName = cinemaName;
    }
    public static StoreOrderItemDto of(CartItem cartItem) {
        Item item = (Item) cartItem.getItem();

        StoreOrderItemDto dto = new StoreOrderItemDto(item, cartItem.getCinemaName());
        dto.setQuantity(cartItem.getCount());
        dto.setPickupTime(cartItem.getPickupTime().toString());
        dto.setCartItemId(cartItem.getId());
        return dto;
    }
}
