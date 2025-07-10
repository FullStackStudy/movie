package com.movie.controller.cinema;

import com.movie.dto.cinema.ScheduleDto;
import com.movie.service.cinema.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ScheduleController {

    private static final Logger log = LoggerFactory.getLogger(ScheduleController.class);
    private final ScheduleService scheduleService;

    /**
     * 특정 영화관의 상영시간표 조회
     */
    @GetMapping("/schedule/{cinemaName}")
    public String showSchedule(@PathVariable String cinemaName, Model model) {
        try {
            // URL 디코딩
            String decodedCinemaName = URLDecoder.decode(cinemaName, StandardCharsets.UTF_8);
            log.info("영화관 이름 (원본): {}, (디코딩): {}", cinemaName, decodedCinemaName);

            // Map<String, List<ScheduleDto>> 형태로 반환한다고 가정
            Map<String, List<ScheduleDto>> schedulesMap = scheduleService.getGroupedSchedulesByCinemaName(decodedCinemaName);
            log.info("영화관: {}, 영화 종류 수: {}", decodedCinemaName, schedulesMap.size());

            model.addAttribute("schedulesMap", schedulesMap);
            model.addAttribute("cinemaName", decodedCinemaName);
            return "schedule/scheduleList";
        } catch (Exception e) {
            log.error("상영시간표 조회 중 오류 발생: {}", e.getMessage(), e);
            model.addAttribute("error", "상영시간표를 불러오는 중 오류가 발생했습니다.");
            model.addAttribute("schedulesMap", null);
            model.addAttribute("cinemaName", cinemaName);
            return "schedule/scheduleList";
        }
    }

    /**
     * (옵션) 영화관 선택해서 스케줄 보기 위한 폼
     */
    @GetMapping("/schedule")
    public String selectCinemaForSchedule() {
        return "schedule/selectCinema"; // 예: 영화관 이름 입력받는 간단한 폼
    }
} 