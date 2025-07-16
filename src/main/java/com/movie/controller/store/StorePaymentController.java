package com.movie.controller.store;

import com.movie.constant.OrderStatus;
import com.movie.dto.store.CartOrderRequestDto;
import com.movie.dto.store.StoreOrderItemDto;
import com.movie.dto.store.StorePaymentInfoDto;
import com.movie.entity.cinema.Cinema;
import com.movie.entity.member.Member;
import com.movie.entity.store.Item;
import com.movie.entity.store.StoreOrderItem;
import com.movie.entity.store.StorePaymentInfo;
import com.movie.repository.cinema.CinemaRepository;
import com.movie.repository.member.MemberRepository;
import com.movie.repository.store.ItemRepository;
import com.movie.repository.store.StoreMemberPaymentInfoRepository;
import com.movie.repository.store.StoreOrderItemRepository;
import com.movie.repository.store.StorePaymentInfoRepository;
import com.movie.service.store.SmsService;
import com.movie.service.store.StorePaymentInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StorePaymentController {

    private final MemberRepository memberRepository;
    private final StorePaymentInfoService storePaymentInfoService;
    private final ItemRepository itemRepository;
    private final SmsService smsService;
    private final StoreOrderItemRepository storeOrderItemRepository;
    private final CinemaRepository cinemaRepository;
    private final StorePaymentInfoRepository storePaymentInfoRepository;
    private final StoreMemberPaymentInfoRepository storeMemberPaymentInfoRepository;

    @PostMapping("/store/cart/payment")
    public ResponseEntity<String> processCartOrder(@RequestBody CartOrderRequestDto requestDto,
                                                   Principal principal) {
        // 결제 정보 생성
        StorePaymentInfoDto paymentInfo = storePaymentInfoService.createCartPaymentInfo(requestDto, principal.getName());

        // 프론트엔드에서 리다이렉션용 URL 반환
        String redirectUrl = "/store/cart/payment/view?memberId=" + principal.getName();
        return ResponseEntity.ok(redirectUrl); // JS에서 location.href 등으로 처리
    }

    @GetMapping("/store/cart/payment/view")
    public String showCartPaymentPage(@RequestParam String memberId, Model model) {
        // 회원 정보 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 결제 정보 조회
        StorePaymentInfoDto storePaymentInfo = storePaymentInfoService.getCartPaymentInfo(memberId);

        model.addAttribute("storePaymentInfo", storePaymentInfo);
        model.addAttribute("member", member);
        return "payment/storeCartPaymentPage";
    }

    // 일반 스토어 주문 결제용 페이지
    @PostMapping("/store/payment")
    public String storePaymentPage(
            @RequestParam String memberId,
            @RequestParam Long itemId,
            @RequestParam String pickupTime,
            @RequestParam int quantity,
            @RequestParam String cinemaName,
            Model model
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음"));
        Cinema cinema = cinemaRepository.findByName(cinemaName)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 영화관입니다."));

        LocalTime time = LocalTime.parse(pickupTime);

        // DTO 구성
        StoreOrderItemDto dto = new StoreOrderItemDto(item,cinemaName);
        dto.setQuantity(quantity);
        dto.setPickupTime(time.toString());
        dto.setCinemaName(cinema.getName());

        StorePaymentInfoDto storePaymentInfo = new StorePaymentInfoDto(memberId, List.of(dto));

        model.addAttribute("storePaymentInfo", storePaymentInfo);
        model.addAttribute("member", member);

        return "payment/storePaymentPage";
    }

    @PostMapping("/store/payment/verify")
    public String verifyAndSavePayment(
            @RequestParam String impUid,
            @RequestParam String merchantUid,
            @RequestParam String memberId,
            @ModelAttribute StorePaymentInfoDto paymentInfoDto,
            Model model
    ) {
        boolean result = storePaymentInfoService.verifyAndSavePayment(
                impUid, merchantUid, memberId, paymentInfoDto.getItems());

        if (!result) {
            // 실패 시 자동 결제 취소
            storePaymentInfoService.cancelPaymentByImpUid(impUid, "검증 실패로 인한 자동 취소");
        }

        model.addAttribute("result", result ? "success" : "fail");
        return result ? "payment/storePaymentSuccess" : "payment/storePaymentFail";
    }
        @GetMapping("/orders")
    public String showOrdersPage(Model model) {
        List<List<StoreOrderItem>> groupedOrders = storePaymentInfoService.getGroupedOrders();

        // 지점 기준으로 2차 그룹핑
        Map<String, List<List<StoreOrderItem>>> cinemaGroupedOrders = groupedOrders.stream()
                .collect(Collectors.groupingBy(group -> group.get(0).getCinemaName()));

        model.addAttribute("cinemaGroupedOrders", cinemaGroupedOrders);
        return "store/orders";
    }

    @PostMapping("/store/order/ready")
    public String markOrderReady(@RequestParam("orderId") Long orderId) {
        StoreOrderItem order = storeOrderItemRepository.findById(orderId).orElseThrow();
        Member member = order.getStorePaymentInfo().getMember();
        String toPhone = member.getPhone();

        String message = member.getNickname() + "님, 주문하신 상품이 픽업 준비되었습니다.";
        smsService.sendSMS(toPhone, message);

        storeOrderItemRepository.delete(order);

        return "redirect:/orders";
    }
    @PostMapping("/store/payment/cancel")
    public String cancelPayment(
            @RequestParam String impUid,
            @RequestParam(required = false) String merchantUid,
            Model model
    ) {
        boolean result = storePaymentInfoService.cancelPaymentByImpUid(impUid, "사용자 요청 취소");

        if (result) {
            StorePaymentInfo paymentInfo = storePaymentInfoRepository.findByImpUid(impUid)
                    .orElseThrow(() -> new RuntimeException("결제 정보를 찾을 수 없습니다."));
            paymentInfo.setStatus(OrderStatus.CANCEL);
            storePaymentInfoRepository.save(paymentInfo);

            storeMemberPaymentInfoRepository.findByMerchantUid(paymentInfo.getMerchantUid()).ifPresent(memberInfo -> {
                memberInfo.setStatus(OrderStatus.CANCEL);
                storeMemberPaymentInfoRepository.save(memberInfo);
            });
        }

        model.addAttribute("result", result ? "success" : "fail");
        return result ? "payment/cancelSuccess" : "payment/cancelFail";
    }
}
