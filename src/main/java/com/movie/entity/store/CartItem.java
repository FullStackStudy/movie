package com.movie.entity.store;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.movie.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalTime;

@Entity
@Getter
@Setter
@Table(name = "cart_item")
public class CartItem extends BaseEntity {
    @Id
    @GeneratedValue
    @Column(name = "cart_item_id")
    private Long id;

    @Column(name = "payment_token")
    private String paymentToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;
    private int count;

    @Convert(converter = com.movie.converter.BooleanConverter.class)
    private Boolean isPast;

    @Convert(converter = com.movie.converter.BooleanConverter.class)
    private Boolean isCart;

    @JoinColumn(name = "cinema_name")
    private String cinemaName;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime pickupTime;
    private LocalTime movieTime;


    public static CartItem createCartItem(Cart cart,Item item,int count,LocalTime pickupTime,String cinemaName){
        CartItem cartItem = new CartItem();
        cartItem.setCart(cart);
        cartItem.setItem(item);
        cartItem.setCount(count);
        cartItem.setPickupTime(pickupTime);
        cartItem.setCinemaName(cinemaName);
        cartItem.setIsCart(true);
        return cartItem;
    }
    public void addCount(int count){this.count+= count;}
    public void updateCount(int count){this.count = count;}
}
