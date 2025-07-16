package com.movie.controller.payment;

import com.movie.dto.payment.PaymentInfoDto;
import com.movie.dto.payment.KakaoPayReadyRequestDto;
import com.movie.dto.payment.KakaoPayReadyResponseDto;
import com.movie.dto.payment.KakaoPayApprovalRequestDto;
import com.movie.dto.payment.KakaoPayApprovalResponseDto;
import com.movie.dto.payment.CardPaymentRequestDto;
import com.movie.dto.payment.CardPaymentResponseDto;
import com.movie.dto.payment.MovieOrderDto;
import com.movie.dto.reservation.ReservationResponseDto;
import com.movie.repository.member.MemberRepository;
import com.movie.entity.member.Member;
import com.movie.service.payment.KakaoPayService;
import com.movie.service.payment.CardPaymentService;
import com.movie.service.payment.MovieOrderService;
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
    private final MovieOrderService movieOrderService;
    
    @Value("${kakao.pay.base-url}")
    private String kakaoPayBaseUrl;

    // 세션에서 ReservationResponseDto를 가져와서 PaymentInfoDto로 변환하는 메서드
    private PaymentInfoDto parseReservationInfo(HttpSession session) {
        ReservationResponseDto reservationData = (ReservationResponseDto) session.getAttribute("reservedData");
        if (reservationData == null) {
            throw new IllegalArgumentException("예약 정보를 찾을 수 없습니다.");
        }

        PaymentInfoDto dto = new PaymentInfoDto();
        dto.setMemberId(reservationData.getMemberId());
        
        // ReservationResponseDto의 price는 이미 예매수(per)를 고려한 총 금액이므로 그대로 사용
        int totalPrice = reservationData.getPrice() != null ? reservationData.getPrice() : 12000;
        Long per = reservationData.getPer() != null ? reservationData.getPer() : 1L;
        
        dto.setMoviePrice(totalPrice);
        dto.setMovieTitle(reservationData.getMovieName());
        dto.setCinemaName(reservationData.getCinemaName());
        dto.setScreenRoomName(reservationData.getScreenName());
        dto.setSeat(String.join(", ", reservationData.getSeatName()));
        dto.setMovieStart(reservationData.getStartTime() != null ? reservationData.getStartTime().toString() : "");
        dto.setCinemaAddress(""); // ReservationResponseDto에는 주소 정보가 없으므로 빈 문자열로 설정
        dto.setPer(per); // 예매수 설정

        return dto;
    }

    @GetMapping("/movie/payment")
    public String moviePaymentPage(@RequestParam String memberId,
                              HttpSession session,
                              Model model) {
        // 세션에서 예약 정보 확인
        ReservationResponseDto reservationData = (ReservationResponseDto) session.getAttribute("reservedData");
        if (reservationData == null) {
            model.addAttribute("error", "예약 정보를 찾을 수 없습니다. 다시 예약해주세요.");
            return "payment/paymentFail";
        }

        // memberId로 회원 정보 조회
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            model.addAttribute("error", "존재하지 않는 회원입니다.");
            return "payment/paymentFail";
        }

        PaymentInfoDto dto = parseReservationInfo(session);

        // 회원 포인트 조회
        int memberPoint = 0;
        if (member.getPoint() != null) {
            try {
                memberPoint = Integer.parseInt(member.getPoint());
            } catch (Exception e) {
                log.warn("포인트 파싱 실패: {}", member.getPoint());
            }
        }

        // 세션에서 usePoint 가져오기
        Integer usePoint = (Integer) session.getAttribute("usePoint");
        if (usePoint == null) usePoint = 0;
        
        model.addAttribute("paymentInfo", dto);
        model.addAttribute("memberPoint", memberPoint);
        model.addAttribute("usePoint", usePoint);
        return "payment/moviePaymentPage";
    }

    // 결제 테스트용 엔드포인트
    @GetMapping("/movie/payment/test")
    public String paymentTest(HttpSession session, Model model) {
        // 세션에서 예약 정보를 가져와서 사용
        ReservationResponseDto reservationData = (ReservationResponseDto) session.getAttribute("reservedData");
        if (reservationData == null) {
            model.addAttribute("error", "예약 정보를 찾을 수 없습니다.");
            return "payment/paymentFail";
        }

        PaymentInfoDto dto = parseReservationInfo(session);

        // 회원 포인트 조회
        String memberId = dto.getMemberId();
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member == null) {
            model.addAttribute("error", "존재하지 않는 회원입니다.");
            return "payment/paymentFail";
        }

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

        // 예약 정보에서 결제 정보 파싱
        PaymentInfoDto paymentInfo = parseReservationInfo(session);
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

        // paymentInfo.getMoviePrice()는 이미 포인트가 차감된 금액이므로 그대로 사용
        int finalPrice = paymentInfo.getMoviePrice();

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

    @GetMapping("/movie/payment/kakao-pay-successkakao-pay-success")
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
        
        // 예약 정보에서 결제 정보 파싱
        PaymentInfoDto paymentInfo = parseReservationInfo(session);

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

            // MovieOrder에 주문 정보 저장
            movieOrderService.saveOrder(
                paymentInfo, 
                orderId, 
                responseDto.getTid(), // transactionId로 사용
                "KAKAO_PAY", 
                usePoint, 
                orderId, // partnerOrderId
                pg_token
            );

            // 세션 정리
            session.removeAttribute("kakao_tid");
            session.removeAttribute("kakao_orderId");
            session.removeAttribute("kakao_memberId");
            session.removeAttribute("kakao_usePoint");
            
            // 성공 페이지에서 주문 정보를 표시하기 위해 주문번호를 세션에 저장
            session.setAttribute("lastOrderNumber", orderId);

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

        PaymentInfoDto dto = parseReservationInfo(null); // session 파라미터 제거

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
                                     HttpSession session,
                                     Model model) {
        try {
            // 예약 정보에서 결제 정보 파싱
            PaymentInfoDto paymentInfo = parseReservationInfo(session);
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

            // paymentInfo.getMoviePrice()는 이미 포인트가 차감된 금액이므로 그대로 사용
            int finalPrice = paymentInfo.getMoviePrice();

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

                // MovieOrder에 주문 정보 저장
                movieOrderService.saveOrder(
                    paymentInfo, 
                    orderId, 
                    responseDto.getTransactionId(), 
                    "CARD", 
                    usePoint, 
                    null, // partnerOrderId (카드결제는 없음)
                    null  // pgToken (카드결제는 없음)
                );

                // 성공 페이지에서 주문 정보를 표시하기 위해 주문번호를 세션에 저장
                session.setAttribute("lastOrderNumber", orderId);

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
            HttpSession session,
            Model model) {
        try {
            // 예약 정보에서 결제 정보 파싱
            PaymentInfoDto paymentInfo = parseReservationInfo(session);
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

            // MovieOrder에 주문 정보 저장
            String orderId = "ORDER_" + System.currentTimeMillis();
            movieOrderService.saveOrder(
                paymentInfo, 
                orderId, 
                "POINT_" + System.currentTimeMillis(), // 포인트 결제용 거래번호
                "POINT", 
                usePoint, 
                null, // partnerOrderId (포인트결제는 없음)
                null  // pgToken (포인트결제는 없음)
            );

            // 성공 페이지에서 주문 정보를 표시하기 위해 주문번호를 세션에 저장
            session.setAttribute("lastOrderNumber", orderId);

            log.info("포인트 결제 성공 - memberId: {}, usePoint: {}", memberId, usePoint);
            return "redirect:/movie/payment/success";

        } catch (Exception e) {
            log.error("포인트 결제 처리 중 오류", e);
            model.addAttribute("error", "결제 처리 중 오류가 발생했습니다.");
            return "payment/paymentFail";
        }
    }

    @GetMapping("/movie/payment/success")
    public String paymentSuccess(@SessionAttribute(name = "lastOrderNumber", required = false) String lastOrderNumber,
                                Model model) {
        if (lastOrderNumber != null) {
            try {
                MovieOrderDto orderInfo = movieOrderService.getOrderByOrderNumber(lastOrderNumber);
                model.addAttribute("paymentInfo", orderInfo);
                model.addAttribute("message", "결제가 성공적으로 완료되었습니다!");
            } catch (Exception e) {
                log.warn("주문 정보 조회 실패: {}", e.getMessage());
            }
        }
        return "payment/paymentSuccess";
    }
}