package com.movie.repository.store;

import com.movie.entity.store.StorePaymentInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StorePaymentInfoRepository extends JpaRepository<StorePaymentInfo,String> {
        Optional<StorePaymentInfo> findByMerchantUid(String merchantUid);
        Optional<StorePaymentInfo> findByImpUid(String impUid);
}
