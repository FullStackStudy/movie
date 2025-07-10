package com.movie.controller.cinema;
import com.movie.dto.cinema.CinemaDto;
import com.movie.entity.cinema.Cinema;
import com.movie.service.cinema.CinemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/cinema")
@RequiredArgsConstructor
public class CinemaController {

    private final CinemaService cinemaService;

    @GetMapping("/map")
    public String showMapPage(Model model) {
        List<Cinema> cinemas = cinemaService.getAll();
        model.addAttribute("cinemas", cinemas);
        return "cinema/map";
    }
} 