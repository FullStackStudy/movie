package com.movie.service.payment;

import com.movie.dto.payment.CardPaymentRequestDto;
import com.movie.dto.payment.CardPaymentResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class CardPaymentService {

    public CardPaymentResponseDto processCardPayment(CardPaymentRequestDto requestDto) {
        CardPaymentResponseDto responseDto = new CardPaymentResponseDto();
        
        try {
            // 카드번호 유효성 검사 (간단한 검증)
            if (!isValidCardNumber(requestDto.getCardNumber())) {
                responseDto.setSuccess(false);
                responseDto.setMessage("유효하지 않은 카드번호입니다.");
                return responseDto;
            }
            
            // 만료일 유효성 검사
            if (!isValidExpiry(requestDto.getCardExpiry())) {
                responseDto.setSuccess(false);
                responseDto.setMessage("유효하지 않은 만료일입니다.");
                return responseDto;
            }
            
            // CVC 유효성 검사
            if (!isValidCvc(requestDto.getCardCvc())) {
                responseDto.setSuccess(false);
                responseDto.setMessage("유효하지 않은 CVC입니다.");
                return responseDto;
            }
            
            // 결제 처리 시뮬레이션 (실제로는 결제 게이트웨이 API 호출)
            log.info("카드결제 처리 중: 주문번호={}, 금액={}", requestDto.getOrderId(), requestDto.getAmount());
            
            // 성공 응답 설정
            responseDto.setSuccess(true);
            responseDto.setTransactionId("TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            responseDto.setMessage("결제가 성공적으로 처리되었습니다.");
            responseDto.setOrderId(requestDto.getOrderId());
            responseDto.setAmount(requestDto.getAmount());
            responseDto.setPaymentDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            log.info("카드결제 성공: 거래번호={}", responseDto.getTransactionId());
            
        } catch (Exception e) {
            log.error("카드결제 처리 중 오류 발생", e);
            responseDto.setSuccess(false);
            responseDto.setMessage("결제 처리 중 오류가 발생했습니다.");
        }
        
        return responseDto;
    }
    
    private boolean isValidCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 13 || cardNumber.length() > 19) {
            return false;
        }
        
        // Luhn 알고리즘으로 카드번호 유효성 검사
        int sum = 0;
        boolean alternate = false;
        
        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(cardNumber.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        
        return (sum % 10 == 0);
    }
    
    private boolean isValidExpiry(String expiry) {
        if (expiry == null || !expiry.matches("\\d{2}/\\d{2}")) {
            return false;
        }
        
        try {
            String[] parts = expiry.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]);
            
            if (month < 1 || month > 12) {
                return false;
            }
            
            // 현재 년도의 마지막 두 자리
            int currentYear = LocalDateTime.now().getYear() % 100;
            int currentMonth = LocalDateTime.now().getMonthValue();
            
            if (year < currentYear || (year == currentYear && month < currentMonth)) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isValidCvc(String cvc) {
        return cvc != null && cvc.matches("\\d{3,4}");
    }
} 