package com.movie.controller;

import com.movie.dto.ScheduleDto;
import com.movie.entity.Schedule;
import com.movie.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;
    @GetMapping("/schedule/{cinemaName}")
    public String showSchedule(@PathVariable String cinemaName, Model model) {
        List<ScheduleDto> schedules = scheduleService.getSchedulesByCinemaName(cinemaName);
        model.addAttribute("schedules", schedules);
        return "schedule/scheduleList"; // thymeleaf 뷰
    }

}
