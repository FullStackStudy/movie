package com.movie.dto.store;

import com.movie.constant.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class StoreMemberPaymentInfoDto {
    private String impUid;
    private String merchantUid;
    private int amount;
    private OrderStatus status;
    private String memberId;
    private LocalTime earliestPickupTime;

    private List<StoreMemberOrderItemDto> items;

    public boolean isAfterFiveMinutes() {
        if (earliestPickupTime == null) return false;
        return ChronoUnit.MINUTES.between(LocalTime.now(), earliestPickupTime) > 5;
    }

    public boolean isWithinFiveMinutes() {
        if (earliestPickupTime == null) return false;

        LocalTime now = LocalTime.now();
        long minutes = ChronoUnit.MINUTES.between(now, earliestPickupTime);
        return minutes <= 5;
    }


    public StoreMemberPaymentInfoDto(String memberId, List<StoreMemberOrderItemDto> items) {
        this.memberId = memberId;

        // 방어 코드: null 또는 빈 리스트일 경우 처리
        if (items == null || items.isEmpty()) {
            this.items = Collections.emptyList();
            this.amount = 0;
        } else {
            this.items = items;
            this.amount = items.stream()
                    .mapToInt(i -> i.getPrice() * i.getQuantity())
                    .sum();
        }
    }

    public int getTotalPrice() {
        return amount;
    }

}

