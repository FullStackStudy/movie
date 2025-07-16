package com.movie.repository.store;

import com.movie.entity.store.StoreOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StoreOrderItemRepository extends JpaRepository<StoreOrderItem, Long> {
    List<StoreOrderItem> findByMemberIdAndIsCart(String memberId, boolean cart);
    @Query("SELECT o FROM StoreOrderItem o JOIN FETCH o.storePaymentInfo p WHERE p.status <> 'CANCEL'")
    List<StoreOrderItem> findAllWithPaymentInfo();

    @Query("SELECT o FROM StoreOrderItem o JOIN FETCH o.storePaymentInfo WHERE o.id = :id")
    Optional<StoreOrderItem> findByIdWithPaymentInfo(Long id);

    @Query("SELECT o FROM StoreOrderItem o WHERE o.storePaymentInfo.merchantUid = :merchantUid")
    List<StoreOrderItem> findAllByMerchantUid(String merchantUid);
}
