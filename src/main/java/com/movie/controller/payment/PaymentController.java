package com.movie.controller.payment;

import com.movie.dto.payment.PaymentInfoDto;
import com.movie.dto.payment.KakaoPayReadyRequestDto;
import com.movie.dto.payment.KakaoPayReadyResponseDto;
import com.movie.dto.payment.KakaoPayApprovalRequestDto;
import com.movie.dto.payment.KakaoPayApprovalResponseDto;
import com.movie.dto.payment.CardPaymentRequestDto;
import com.movie.dto.payment.CardPaymentResponseDto;
import com.movie.repository.member.MemberRepository;
import com.movie.entity.member.Member;
import com.movie.service.payment.KakaoPayService;
import com.movie.service.payment.CardPaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpSession;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final MemberRepository memberRepository;
    private final KakaoPayService kakaoPayService;
    private final CardPaymentService cardPaymentService;
    
    @Value("${kakao.pay.base-url}")
    private String kakaoPayBaseUrl;

    // member_reserve를 파싱하는 공통 메서드
    private PaymentInfoDto parseReserveInfo(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        PaymentInfoDto dto = new PaymentInfoDto();
        dto.setMemberId(memberId);
        dto.setMoviePrice(12000); // 기본 가격 설정

        // member_reserve 에서 정보 파싱
        String reserve = member.getReserve();
        if (reserve != null && !reserve.isEmpty()) {
            String[] reserveLines = reserve.split("\\n");
            if (reserveLines.length > 0) {
                String firstReserve = reserveLines[0];
                String[] reserveInfo = firstReserve.split(",");

                if (reserveInfo.length >= 6) {
                    dto.setMovieStart(reserveInfo[0].trim()); // 영화시작시간 (인덱스 0)
                    dto.setCinemaName(reserveInfo[1].trim()); // 영화관명 (인덱스 1)
                    dto.setCinemaAddress(reserveInfo[2].trim()); // 주소 (인덱스 2)
                    dto.setScreenRoomName(reserveInfo[3].trim()); // 상영관 (인덱스 3)
                    dto.setSeat(reserveInfo[4].trim()); // 좌석 (인덱스 4)
                    dto.setMovieTitle(reserveInfo[5].trim()); // 영화제목 (인덱스 5)
                }
            }
        }

        return dto;
    }

    @GetMapping("/movie/payment")
    public String moviePaymentPage(@RequestParam String memberId,
                              Model model) {
        // memberId로 회원 정보 조회
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            model.addAttribute("error", "존재하지 않는 회원입니다.");
            return "payment/paymentFail";
        }

        PaymentInfoDto dto = parseReserveInfo(memberId);

        // 회원 포인트 조회
        int memberPoint = 0;
        if (member.getPoint() != null) {
            try {
                memberPoint = Integer.parseInt(member.getPoint());
            } catch (Exception e) {
                log.warn("포인트 파싱 실패: {}", member.getPoint());
            }
        }

        model.addAttribute("paymentInfo", dto);
        model.addAttribute("memberPoint", memberPoint);
        return "payment/moviePaymentPage";
    }

    // 결제 테스트용 엔드포인트
    @GetMapping("/movie/payment/test")
    public String paymentTest(Model model) {
        // test@test.com 계정의 reserve 내역을 파싱해서 사용
        String memberId = "test@test.com";
        Member member = memberRepository.findById(memberId).orElse(null);
        
        if (member == null) {
            model.addAttribute("error", "테스트 계정을 찾을 수 없습니다.");
            return "payment/paymentFail";
        }

        PaymentInfoDto dto = parseReserveInfo(memberId);

        // 회원 포인트 조회
        int memberPoint = 0;
        if (member.getPoint() != null) {
            try {
                memberPoint = Integer.parseInt(member.getPoint());
            } catch (Exception e) {
                log.warn("포인트 파싱 실패: {}", member.getPoint());
            }
        }

        model.addAttribute("paymentInfo", dto);
        model.addAttribute("memberPoint", memberPoint);
        return "payment/moviePaymentPage";
    }

    @PostMapping("/movie/payment/kakao-pay-ready")
    public String kakaoPayReady(@RequestParam String memberId,
                                @RequestParam(defaultValue = "0") int usePoint,
                                HttpSession session,
                                Model model) {

        // member_reserve에서 정보 파싱
        PaymentInfoDto paymentInfo = parseReserveInfo(memberId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        int currentPoint = 0;
        try {
            currentPoint = Integer.parseInt(member.getPoint());
        } catch (Exception e) {
        }

        // 사용 포인트가 보유 포인트보다 많으면 실패
        if (usePoint > currentPoint) {
            model.addAttribute("error", "보유 포인트를 초과하여 사용할 수 없습니다.");
            return "payment/paymentFail";
        }

        // 실제 결제 금액 계산
        int finalPrice = paymentInfo.getMoviePrice() - Math.min(usePoint, paymentInfo.getMoviePrice());

        // 주문 ID 생성 (현재 시간 기반)
        String orderId = "ORDER_" + System.currentTimeMillis();

        // 카카오페이 준비 요청
        KakaoPayReadyRequestDto requestDto = new KakaoPayReadyRequestDto();
        requestDto.setPartner_order_id(orderId);
        requestDto.setPartner_user_id(memberId);
        requestDto.setItem_name(paymentInfo.getMovieTitle() + " - " + paymentInfo.getCinemaName() + " " + paymentInfo.getScreenRoomName() + " " + paymentInfo.getSeat());
        requestDto.setQuantity(1);
        requestDto.setTotal_amount(finalPrice);
        requestDto.setTax_free_amount(0);
        String approvalUrl = "http://localhost:80/movie/payment/kakao-pay-success";
        String cancelUrl = "http://localhost:80/movie/payment/kakao-pay-cancel";
        String failUrl = "http://localhost:80/movie/payment/kakao-pay-fail";
        
        log.info("카카오페이 URL 설정 - approval: {}, cancel: {}, fail: {}", approvalUrl, cancelUrl, failUrl);
        
        requestDto.setApproval_url(approvalUrl);
        requestDto.setCancel_url(cancelUrl);
        requestDto.setFail_url(failUrl);

        try {
            KakaoPayReadyResponseDto responseDto = kakaoPayService.kakaoPayReady(requestDto);
            
            // tid를 세션에 저장 (승인 요청 시 사용)
            session.setAttribute("kakao_tid", responseDto.getTid());
            session.setAttribute("kakao_orderId", orderId);
            session.setAttribute("kakao_memberId", memberId);
            session.setAttribute("kakao_usePoint", usePoint);
            
            log.info("카카오페이 준비 성공 - tid: {}, orderId: {}", responseDto.getTid(), orderId);
            return "redirect:" + responseDto.getNext_redirect_pc_url();
        } catch (Exception e) {
            log.error("카카오페이 준비 요청 실패", e);
            model.addAttribute("error", "결제 준비에 실패했습니다.");
            return "payment/paymentFail";
        }
    }

    @GetMapping("/movie/payment/kakao-pay-success")
    public String kakaoPaySuccess(@RequestParam String pg_token,
                                  HttpSession session,
                                  Model model) {

        // 세션에서 저장된 정보 가져오기
        String tid = (String) session.getAttribute("kakao_tid");
        String orderId = (String) session.getAttribute("kakao_orderId");
        String memberId = (String) session.getAttribute("kakao_memberId");
        Integer usePoint = (Integer) session.getAttribute("kakao_usePoint");
        
        if (tid == null || orderId == null || memberId == null || usePoint == null) {
            log.error("세션에서 카카오페이 정보를 찾을 수 없습니다.");
            model.addAttribute("error", "결제 정보를 찾을 수 없습니다.");
            return "payment/paymentFail";
        }
        
        // member_reserve에서 정보 파싱
        PaymentInfoDto paymentInfo = parseReserveInfo(memberId);

        try {
            // 카카오페이 승인 요청
            KakaoPayApprovalRequestDto requestDto = new KakaoPayApprovalRequestDto();
            requestDto.setTid(tid);
            requestDto.setPartner_order_id(orderId);
            requestDto.setPartner_user_id(memberId);
            requestDto.setPg_token(pg_token);

            KakaoPayApprovalResponseDto responseDto = kakaoPayService.kakaoPayApprove(requestDto);

            // 결제 성공 시 회원 정보 업데이트
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

            int currentPoint = 0;
            try {
                currentPoint = Integer.parseInt(member.getPoint());
            } catch (Exception e) {
            }

            // 포인트 차감
            int newPoint = currentPoint - usePoint;
            member.setPoint(String.valueOf(newPoint));

            // 예약 정보 업데이트 (결제 완료 표시)
            String reserve = member.getReserve();
            if (reserve != null && !reserve.isEmpty()) {
                String[] reserveLines = reserve.split("\\n");
                if (reserveLines.length > 0) {
                    // 첫 번째 예약에 결제 완료 표시 추가
                    reserveLines[0] = reserveLines[0] + ",결제완료";
                    member.setReserve(String.join("\n", reserveLines));
                }
            }

            memberRepository.save(member);

            // 세션 정리
            session.removeAttribute("kakao_tid");
            session.removeAttribute("kakao_orderId");
            session.removeAttribute("kakao_memberId");
            session.removeAttribute("kakao_usePoint");

            log.info("카카오페이 결제 성공 - memberId: {}, orderId: {}, usePoint: {}", memberId, orderId, usePoint);
            return "redirect:/movie/payment/success";

        } catch (Exception e) {
            log.error("카카오페이 승인 요청 실패", e);
            model.addAttribute("error", "결제 승인에 실패했습니다.");
            return "payment/paymentFail";
        }
    }

    @GetMapping("/movie/payment/kakao-pay-cancel")
    public String kakaoPayCancel(HttpSession session, Model model) {
        log.info("카카오페이 결제 취소");
        
        // 세션 정리
        session.removeAttribute("kakao_tid");
        session.removeAttribute("kakao_orderId");
        session.removeAttribute("kakao_memberId");
        session.removeAttribute("kakao_usePoint");
        
        model.addAttribute("error", "결제가 취소되었습니다.");
        return "payment/paymentFail";
    }

    @GetMapping("/movie/payment/kakao-pay-fail")
    public String kakaoPayFail(HttpSession session, Model model) {
        log.info("카카오페이 결제 실패");
        
        // 세션 정리
        session.removeAttribute("kakao_tid");
        session.removeAttribute("kakao_orderId");
        session.removeAttribute("kakao_memberId");
        session.removeAttribute("kakao_usePoint");
        
        model.addAttribute("error", "결제에 실패했습니다.");
        return "payment/paymentFail";
    }

    @GetMapping("/movie/payment/card")
    public String cardPaymentPage(@RequestParam String memberId,
                                  @RequestParam(defaultValue = "0") int usePoint,
                                  Model model) {
        // memberId로 회원 정보 조회
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            model.addAttribute("error", "존재하지 않는 회원입니다.");
            return "payment/paymentFail";
        }

        PaymentInfoDto dto = parseReserveInfo(memberId);

        // 회원 포인트 조회
        int memberPoint = 0;
        if (member.getPoint() != null) {
            try {
                memberPoint = Integer.parseInt(member.getPoint());
            } catch (Exception e) {
                log.warn("포인트 파싱 실패: {}", member.getPoint());
            }
        }

        // 사용 포인트가 보유 포인트보다 많으면 실패
        if (usePoint > memberPoint) {
            model.addAttribute("error", "보유 포인트를 초과하여 사용할 수 없습니다.");
            return "payment/paymentFail";
        }

        model.addAttribute("paymentInfo", dto);
        model.addAttribute("memberPoint", memberPoint);
        model.addAttribute("usePoint", usePoint);
        return "payment/cardPaymentPage";
    }

    @PostMapping("/movie/payment/card/process")
    public String processCardPayment(@RequestParam String cardNumber,
                                     @RequestParam String cardExpiry,
                                     @RequestParam String cardCvc,
                                     @RequestParam String cardHolderName,
                                     @RequestParam String memberId,
                                     @RequestParam int usePoint,
                                     Model model) {
        try {
            // member_reserve에서 정보 파싱
            PaymentInfoDto paymentInfo = parseReserveInfo(memberId);
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

            int currentPoint = 0;
            try {
                currentPoint = Integer.parseInt(member.getPoint());
            } catch (Exception e) {
            }

            // 사용 포인트가 보유 포인트보다 많으면 실패
            if (usePoint > currentPoint) {
                model.addAttribute("error", "보유 포인트를 초과하여 사용할 수 없습니다.");
                return "payment/paymentFail";
            }

            // 실제 결제 금액 계산
            int finalPrice = paymentInfo.getMoviePrice() - Math.min(usePoint, paymentInfo.getMoviePrice());

            // 주문 ID 생성
            String orderId = "ORDER_" + System.currentTimeMillis();

            // 카드 결제 요청
            CardPaymentRequestDto requestDto = new CardPaymentRequestDto();
            requestDto.setOrderId(orderId);
            requestDto.setAmount(finalPrice);
            requestDto.setCardNumber(cardNumber);
            requestDto.setCardExpiry(cardExpiry);
            requestDto.setCardCvc(cardCvc);
            requestDto.setCardHolderName(cardHolderName);

            CardPaymentResponseDto responseDto = cardPaymentService.processCardPayment(requestDto);

            if (responseDto.isSuccess()) {
                // 결제 성공 시 회원 정보 업데이트
                int newPoint = currentPoint - usePoint;
                member.setPoint(String.valueOf(newPoint));

                // 예약 정보 업데이트 (결제 완료 표시)
                String reserve = member.getReserve();
                if (reserve != null && !reserve.isEmpty()) {
                    String[] reserveLines = reserve.split("\\n");
                    if (reserveLines.length > 0) {
                        // 첫 번째 예약에 결제 완료 표시 추가
                        reserveLines[0] = reserveLines[0] + ",결제완료";
                        member.setReserve(String.join("\n", reserveLines));
                    }
                }

                memberRepository.save(member);

                log.info("카드 결제 성공 - memberId: {}, orderId: {}, usePoint: {}", memberId, orderId, usePoint);
                return "redirect:/movie/payment/success";
            } else {
                model.addAttribute("error", responseDto.getMessage());
                return "payment/paymentFail";
            }

        } catch (Exception e) {
            log.error("카드 결제 처리 중 오류", e);
            model.addAttribute("error", "결제 처리 중 오류가 발생했습니다.");
            return "payment/paymentFail";
        }
    }

    @PostMapping("/movie/payment/complete")
    public String completePayment(
            @RequestParam String memberId,
            @RequestParam(defaultValue = "0") int usePoint,
            Model model) {
        try {
            // member_reserve에서 정보 파싱
            PaymentInfoDto paymentInfo = parseReserveInfo(memberId);
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

            int currentPoint = 0;
            try {
                currentPoint = Integer.parseInt(member.getPoint());
            } catch (Exception e) {
            }

            // 사용 포인트가 보유 포인트보다 많으면 실패
            if (usePoint > currentPoint) {
                model.addAttribute("error", "보유 포인트를 초과하여 사용할 수 없습니다.");
                return "payment/paymentFail";
            }

            // 포인트 차감
            int newPoint = currentPoint - usePoint;
            member.setPoint(String.valueOf(newPoint));

            // 예약 정보 업데이트 (결제 완료 표시)
            String reserve = member.getReserve();
            if (reserve != null && !reserve.isEmpty()) {
                String[] reserveLines = reserve.split("\\n");
                if (reserveLines.length > 0) {
                    // 첫 번째 예약에 결제 완료 표시 추가
                    reserveLines[0] = reserveLines[0] + ",결제완료";
                    member.setReserve(String.join("\n", reserveLines));
                }
            }

            memberRepository.save(member);

            log.info("포인트 결제 성공 - memberId: {}, usePoint: {}", memberId, usePoint);
            return "redirect:/movie/payment/success";

        } catch (Exception e) {
            log.error("포인트 결제 처리 중 오류", e);
            model.addAttribute("error", "결제 처리 중 오류가 발생했습니다.");
            return "payment/paymentFail";
        }
    }

    @GetMapping("/movie/payment/success")
    public String paymentSuccess(Model model) {
        return "payment/paymentSuccess";
    }
}