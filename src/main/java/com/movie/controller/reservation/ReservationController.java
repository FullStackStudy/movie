package com.movie.controller.reservation;

import com.movie.dto.ReservedSeatDto;
import com.movie.dto.ScheduleDto;
import com.movie.dto.SeatDto;
import com.movie.dto.reservation.ReservationDto;
import com.movie.dto.reservation.ReservationRedisResultDto;
import com.movie.dto.reservation.ReservationResponseDto;
import com.movie.entity.ReservedSeat;
import com.movie.entity.ScreenRoom;
import com.movie.entity.Seat;
import com.movie.repository.ReservedSeatRepository;
import com.movie.repository.ScheduleRepository;
import com.movie.repository.ScreenRoomRepository;
import com.movie.repository.SeatRepository;
import com.movie.service.reservation.ReservationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
@RequiredArgsConstructor
@RequestMapping("/reservation")
public class ReservationController {
    private final ReservationService reservationService;
    private final SeatRepository seatRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final ScreenRoomRepository screenRoomRepository;

    //예약페이지 in
    @GetMapping({"/",""})
    public String reserveSet(@AuthenticationPrincipal UserDetails userDetails, Model model){
        System.out.println("들어오니");
        List<ScheduleDto> scheduleDtoList = reservationService.getScheduleInfo();
        model.addAttribute("member_id",userDetails.getUsername());
        model.addAttribute("scheduleList", scheduleDtoList);
        for(ScheduleDto schedule : scheduleDtoList) {
            System.out.println("영화제목" + schedule.getMovieTitle());
            System.out.println("영화관" + schedule.getCinemaName());
            System.out.println("영화스크린"+schedule.getScreenRoomName());
            System.out.println("영화몇관인지"+schedule.getRoomId());
            System.out.println("스케쥴 아이디"+schedule.getScheduleId());

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

        model.addAttribute("member_id", member_id);
        model.addAttribute("movieTitle",movieTitle);
        model.addAttribute("cinemaName",cinemaName);
        model.addAttribute("startTime",startTime);
        model.addAttribute("screenRoomName",screenRoomName);
        model.addAttribute("roomId",roomId);
        model.addAttribute("scheduleId",scheduleId);


       model.addAttribute("seats", seatDtos);
        return "reserve/reservation"; // templates/reservation.html
    }

    //예약하기
    @PostMapping("/reserve")
    public @ResponseBody ResponseEntity<String> reserveSeats(@RequestBody ReservationResponseDto reservationResponseDto, HttpSession session){

        System.out.println("스케쥴 아이디"+reservationResponseDto.getScheduleId());
        System.out.println("d영화제목"+reservationResponseDto.getMovieName());

        ReservationRedisResultDto result = reservationService.reserveRedis(reservationResponseDto); //redis에 저장

        //하나라도 redis 걸려있으면 (두 자리 선택했는데 한자리가 누가 예약중일 때)
        if(!result.getFailList().isEmpty()){
            for(Long seatId : result.getSuccessList()){ //성공해서 레디스에 저장한 걸
                reservationService.releaseSeat(reservationResponseDto.getScheduleId(),seatId, reservationResponseDto.getMemberId()); //redis 삭제
            }
            return new ResponseEntity<String>("이미 선택되있는 좌석입니다. 다시 선택해주세요", HttpStatus.FORBIDDEN);
        }else{
            //예약 -> 결제로 가져가는 정보인데 계속 가져가는거보다 세션 끝나면 없애주는게 좋을거같아 session에 저장
            System.out.println("reDto member_id" + reservationResponseDto.getMemberId());
            session.setAttribute("reservedData", reservationResponseDto); //결제페이지로 갈 예약정보 session에 저장
            return new ResponseEntity<String>("결제로 넘어갑니다.",HttpStatus.OK);
        }
    }

    //결제 페이지 in
    @GetMapping("/pay")
    public String payReservation(HttpSession session, Model model){//redis는 예약하기 누르부터 저장됨
        ReservationResponseDto reservationResponseDto = (ReservationResponseDto) session.getAttribute("reservedData");
        model.addAttribute("reservedData", reservationResponseDto);
        return "/reserve/reservePay";
    }

    //결제
    @PostMapping("/pay")
    public String payReservation(HttpSession session,@ModelAttribute ReservationDto reservationDto, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        ReservationDto reserve = (ReservationDto) session.getAttribute("reservedData");

        boolean failPay = true; //결제 아직 모르니까 성공으로 가게 해놓음
        if (!failPay) { //결제 취소하면, 결제 화면 나가면 그냥 결제 화면 보여주는거잖아? //이건 아예 예약확정 전에 예약취소하는거지 -> 수정하기
            for (Long seatId : reserve.getSeatId()) { //좌석
                reservationService.releaseSeat(reserve.getScheduleId(), seatId, reserve.getMemberId());
                model.addAttribute("errorMessage", "결제 실패");
            }
            return "redirect:/reservation/pay";
        } else { //결제 성공
            boolean success = reservationService.saveReservation(userDetails ,reserve);
            if (success) {
                return "redirect:/reservation/checkReservation";
            }else return "redirect:/reservation/pay";
        }
    }

    @GetMapping("/checkReservation")
    public String checkReservation(){
        return "/reserve/successReservation";
    }

    @PostMapping("/cancel") //좌석 다시 선택 했을때 꺼 나중에해라ㅠ
    public @ResponseBody ResponseEntity<String> backSeat(HttpSession session,Model model, @RequestParam("type") String type){
        System.out.println("typoe="+ type);
        if(type.equals("back")){
            ReservationDto reservationDto = (ReservationDto) session.getAttribute("reservedData");
            System.out.println("세션 정보"+ reservationDto.getScheduleId());


            if(reservationDto !=null){ // 저장된 세션 정보가 있는것
                System.out.println("여기 타겠지 있잔하");
                model.addAttribute("reservedData" , reservationDto);
            }else{
                model.addAttribute("reservedData", new ReservationDto());
            }
            return new ResponseEntity<String>("좌석 선택으로 갑니다.", HttpStatus.OK);

        }else{ // 취소
            return new ResponseEntity<String>("예약을 취소하고 메인으로 갑니다.", HttpStatus.OK);
        }
    }
}



