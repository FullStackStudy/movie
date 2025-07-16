package com.movie.controller.reservation;

import com.movie.dto.reservation.ReservedSeatDto;
import com.movie.dto.cinema.ScheduleDto;
import com.movie.dto.seat.SeatDto;
import com.movie.dto.reservation.ReservationDto;
import com.movie.dto.reservation.ReservationRedisResultDto;
import com.movie.dto.reservation.ReservationResponseDto;
import com.movie.dto.reservation.SeatStatusMessageDto;
import com.movie.entity.cinema.Seat;
import com.movie.repository.reservation.ReservedSeatRepository;
import com.movie.repository.cinema.ScreenRoomRepository;
import com.movie.repository.seat.SeatRepository;
import com.movie.service.cinema.ScheduleService;
import com.movie.service.seat.SeatService;
import com.movie.service.reservation.ReservationService;
import com.movie.service.reservation.SeatNotificationService;
import com.movie.repository.member.MemberRepository;
import com.movie.entity.member.Member;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Controller
@RequiredArgsConstructor
@RequestMapping("/reservation")
public class ReservationController {
    private final ReservationService reservationService;
    private final SeatRepository seatRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final ScreenRoomRepository screenRoomRepository;
    private final SeatNotificationService seatNotificationService;
    private final RedisTemplate redisTemplate;
    private final SeatService seatService;
    private final ScheduleService scheduleService;
    private final MemberRepository memberRepository;
/*    //예약페이지 in
    @GetMapping({"/",""})
    public String reserveSet(@AuthenticationPrincipal UserDetails userDetails, Model model){
        System.out.println("들어오니");
        List<ScheduleDto> scheduleDtoList = reservationService.getScheduleInfo();
        model.addAttribute("member_id",userDetails.getUsername());
        model.addAttribute("scheduleList", scheduleDtoList);
        model.addAttribute("nextButtonType", "first");
        for(ScheduleDto schedule : scheduleDtoList) {
            System.out.println("영화제목" + schedule.getMovieTitle());
            System.out.println("영화관" + schedule.getCinemaNm());
            System.out.println("영화스크린"+schedule.getRoomId());
            System.out.println("스케쥴 아이디"+schedule.getId());

        }
        return "reserve/reserveSet";
    }
    //좌석선택페이지 in
    @GetMapping("/reserveSeat")
    public String reserveSeats(
            @RequestParam String member_id,
            @RequestParam String movieTitle,
            @RequestParam String cinemaName,
            @RequestParam String startTime,
            @RequestParam String screenRoomName,
            @RequestParam("roomId") Long roomId,
            @RequestParam Long scheduleId,
            @AuthenticationPrincipal UserDetails userDetails, Model model){

        System.out.println("roomId: "+ roomId);
        List<SeatDto> seatDtos= reservationService.getSeats(roomId); //룸아이디로 (pk) 저장된 자리 다가져옴
        List<ReservedSeatDto> reservedSeatDtos = reservationService.getReservedSeats(scheduleId);

        //redis에서 hold중인 좌석
        //List<Long> holdingSeatIds = reservationService.getHoldingSeats(scheduleId);

        for(ReservedSeatDto reservedSeatDto: reservedSeatDtos){
            System.out.println("----------------------------------------------");
            System.out.println("예약된 좌석 id: "+ reservedSeatDto.getSeatId());
            System.out.println("누가예약함:" + reservedSeatDto.getMemberId());
            System.out.println("-=------------------------------------------");
        }
        model.addAttribute("member_id", member_id);
        model.addAttribute("movieTitle",movieTitle);
        model.addAttribute("cinemaName",cinemaName);
        model.addAttribute("startTime",startTime);
        model.addAttribute("screenRoomName",screenRoomName);
        model.addAttribute("roomId",roomId);
        model.addAttribute("scheduleId",scheduleId);

        model.addAttribute("nextButtonType", "second");

        model.addAttribute("seats", seatDtos);
        model.addAttribute("reservedSeats", reservedSeatDtos);
        model.addAttribute("reservedSeatList",reservedSeatDtos.stream().map(ReservedSeatDto::getSeatId).collect(Collectors.toList()));

        //model.addAttribute("holdingSeatList", holdingSeatIds);
        return "seat/seatSelect"; // templates/reservation.html
    }*/

    //예약하기
    @PostMapping("/reserve")
    public @ResponseBody ResponseEntity<String> reserveSeats(
            @RequestParam Long scheduleId,
            @RequestBody List<SeatDto> seatDtos, HttpSession session,@AuthenticationPrincipal UserDetails userDetails){


        List<Long> reservedSeatNow  = new ArrayList<>();

        for(SeatDto seat: seatDtos){
            //row col 합쳐서 roomId랑 같이 보내서 seatId 가져옴..
            Long seatId =seatService.getSeatId(seat.getSeatRow(),seat.getSeatColumn(),seat.getScreenRoomId());
            reservedSeatNow.add(seatId);
        }

        //그 전에 예약된 좌석들
        List<ReservedSeatDto> alreadyReserved = reservationService.getReservedSeats(scheduleId);
        Set<Long> alreadyReservedIds = alreadyReserved.stream()
                .map(ReservedSeatDto::getSeatId)
                .collect(Collectors.toSet());


        //좌석 앞에서 못 선택하게 하긴했지만 한번 더 중복 체크한다.
        for(Long seatNow : reservedSeatNow){
            if(alreadyReservedIds.contains(seatNow)){
                throw new IllegalArgumentException("이미 예약된 좌석이 포함되어 있습니다."+seatNow);
            }
        }

        ReservationRedisResultDto result =
                reservationService.reserveRedis(reservedSeatNow, userDetails.getUsername(),scheduleId); //redis에 저장

        //하나라도 redis 걸려있으면 (두 자리 선택했는데 한자리가 누가 예약중일 때)
        if(!result.getFailList().isEmpty()){
            for(Long seatId : result.getSuccessList()){ //성공해서 레디스에 저장한 걸
                reservationService.releaseSeat(scheduleId,seatId, userDetails.getUsername()); //redis 삭제
            }
            return new ResponseEntity<String>("이미 선택되있는 좌석입니다. 다시 선택해주세요", HttpStatus.FORBIDDEN);
        }else{
            //예약 -> 결제로 가져가는 정보인데 계속 가져가는거보다 세션 끝나면 없애주는게 좋을거같아 session에 저장
            System.out.println("reDto member_id" + userDetails.getUsername());
            ReservationResponseDto reservationResponseDto =
                    reservationService.getReservationResponseDto(scheduleId,seatDtos, userDetails.getUsername());
            session.setAttribute("scheduleId", scheduleId);
            System.out.println(reservedSeatNow);
            session.setAttribute("seatId", reservedSeatNow);
            session.setAttribute("reservedData", reservationResponseDto); //결제페이지로 갈 예약정보 session에 저장
            return new ResponseEntity<String>("결제로 넘어갑니다.",HttpStatus.OK);
        }
    }

    //결제 페이지 in
    @GetMapping("/pay")
    public String payReservation(HttpSession session, Model model, @AuthenticationPrincipal UserDetails userDetails){//redis는 예약하기 누르부터 저장됨
        ReservationResponseDto reservationResponseDto = (ReservationResponseDto) session.getAttribute("reservedData");
        List<Long> seatId = (List<Long>)session.getAttribute("seatId");
        Long scheduleId = (Long)session.getAttribute("scheduleId");
        String moviePoster = scheduleService.getPosterUrl(scheduleId).getMoviePoster();
        
        // 회원 포인트 정보 조회
        int memberPoint = 0;
        try {
            String memberId = userDetails.getUsername();
            Member member = memberRepository.findById(memberId).orElse(null);
            if (member != null && member.getPoint() != null) {
                memberPoint = Integer.parseInt(member.getPoint());
            }
        } catch (Exception e) {
            // 포인트 조회 실패 시 0으로 설정
        }
        
        model.addAttribute("moviePoster", moviePoster);
        model.addAttribute("seatId",seatId);
        model.addAttribute("scheduleId",scheduleId);
        model.addAttribute("reservedData", reservationResponseDto);
        model.addAttribute("memberPoint", memberPoint);
        //Long key = reservationService.getTtl(reservationService.makeKey(reservationResponseDto.getScheduleId(), reservationResponseDto.getSeatId().getFirst()));
        //seatNotificationService.notifyPayHold(reservationResponseDto.getScheduleId(), reservationResponseDto.getSeatId(),"hold", key);
        return "reserve/reservePay";
    }

    //결제
    @PostMapping("/pay")
    public String payReservation(HttpSession session,
                                 @RequestParam("seat_id")List<Long> seatId,
                                 @RequestParam("schedule_id") Long scheduleId,
                                 @RequestParam(defaultValue = "0") int usePoint,
                                 @RequestParam int finalPrice,
                                 Model model,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        ReservationResponseDto reserve = (ReservationResponseDto) session.getAttribute("reservedData");
        System.out.println("결제 페이지로 이동 - 예약 정보: " + reserve.toString());
        reserve.setSeatId(seatId);
        reserve.setScheduleId(scheduleId);

        // 최종 금액 설정 (reservePay에서 계산된 값 사용)
        reserve.setPrice(finalPrice);

        // 세션에 업데이트된 예약 정보 저장
        session.setAttribute("reservedData", reserve);
        session.setAttribute("usePoint", usePoint);
        
        // movie/payment 페이지로 리다이렉트
        return "redirect:/movie/payment?memberId=" + userDetails.getUsername ();
    }

    @PostMapping("/back")
    public @ResponseBody ResponseEntity<String> backSeat(HttpSession session,Model model, @RequestParam("type") String type){
        ReservationDto reservationDto = (ReservationDto) session.getAttribute("reservedData");

        if(type.equals("back")){
            System.out.println("세션 정보"+ reservationDto.getScheduleId());

            if(reservationDto !=null){ // 저장된 세션 정보가 있는것
                System.out.println("여기 타겠지 있잔하");
                model.addAttribute("reservedData" , reservationDto);
            }else{
                model.addAttribute("reservedData", new ReservationDto());
            }
            return new ResponseEntity<String>("좌석 선택으로 갑니다.", HttpStatus.OK);

        }else{ // 취소
            //메인으로 갈 때 아예 예약취소니까 redis 삭제해줌 (user는 releaseSeat에서 해줌)
            for(Long seatId : reservationDto.getSeatId()) {
                reservationService.releaseSeat(reservationDto.getScheduleId(), seatId, reservationDto.getMemberId());
            }
            return new ResponseEntity<String>("예약을 취소하고 메인으로 갑니다.", HttpStatus.OK);
        }
    }

    //에약목록 가져오기 취소하기위해서 임의로 만든거
    @GetMapping("/list")
    public String reservationList(@AuthenticationPrincipal UserDetails userDetails, Model model){
        //내꺼 예약목록 가져오기
        List<ReservationResponseDto> reservationDtoList = reservationService.getAllReservation(userDetails.getUsername());
        for(ReservationResponseDto responseDto : reservationDtoList){
            LocalDateTime movieDateTime = LocalDateTime.of(responseDto.getShowDate(), responseDto.getStartTime());
            responseDto.setMovieDateTime(movieDateTime);
        }
        model.addAttribute("myReservationList", reservationDtoList);
        return "/reserve/reservationList";
    }

    //예약취소하기
    @PostMapping("/cancel")
    public ResponseEntity<String> cancelReservation(
            @RequestBody Map<String, Object> data,
            @AuthenticationPrincipal UserDetails userDetails) throws AccessDeniedException {
        Long reservationId = Long.parseLong(data.get("reservationId").toString());
        System.out.println("취소할 에약번호: "+ reservationId);
        //예약한 유저아이디 가져옴 검사

        boolean cancelReservation = reservationService.cancelReservation(reservationId, userDetails);
        if(cancelReservation){
            return new ResponseEntity<String>("예약을 취소했습니다.", HttpStatus.OK);
        }
        return new ResponseEntity<String>("유저가 다릅니다.",HttpStatus.FORBIDDEN);
    }
/////////////////////////////////////tlqj
@PostMapping("/triggerHold")
public ResponseEntity<String> triggerWebSocket(@RequestBody SeatStatusMessageDto dto) {
    // 기존 notifyPayHold() 재사용
    String key = reservationService.makeKey(dto.getScheduleId(), dto.getSeatId().get(0));
    System.out.println("key: [" + key + "]");
    Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
    System.out.println("ttl: " + ttl);
    seatNotificationService.notifyPayHold(dto.getScheduleId(), dto.getSeatId(), "hold", ttl);
    return ResponseEntity.ok("WebSocket 알림 전송 완료");
}

}



