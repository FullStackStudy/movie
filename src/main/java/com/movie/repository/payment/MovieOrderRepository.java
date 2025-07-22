package com.movie.repository.payment;

import com.movie.entity.payment.MovieOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieOrderRepository extends JpaRepository<MovieOrder, Long> {
    
    // 주문번호로 주문 조회
    Optional<MovieOrder> findByOrderNumber(String orderNumber);
    
    // 회원별 주문 목록 조회 (최신순)
    List<MovieOrder> findByMemberMemberIdOrderByPaymentDateDesc(String memberId);
    
    // 거래번호로 주문 조회
    Optional<MovieOrder> findByTransactionId(String transactionId);
    
    // 결제 상태별 주문 목록 조회
    List<MovieOrder> findByPaymentStatus(String paymentStatus);
    
    // 회원별 결제 상태별 주문 목록 조회
    List<MovieOrder> findByMemberMemberIdAndPaymentStatus(String memberId, String paymentStatus);
    
    // 회원이 특정 영화를 결제했는지 확인 (결제 성공 상태만)
    boolean existsByMemberMemberIdAndMovieTitleAndPaymentStatus(String memberId, String movieTitle, String paymentStatus);
} 