package com.movie.entity.store;

import com.movie.constant.OrderStatus;
import com.movie.entity.common.BaseEntity;
import com.movie.entity.member.Member;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class StorePaymentInfo extends BaseEntity {

    @Id
    @Column(name = "merchant_uid")
    private String merchantUid;   // 주문 ID

    private String impUid;        // 결제 고유 번호 (카카오페이의 TID)

    private int amount;           // 총 결제 금액

    @Enumerated(EnumType.STRING)
    private OrderStatus status;   // 결제 상태 (예: PAID, CANCELLED)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
    private Boolean isCart;

    // 주문된 상품 리스트와의 관계
    @OneToMany(mappedBy = "storePaymentInfo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoreOrderItem> items = new ArrayList<>();


}