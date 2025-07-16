package com.movie.service.store;

import com.movie.dto.store.StoreMemberOrderItemDto;
import com.movie.dto.store.StoreMemberPaymentInfoDto;
import com.movie.entity.store.StoreMemberOrderItem;
import com.movie.entity.store.StoreMemberPaymentInfo;
import com.movie.entity.store.StoreOrderItem;
import com.movie.entity.store.StorePaymentInfo;
import com.movie.repository.store.StoreMemberPaymentInfoRepository;
import com.movie.repository.store.StoreOrderItemRepository;
import com.movie.repository.store.StorePaymentInfoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreMemberPaymentInfoService {

    private final StoreOrderItemRepository storeOrderItemRepository;
    private final StoreMemberPaymentInfoRepository storeMemberPaymentInfoRepository;
    private final StorePaymentInfoRepository storePaymentInfoRepository;

    @Transactional
    public void saveMemberOrderHistory(StorePaymentInfo storePaymentInfo) {
        // DB에서 원본 결제 정보 가져오기
        StorePaymentInfo entity = storePaymentInfoRepository.findByMerchantUid(storePaymentInfo.getMerchantUid())
                .orElseThrow(() -> new IllegalArgumentException("해당 결제 정보 없음"));

        // 이미 저장된 내역인지 체크
        if (storeMemberPaymentInfoRepository.existsById(entity.getMerchantUid())) return;

        // 회원용 결제 이력 생성
        StoreMemberPaymentInfo memberInfo = new StoreMemberPaymentInfo();
        memberInfo.setMerchantUid(entity.getMerchantUid());
        memberInfo.setImpUid(entity.getImpUid());
        memberInfo.setAmount(entity.getAmount());
        memberInfo.setStatus(entity.getStatus());
        memberInfo.setIsCart(entity.getIsCart());
        memberInfo.setMember(entity.getMember());

        // 결제에 해당하는 모든 주문 복사
        List<StoreOrderItem> items = storeOrderItemRepository.findAllByMerchantUid(entity.getMerchantUid());
        for (StoreOrderItem item : items) {
            StoreMemberOrderItem mItem = new StoreMemberOrderItem();
            mItem.setItemNm(item.getItemNm());
            mItem.setItemComposition(item.getItemComposition());
            mItem.setPrice(item.getPrice());
            mItem.setQuantity(item.getQuantity());
            mItem.setPickupTime(item.getPickupTime());
            mItem.setCinemaName(item.getCinemaName());
            mItem.setMemberId(entity.getMember().getMemberId());
            mItem.setStoreMemberOrderInfo(memberInfo);
            memberInfo.getItems().add(mItem);
        }

        storeMemberPaymentInfoRepository.save(memberInfo);
        storeOrderItemRepository.deleteAll(items);  // 원본 삭제 (옵션)
    }

    @Transactional
    public List<StoreMemberPaymentInfoDto> getOrderHistoryByMemberId(String memberId) {
        List<StoreMemberPaymentInfo> orderEntities =
                storeMemberPaymentInfoRepository.findAllByMember_MemberIdOrderByMerchantUidDesc(memberId);

        return orderEntities.stream().map(entity -> {
            // 주문 항목 DTO 변환
            List<StoreMemberOrderItemDto> itemDtos = entity.getItems().stream()
                    .map(StoreMemberOrderItemDto::new)
                    .toList();

            // DTO 생성
            StoreMemberPaymentInfoDto dto = new StoreMemberPaymentInfoDto(entity.getMember().getMemberId(), itemDtos);
            dto.setImpUid(entity.getImpUid());
            dto.setMerchantUid(entity.getMerchantUid());
            dto.setStatus(entity.getStatus());

            // ⏰ 가장 이른 픽업 시간 설정
            LocalTime minPickupTime = itemDtos.stream()
                    .map(StoreMemberOrderItemDto::getPickupTime)
                    .min(LocalTime::compareTo)
                    .orElse(null);
            dto.setEarliestPickupTime(minPickupTime); // dto에 필드와 setter가 있어야 함

            return dto;
        }).toList();
    }

}
