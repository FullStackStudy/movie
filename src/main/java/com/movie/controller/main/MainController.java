package com.movie.controller.main;

import com.movie.dto.movie.MovieDto;
import com.movie.service.movie.MovieService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public String main(Model model, @RequestParam(required = false) String signupSuccess) {
        List<MovieDto> movies = movieService.getMainMovies(); // 크롤링 데이터 반환
        model.addAttribute("movies", movies);
        
        // 회원가입 성공 메시지 처리
        if ("true".equals(signupSuccess)) {
            model.addAttribute("signupSuccessMessage", "회원가입이 성공적으로 완료되었습니다! 🎉");
        }
        
        return "main";
    }

    @GetMapping("/error")
    public String handleError() {
        return "error";
    }
} 