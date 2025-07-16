package com.movie.repository.store;

import com.movie.entity.member.Member;
import com.movie.entity.store.StoreMemberPaymentInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreMemberPaymentInfoRepository extends JpaRepository<StoreMemberPaymentInfo,String> {

    boolean existsById(String merchantUid);
    List<StoreMemberPaymentInfo> findByMember(Member member);
    List<StoreMemberPaymentInfo> findAllByMember_MemberIdOrderByMerchantUidDesc(String memberId);
    Optional<StoreMemberPaymentInfo> findByMerchantUid(String merchantUid);
}
