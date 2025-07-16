package com.movie.dto.store;

import com.movie.constant.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
//결제 정보 파싱용
public class PaymentInfoDto {
    private String impUid;
    private String merchantUid;
    private int amount;
    private OrderStatus status;
}
