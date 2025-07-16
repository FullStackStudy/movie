package com.movie.dto.store;

import com.movie.constant.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class StorePaymentInfoDto {
    private String impUid;
    private String merchantUid;
    private int amount;
    private OrderStatus status;
    private String memberId;
    private List<StoreOrderItemDto> items = new ArrayList<>();

    public StorePaymentInfoDto() {
        this.items = new ArrayList<>();
        this.amount = 0;
    }

    public StorePaymentInfoDto(String memberId, List<StoreOrderItemDto> items) {
        this.memberId = memberId;

        if (items == null || items.isEmpty()) {
            this.items = new ArrayList<>();
            this.amount = 0;
        } else {
            this.items = items;
            this.amount = items.stream()
                    .mapToInt(i -> i.getPrice() * i.getQuantity())
                    .sum();
        }
    }

    public StorePaymentInfoDto(String memberId, List<StoreOrderItemDto> items, int amount) {
        this.memberId = memberId;
        this.items = items;
        this.amount = amount;
    }

    public int getTotalPrice() {
        return amount;
    }
}
