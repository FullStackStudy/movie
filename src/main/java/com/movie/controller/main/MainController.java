package com.movie.controller.main;

import com.movie.dto.movie.MovieDto;
import com.movie.service.MainService;
import com.movie.service.movie.MovieService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Controller
public class MainController {

    private final MovieService movieService;
    private final MainService mainService;
    @Autowired
    public MainController(MovieService movieService, MainService mainService) {
        this.movieService = movieService;
        this.mainService = mainService;
    }

    @GetMapping("/")
    public String main(Model model, @RequestParam(required = false) String signupSuccess) {
        List<MovieDto> movies = movieService.getMainMovies(); // 크롤링 데이터 반환
        model.addAttribute("movies", movies);


        //영상 크롤링
        List<String> videoInfo = mainService.crowlingMovieVideo();
        String url = videoInfo.get(0);
        String title = videoInfo.get(1);
        model.addAttribute("url", url);
        model.addAttribute("title", title);
        
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