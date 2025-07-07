package com.movie.controller;

import com.movie.dto.PaymentInfoDto;
import com.movie.dto.KakaoPayReadyRequestDto;
import com.movie.dto.KakaoPayReadyResponseDto;
import com.movie.dto.KakaoPayApprovalRequestDto;
import com.movie.dto.KakaoPayApprovalResponseDto;
import com.movie.dto.CardPaymentRequestDto;
import com.movie.dto.CardPaymentResponseDto;
import com.movie.repository.MemberRepository;
import com.movie.entity.Member;
import com.movie.service.KakaoPayService;
import com.movie.service.CardPaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class PaymentController {

    private final MemberRepository memberRepository;
    private final KakaoPayService kakaoPayService;
    private final CardPaymentService cardPaymentService;

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

    @PostMapping("/movie/payment/kakao-pay-ready")
    public String kakaoPayReady(@RequestParam String memberId,
                                @RequestParam(defaultValue = "0") int usePoint,
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
        requestDto.setApproval_url("http://localhost:8080/movie/payment/kakao-pay-success?orderId=" + orderId +
                "&memberId=" + memberId + "&movieTitle=" + paymentInfo.getMovieTitle() +
                "&cinemaName=" + paymentInfo.getCinemaName() + "&screenRoomName=" + paymentInfo.getScreenRoomName() +
                "&seat=" + paymentInfo.getSeat() + "&moviePrice=" + paymentInfo.getMoviePrice() + "&usePoint=" + usePoint);
        requestDto.setCancel_url("http://localhost:8080/movie/payment/kakao-pay-cancel");
        requestDto.setFail_url("http://localhost:8080/movie/payment/kakao-pay-fail");

        try {
            KakaoPayReadyResponseDto responseDto = kakaoPayService.kakaoPayReady(requestDto);
            return "redirect:" + responseDto.getNext_redirect_pc_url();
        } catch (Exception e) {
            log.error("카카오페이 준비 요청 실패", e);
            model.addAttribute("error", "결제 준비에 실패했습니다.");
            return "payment/paymentFail";
        }
    }

    @GetMapping("/movie/payment/kakao-pay-success")
    public String kakaoPaySuccess(@RequestParam String pg_token,
                                  @RequestParam String orderId,
                                  @RequestParam String memberId,
                                  @RequestParam int usePoint,
                                  Model model) {

        // member_reserve에서 정보 파싱
        PaymentInfoDto paymentInfo = parseReserveInfo(memberId);

        try {
            // 카카오페이 승인 요청
            KakaoPayApprovalRequestDto requestDto = new KakaoPayApprovalRequestDto();
            requestDto.setTid(""); // 실제로는 세션에서 가져와야 함
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
            member.setPoint(String.valueOf(currentPoint - usePoint));

            // 결제 내역 저장
            String history = String.format("%s,%s,%s,%s,%s,%d원,포인트사용:%d,실결제:%d원,결제수단:카카오페이",
                    java.time.LocalDate.now(),
                    paymentInfo.getCinemaName(),
                    paymentInfo.getCinemaAddress(),
                    paymentInfo.getScreenRoomName(),
                    paymentInfo.getSeat(),
                    paymentInfo.getMovieTitle(),
                    paymentInfo.getMoviePrice(),
                    usePoint,
                    responseDto.getAmount().getTotal()
            );

            String prev = member.getReserve();
            if (prev == null || prev.isEmpty()) {
                member.setReserve(history);
            } else {
                member.setReserve(prev + "\n" + history);
            }

            memberRepository.save(member);

            model.addAttribute("paymentInfo", responseDto);
            return "payment/paymentSuccess";

        } catch (Exception e) {
            log.error("카카오페이 승인 요청 실패", e);
            model.addAttribute("error", "결제 승인에 실패했습니다.");
            return "payment/paymentFail";
        }
    }

    @GetMapping("/movie/payment/kakao-pay-cancel")
    public String kakaoPayCancel(Model model) {
        model.addAttribute("error", "결제가 취소되었습니다.");
        return "payment/paymentFail";
    }

    @GetMapping("/movie/payment/kakao-pay-fail")
    public String kakaoPayFail(Model model) {
        model.addAttribute("error", "결제에 실패했습니다.");
        return "payment/paymentFail";
    }

    @GetMapping("/movie/payment/card")
    public String cardPaymentPage(@RequestParam String memberId,
                                  @RequestParam(defaultValue = "0") int usePoint,
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

        model.addAttribute("paymentInfo", paymentInfo);
        model.addAttribute("usePoint", usePoint);
        model.addAttribute("finalPrice", finalPrice);

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

        // member_reserve에서 정보 파싱
        PaymentInfoDto paymentInfo = parseReserveInfo(memberId);

        try {
            // 주문 ID 생성
            String orderId = "ORDER_" + System.currentTimeMillis();

            // 카드결제 요청 DTO 생성
            CardPaymentRequestDto requestDto = new CardPaymentRequestDto();
            requestDto.setCardNumber(cardNumber.replaceAll("\\s", ""));
            requestDto.setCardExpiry(cardExpiry);
            requestDto.setCardCvc(cardCvc);
            requestDto.setCardHolderName(cardHolderName);
            requestDto.setAmount(paymentInfo.getMoviePrice() - usePoint);
            requestDto.setOrderId(orderId);
            requestDto.setMemberId(memberId);
            requestDto.setMovieTitle(paymentInfo.getMovieTitle());
            requestDto.setCinemaName(paymentInfo.getCinemaName());
            requestDto.setScreenRoomName(paymentInfo.getScreenRoomName());
            requestDto.setSeat(paymentInfo.getSeat());
            requestDto.setUsePoint(usePoint);

            // 카드결제 처리
            CardPaymentResponseDto responseDto = cardPaymentService.processCardPayment(requestDto);

            if (responseDto.isSuccess()) {
                // 결제 성공 시 회원 정보 업데이트
                Member member = memberRepository.findById(memberId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

                int currentPoint = 0;
                try {
                    currentPoint = Integer.parseInt(member.getPoint());
                } catch (Exception e) {
                }

                // 포인트 차감
                member.setPoint(String.valueOf(currentPoint - usePoint));

                // 결제 내역 저장
                String history = String.format("%s,%s,%s,%s,%s,%d원,포인트사용:%d,실결제:%d원,결제수단:신용카드,거래번호:%s",
                        java.time.LocalDate.now(),
                        paymentInfo.getCinemaName(),
                        paymentInfo.getCinemaAddress(),
                        paymentInfo.getScreenRoomName(),
                        paymentInfo.getSeat(),
                        paymentInfo.getMovieTitle(),
                        paymentInfo.getMoviePrice(),
                        usePoint,
                        responseDto.getAmount(),
                        responseDto.getTransactionId()
                );

                String prev = member.getReserve();
                if (prev == null || prev.isEmpty()) {
                    member.setReserve(history);
                } else {
                    member.setReserve(prev + "\n" + history);
                }

                memberRepository.save(member);

                model.addAttribute("paymentInfo", responseDto);
                return "payment/paymentSuccess";

            } else {
                model.addAttribute("error", responseDto.getMessage());
                return "payment/paymentFail";
            }

        } catch (Exception e) {
            log.error("카드결제 처리 중 오류 발생", e);
            model.addAttribute("error", "결제 처리 중 오류가 발생했습니다.");
            return "payment/paymentFail";
        }
    }

    @PostMapping("/movie/payment/complete")
    public String completePayment(
            @RequestParam String memberId,
            @RequestParam(defaultValue = "0") int usePoint,
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

        // 사용 포인트가 결제 금액보다 많으면 결제 금액까지만 사용
        int realUsePoint = Math.min(usePoint, paymentInfo.getMoviePrice());

        // 실제 결제 금액 계산
        int finalPrice = paymentInfo.getMoviePrice() - realUsePoint;

        // 포인트 차감
        member.setPoint(String.valueOf(currentPoint - realUsePoint));

        // 결제 내역 저장
        String history = String.format("%s,%s,%s,%s,%s,%d원,포인트사용:%d,실결제:%d원,결제수단:포인트결제",
                java.time.LocalDate.now(),
                paymentInfo.getCinemaName(),
                paymentInfo.getCinemaAddress(),
                paymentInfo.getScreenRoomName(),
                paymentInfo.getSeat(),
                paymentInfo.getMovieTitle(),
                paymentInfo.getMoviePrice(),
                realUsePoint,
                finalPrice
        );
        String prev = member.getReserve();
        if (prev == null || prev.isEmpty()) {
            member.setReserve(history);
        } else {
            member.setReserve(prev + "\n" + history);
        }
        memberRepository.save(member);

        // 결제 성공 페이지로 리다이렉트
        return "redirect:/movie/payment/success";
    }

    @GetMapping("/movie/payment/success")
    public String paymentSuccess(Model model) {
        model.addAttribute("message", "결제가 성공적으로 완료되었습니다!");
        return "payment/paymentSuccess";
    }

    @GetMapping("/movie/payment/test")
    public String paymentTest(Model model) {
        // test@test.com 계정의 실제 정보 조회
        Member member = memberRepository.findById("test@test.com").orElse(null);
        if (member == null) {
            model.addAttribute("error", "테스트 계정을 찾을 수 없습니다.");
            return "payment/paymentFail";
        }

        PaymentInfoDto dto = parseReserveInfo("test@test.com");

        // test@test.com 계정의 실제 포인트 조회
        int memberPoint = 0;
        if (member.getPoint() != null) {
            try {
                memberPoint = Integer.parseInt(member.getPoint());
            } catch (Exception e) {
                log.warn("포인트 파싱 실패: {}", member.getPoint());
            }
        }

        log.info("테스트 결제 페이지 - 회원: {}, 포인트: {}, 예약정보: {}",
                member.getMemberId(), memberPoint, member.getReserve());

        model.addAttribute("paymentInfo", dto);
        model.addAttribute("memberPoint", memberPoint);
        return "payment/moviePaymentPage";
    }
}
