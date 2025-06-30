package com.movie.controller;

import com.movie.dto.MovieDto;
import com.movie.service.MovieService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Controller
public class MainController {

    private final MovieService movieService;

    @Autowired
    public MainController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/")
    public String main(Model model) {
        List<MovieDto> movies = movieService.getMainMovies(); // 크롤링 데이터 반환
        model.addAttribute("movies", movies);
        return "main";
    }

    @GetMapping("/main")
    public String main() {
        return "main";
    }

    @GetMapping("/error")
    public String handleError() {
        return "error";
    }
}