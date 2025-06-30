package com.movie.controller;
import com.movie.dto.ScheduleDto;
import com.movie.repository.CinemaRepository;
import com.movie.repository.MovieRepository;
import com.movie.repository.ScreenRoomRepository;
import com.movie.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@RequiredArgsConstructor
public class AdminScheduleController {

    private final ScheduleService scheduleService;
    private final CinemaRepository cinemaRepository;
    private final MovieRepository movieRepository;
    private final ScreenRoomRepository screenRoomRepository;

    // ⬇️ 스케줄 등록 페이지 열기
    @GetMapping("/admin/schedule")
    public String showScheduleRegisterForm(Model model) {
        var cinemas = cinemaRepository.findAll();
        var movies = movieRepository.findAll();
        var screenRooms = screenRoomRepository.findAll();

        System.out.println("cinemas: " + cinemas.size());
        System.out.println("movies: " + movies.size());
        System.out.println("screenRooms: " + screenRooms.size());

        model.addAttribute("cinemas", cinemas);
        model.addAttribute("movies", movies);
        model.addAttribute("screenRooms", screenRooms);
        model.addAttribute("scheduleDto", new ScheduleDto());
        return "schedule/scheduleRegister";
    }

    // ⬇️ 등록 POST
    @PostMapping("/admin/schedule")
    public String registerSchedule(@ModelAttribute ScheduleDto scheduleDto, RedirectAttributes redirectAttributes) {
        try {
            scheduleService.registerFromDto(scheduleDto);
            redirectAttributes.addFlashAttribute("successMessage", "상영시간표가 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "등록 중 오류 발생: " + e.getMessage());
        }
        return "redirect:/admin/schedule";
    }
}