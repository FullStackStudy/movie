package com.movie.service.payment;

import com.movie.dto.payment.MovieOrderDto;
import com.movie.dto.payment.PaymentInfoDto;
import com.movie.entity.member.Member;
import com.movie.entity.payment.MovieOrder;
import com.movie.repository.member.MemberRepository;
import com.movie.repository.payment.MovieOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieOrderService {

    private final MovieOrderRepository movieOrderRepository;
    private final MemberRepository memberRepository;

    /**
     * 결제 성공 시 주문 정보를 저장
     */
    @Transactional
    public MovieOrder saveOrder(PaymentInfoDto paymentInfo, String orderNumber, 
                               String transactionId, String paymentMethod, 
                               Integer usedPoint, String partnerOrderId, String pgToken) {
        
        Member member = memberRepository.findById(paymentInfo.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        MovieOrder movieOrder = new MovieOrder();
        movieOrder.setOrderNumber(orderNumber);
        movieOrder.setTransactionId(transactionId);
        movieOrder.setMember(member);
        movieOrder.setMovieTitle(paymentInfo.getMovieTitle());
        movieOrder.setCinemaName(paymentInfo.getCinemaName());
        movieOrder.setCinemaAddress(paymentInfo.getCinemaAddress());
        movieOrder.setScreenRoomName(paymentInfo.getScreenRoomName());
        movieOrder.setSeat(paymentInfo.getSeat());
        movieOrder.setMovieStart(paymentInfo.getMovieStart());
        movieOrder.setPer(paymentInfo.getPer());
        movieOrder.setPaymentAmount(paymentInfo.getMoviePrice() - usedPoint);
        movieOrder.setUsedPoint(usedPoint);
        movieOrder.setPaymentMethod(paymentMethod);
        movieOrder.setPaymentStatus("SUCCESS");
        movieOrder.setPaymentDate(LocalDateTime.now());
        movieOrder.setPartnerOrderId(partnerOrderId);
        movieOrder.setPgToken(pgToken);

        MovieOrder savedOrder = movieOrderRepository.save(movieOrder);
        log.info("주문 저장 완료: orderNumber={}, memberId={}", orderNumber, paymentInfo.getMemberId());
        
        return savedOrder;
    }

    /**
     * 회원별 주문 목록 조회
     */
    public List<MovieOrderDto> getOrdersByMemberId(String memberId) {
        List<MovieOrder> orders = movieOrderRepository.findByMemberMemberIdOrderByPaymentDateDesc(memberId);
        return orders.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 주문번호로 주문 조회
     */
    public MovieOrderDto getOrderByOrderNumber(String orderNumber) {
        MovieOrder order = movieOrderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));
        return convertToDto(order);
    }

    /**
     * Entity를 DTO로 변환
     */
    private MovieOrderDto convertToDto(MovieOrder order) {
        MovieOrderDto dto = new MovieOrderDto();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setTransactionId(order.getTransactionId());
        dto.setMemberId(order.getMember().getMemberId());
        dto.setMovieTitle(order.getMovieTitle());
        dto.setCinemaName(order.getCinemaName());
        dto.setCinemaAddress(order.getCinemaAddress());
        dto.setScreenRoomName(order.getScreenRoomName());
        dto.setSeat(order.getSeat());
        dto.setMovieStart(order.getMovieStart());
        dto.setPer(order.getPer());
        dto.setPaymentAmount(order.getPaymentAmount());
        dto.setUsedPoint(order.getUsedPoint());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setPaymentDate(order.getPaymentDate());
        dto.setPartnerOrderId(order.getPartnerOrderId());
        dto.setPgToken(order.getPgToken());
        return dto;
    }
} 