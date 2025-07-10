package com.movie.controller.seat;

import com.movie.dto.seat.SeatDto;
import com.movie.entity.reservation.ReservedSeat;
import com.movie.entity.cinema.Schedule;
import com.movie.entity.cinema.ScreenRoom;
import com.movie.entity.cinema.Seat;
import com.movie.repository.reservation.ReservedSeatRepository;
import com.movie.repository.cinema.ScheduleRepository;
import com.movie.repository.seat.SeatRepository;
import com.movie.service.seat.SeatService;
import com.movie.service.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reservation")
public class SeatController {

    private final SeatService seatService;
    private final ScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final ReservationService reservationService;

    @PostMapping("/seats")
    public ResponseEntity<String> createSeat(@RequestBody SeatDto seatDto) {
        seatService.saveSeat(seatDto);
        return ResponseEntity.ok("좌석 저장 완료");
    }

    @GetMapping("/reserve")
    public String showSeatPage(@RequestParam Long scheduleId, Model model) {
        try {
            Schedule schedule = scheduleRepository.findById(scheduleId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 스케줄이 존재하지 않습니다: " + scheduleId));

            List<String> seatRows = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");

            ScreenRoom screenRoom = schedule.getScreenRoom(); // 상영관
            List<Seat> seats = seatRepository.findByScreenRoom(screenRoom); // 좌석 목록

            // 좌석이 없으면 기본 좌석 생성
            if (seats.isEmpty()) {
                seats = createDefaultSeats(screenRoom);
            }

            List<ReservedSeat> reservedSeats = reservedSeatRepository.findBySeat_ScreenRoom(screenRoom);

            // 예약된 좌석 목록을 "A1", "B3" 이런 식으로 저장 (1부터 시작)
            List<String> reservedKeys = reservedSeats.stream()
                    .map(rs -> rs.getSeat().getSeatRow() + rs.getSeat().getSeatColumn())
                    .toList();

            /// //////////////이다은 추가 //////////////////
            //redis에서 hold중인 좌석
            List<Seat> holdingSeat = reservationService.getHoldingSeats(scheduleId);

            List<String> holdingSeatName = new ArrayList<>();
            for(Seat seat : holdingSeat){
                String seatName = seat.getSeatRow()+seat.getSeatColumn();
                holdingSeatName.add(seatName);
            }
            model.addAttribute("holdingSeatList", holdingSeatName);
            /// ////////////////////////////////////////////
            model.addAttribute("seatRows", seatRows);
            model.addAttribute("seats", seats);
            model.addAttribute("reservedKeys", reservedKeys);
            model.addAttribute("schedule", schedule);
            model.addAttribute("scheduleId", scheduleId);

            return "seat/seatSelect"; // → 좌석 선택 HTML
        } catch (Exception e) {
            model.addAttribute("error", "좌석 선택 페이지를 불러오는 중 오류가 발생했습니다: " + e.getMessage());
            return "seat/seatSelect";
        }
    }

    // 기본 좌석 생성 메서드 (0부터 9까지)
    private List<Seat> createDefaultSeats(ScreenRoom screenRoom) {
        List<Seat> seats = new ArrayList<>();
        List<String> rows = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");

        for (String row : rows) {
            for (int col = 0; col <= 9; col++) { // 0부터 9까지
                Seat seat = new Seat();
                seat.setSeatRow(row);
                seat.setSeatColumn(col);
                seat.setScreenRoom(screenRoom);
                seat.setPrice(15000);
                seats.add(seat);
            }
        }

        // 생성된 좌석들을 DB에 저장
        seatRepository.saveAll(seats);
        return seats;
    }

} 