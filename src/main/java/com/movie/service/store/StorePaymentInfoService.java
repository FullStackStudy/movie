package com.movie.service.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.movie.constant.OrderStatus;
import com.movie.dto.store.CartOrderDto;
import com.movie.dto.store.CartOrderRequestDto;
import com.movie.dto.store.StoreOrderItemDto;
import com.movie.dto.store.StorePaymentInfoDto;
import com.movie.entity.member.Member;
import com.movie.entity.store.CartItem;
import com.movie.entity.store.StoreOrderItem;
import com.movie.entity.store.StorePaymentInfo;
import com.movie.repository.member.MemberRepository;
import com.movie.repository.store.CartItemRepository;
import com.movie.repository.store.ItemRepository;
import com.movie.repository.store.StoreOrderItemRepository;
import com.movie.repository.store.StorePaymentInfoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorePaymentInfoService {

    private final StoreOrderItemRepository storeOrderItemRepository;
    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;
    private final StorePaymentInfoRepository storePaymentInfoRepository;
    private final SmsService smsService;
    private final StoreMemberPaymentInfoService storeMemberPaymentInfoService;

    //장바구니 전체 결제 정보 구성
    public StorePaymentInfoDto getCartPaymentInfo(String memberId) {
        log.info("장바구니 결제 정보 생성 시작 - memberId: {}", memberId);

        List<CartItem> cartItems = cartItemRepository.findByCart_Member_MemberIdAndIsCartTrue(memberId);

        if (cartItems == null || cartItems.isEmpty()) {
            log.warn("장바구니가 비어 있음 - memberId: {}", memberId);
            return new StorePaymentInfoDto(memberId, Collections.emptyList());
        }

        List<StoreOrderItemDto> itemDtos = cartItems.stream()
                .map(this::toStoreOrderItemDto)
                .collect(Collectors.toList());

        return new StorePaymentInfoDto(memberId, itemDtos);
    }
    //선택된 장바구니 아이템으로 결제 정보 생성
    @Transactional
    public StorePaymentInfoDto createCartPaymentInfo(CartOrderRequestDto requestDto, String memberId) {
        List<Long> cartItemIds = requestDto.getCartItems().stream()
                .map(CartOrderDto::getCartItemId)
                .toList();

        List<CartItem> selectedCartItems = cartItemRepository.findByIdIn(cartItemIds);

        if (selectedCartItems == null || selectedCartItems.isEmpty()) {
            log.warn("선택된 장바구니 아이템이 없음 - memberId: {}, 선택 ID들: {}", memberId, cartItemIds);
            return new StorePaymentInfoDto(memberId, Collections.emptyList());
        }

        List<StoreOrderItemDto> itemDtos = selectedCartItems.stream()
                .map(this::toStoreOrderItemDto)
                .collect(Collectors.toList());

        return new StorePaymentInfoDto(memberId, itemDtos);
    }

    private StoreOrderItemDto toStoreOrderItemDto(CartItem cartItem) {
        StoreOrderItemDto dto = new StoreOrderItemDto(cartItem.getItem(),cartItem.getCinemaName());
        dto.setQuantity(cartItem.getCount());
        dto.setPickupTime(cartItem.getPickupTime().toString());
        dto.setCartItemId(cartItem.getId());
        dto.setCinemaName(cartItem.getCinemaName());

        return dto;
    }
    public List<List<StoreOrderItem>> getGroupedOrders() {
        List<StoreOrderItem> orders = storeOrderItemRepository.findAllWithPaymentInfo();

        // 1. 그룹핑: 지점 + 픽업시간 + 주문번호 기준
        Map<String, List<StoreOrderItem>> groupedMap = orders.stream()
                .collect(Collectors.groupingBy(order ->
                        order.getCinemaName() + "|" +
                                order.getPickupTime() + "|" +
                                order.getStorePaymentInfo().getMerchantUid()
                ));

        // 2. 그룹핑 결과 -> List로 변환
        List<List<StoreOrderItem>> groupedOrders = new ArrayList<>(groupedMap.values());

        // 3. 그룹 정렬 (픽업 시간 오름차순)
        groupedOrders.sort(Comparator.comparing(group -> group.get(0).getPickupTime()));

        return groupedOrders;
    }
    // 이그니스 결제 후 포트원 검증 및 저장
    @Transactional
    public boolean verifyAndSavePayment(String impUid, String merchantUid, String memberId, List<StoreOrderItemDto> itemsDto) {
        try {
            // 1. 아임포트에서 결제 정보 조회
            String accessToken = requestAccessToken();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.iamport.kr/payments/" + impUid))
                    .header("Authorization", accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());

            boolean success = root.path("response").path("status").asText().equals("paid");
            int paidAmount = root.path("response").path("amount").asInt();

            if (!success) {
                log.warn("결제 상태가 'paid'가 아닙니다. impUid: {}", impUid);
                return false;
            }

            // 2. itemsDto 유효성 검사
            if (itemsDto == null || itemsDto.isEmpty()) {
                log.error("itemsDto가 비어 있습니다.");
                return false;
            }

            // 3. 결제 금액 확인
            int expectedAmount = itemsDto.stream()
                    .mapToInt(i -> i.getPrice() * i.getQuantity())
                    .sum();

            if (Math.abs(expectedAmount - paidAmount) > 10) {
                log.error("결제 금액 불일치: expected {}, actual {}", expectedAmount, paidAmount);
                return false;
            }

            // 4. 회원 조회
            Member member = memberRepository.findByMemberId(memberId);
            if (member == null) {
                log.error("회원 정보 없음: {}", memberId);
                return false;
            }

            // 5. 결제 성공 정보 저장
            StorePaymentInfo paymentInfo = new StorePaymentInfo();
            paymentInfo.setImpUid(impUid);
            paymentInfo.setMerchantUid(merchantUid);
            paymentInfo.setAmount(paidAmount);
            paymentInfo.setStatus(OrderStatus.ORDER);
            paymentInfo.setIsCart(false);
            paymentInfo.setMember(member);

            // 6. 주문 항목 저장
            List<StoreOrderItem> orderItems = itemsDto.stream().map(dto -> {
                StoreOrderItem item = new StoreOrderItem();
                item.setItemNm(dto.getItemNm());
                item.setItemComposition(dto.getItemComposition());
                item.setQuantity(dto.getQuantity());
                item.setPrice(dto.getPrice());
                item.setPickupTime(LocalTime.parse(dto.getPickupTime()));
                item.setCinemaName(dto.getCinemaName());
                item.setMemberId(memberId);
                item.setCart(false);
                item.setStorePaymentInfo(paymentInfo);
                return item;
            }).toList();

            paymentInfo.setItems(orderItems);

            storePaymentInfoRepository.save(paymentInfo);
            storeMemberPaymentInfoService.saveMemberOrderHistory(paymentInfo);

            itemsDto.forEach(dto -> {
                cartItemRepository.deleteByItem_ItemNmAndItem_ItemCompositionAndCinemaNameAndCart_Member_MemberIdAndIsCartTrue(
                        dto.getItemNm(), dto.getItemComposition(), dto.getCinemaName(), memberId
                );
            });

            return true;

        } catch (Exception e) {
            log.error("결제 저장 중 오류 발생: {}", e.getMessage(), e);
            return false;
        }
    }

    public String markCartItemsWithPaymentToken(List<Long> cartItemIds, String paymentToken) {
        for (Long cartItemId : cartItemIds) {
            CartItem cartItem = cartItemRepository.findById(cartItemId)
                    .orElseThrow(() -> new RuntimeException("장바구니 항목 없음: " + cartItemId));

            cartItem.setPaymentToken(paymentToken);
            cartItemRepository.save(cartItem);
        }
        return paymentToken;
    }
    private String requestAccessToken() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String body = "imp_key=7251445755405710&imp_secret=uZbHJBrDF307TXxASqqljRkXokcOlmelhlbTUGlx1zQ87haLvH5i1YhOxHmQM2wGVGuNbdQ2rJdgREyx";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.iamport.kr/users/getToken"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(response.body());

        return jsonNode.path("response").path("access_token").asText();
    }

@Transactional
public boolean cancelPaymentByImpUid(String impUid, String reason) {
    try {
        String accessToken = requestAccessToken();

        Map<String, Object> body = new HashMap<>();
        body.put("imp_uid", impUid);
        body.put("reason", reason);

        ObjectMapper mapper = new ObjectMapper();
        String jsonBody = mapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.iamport.kr/payments/cancel"))
                .header("Authorization", accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = mapper.readTree(response.body());
        String status = root.path("response").path("status").asText();

        if ("cancelled".equalsIgnoreCase(status)) {
            // ✅ DB 상태도 변경
            Optional<StorePaymentInfo> paymentOpt = storePaymentInfoRepository.findByMerchantUid(impUid);
            if (paymentOpt.isPresent()) {
                StorePaymentInfo payment = paymentOpt.get();
                payment.setStatus(OrderStatus.CANCEL);  // OrderStatus.CANCEL 로 변경
                storePaymentInfoRepository.save(payment);
            } else {
                log.warn("impUid {} 에 해당하는 주문 정보를 찾을 수 없습니다.", impUid);
            }

            return true;
        }

        return false;

    } catch (Exception e) {
        log.error("결제 취소 실패: {}", e.getMessage(), e);
        return false;
    }
}
}
