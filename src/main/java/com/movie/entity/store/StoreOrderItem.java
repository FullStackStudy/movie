package com.movie.entity.store;

import com.movie.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Getter
@Setter
public class StoreOrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String itemNm;             // 상품 이름
    private String itemComposition;  // 구성 (예: 음료 + 팝콘)
    private int price;               // 가격
    private int quantity;            // 수량
    private LocalTime pickupTime;    // 픽업 시간

    private String memberId;
    private boolean isCart;

    @JoinColumn(name = "cinema_name")
    private String cinemaName;

    // 결제 정보와 다대일 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_payment_info_id")
    private StorePaymentInfo storePaymentInfo;
}
