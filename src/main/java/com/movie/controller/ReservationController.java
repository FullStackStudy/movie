package com.movie.controller;

import com.movie.dto.ReservationDto;
import com.movie.dto.ReservationRedisResultDto;
import com.movie.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequiredArgsConstructor
@RequestMapping("/reservation")
public class ReservationController {
    private final ReservationService reservationService;

    //예약페이지 in
    @GetMapping
    public String reserveSeats() {
       // model.addAttribute("reserveData", new ReservationDto());
        return "reserve/reservation"; // templates/reservation.html
    }

    //예약하기
    @PostMapping("/reserve")
    public @ResponseBody ResponseEntity<String> reserveSeats(@RequestBody ReservationDto reservationDto, HttpSession session){
        System.out.println("자리"+reservationDto.getSeat_id());

        ReservationRedisResultDto result = reservationService.reserveRedis(reservationDto); //redis에 저장

        //하나라도 redis 걸려있으면 (두 자리 선택했는데 한자리가 누가 예약중일 때)
        if(!result.getFailList().isEmpty()){
            for(String seatId : result.getSuccessList()){ //성공해서 레디스에 저장한 걸
                reservationService.releaseSeat(reservationDto.getMovie_id(),seatId, reservationDto.getUser_id()); //redis 삭제
            }
            return new ResponseEntity<String>("이미 선택되있는 좌석입니다. 다시 선택해주세요", HttpStatus.FORBIDDEN);
        }else{
            //예약 -> 결제로 가져가는 정보인데 계속 가져가는거보다 세션 끝나면 없애주는게 좋을거같아 session에 저장
            session.setAttribute("reservedData", reservationDto); //결제페이지로 갈 예약정보 session에 저장
            return new ResponseEntity<String>("결제로 넘어갑니다.",HttpStatus.OK);
        }

    }

    //결제 페이지 in
    @GetMapping("/pay")
    public String payReservation(HttpSession session, Model model){//redis는 예약하기 누르부터 저장됨
        ReservationDto reservationDto = (ReservationDto) session.getAttribute("reservedData");
        model.addAttribute("reservedData", reservationDto);
        return "/reserve/reservePay";
    }

    //결제 (지금은 성공이라 치고 디비 저장함 redis test르 위해)
    @PostMapping("/pay")
    public String payReservation(@ModelAttribute ReservationDto reservationDto, Model model){
        System.out.println(reservationDto.getMovie_id()+"영화정보");
        System.out.println(reservationDto.getSeat_id()+"좌석정보");
        boolean failPay = true; //결제 아직 모르니까 성공으로 가게 해놓음
        if(failPay){ //결제 취소하면, 결제 화면 나가면 그냥 결제 화면 보여주는거잖아? //이건 아예 예약확정 전에 예약취소하는거지
            for(String seatId : reservationDto.getSeat_id()) { //좌석
                reservationService.releaseSeat(reservationDto.getMovie_id(), seatId, reservationDto.getUser_id());
                model.addAttribute("errorMessage", "결제 실패");
            }
            return "redirect:/reservation/pay";
        }else{
            reservationService.saveReservation(reservationDto);
            return "/reserve/finishPay";
        }

    }

}
