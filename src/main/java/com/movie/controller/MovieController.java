package com.movie.controller;

import com.movie.service.MovieService;
import com.movie.entity.Movie;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;

    @GetMapping("/movie/{movieId}")
    public String movieDetail(@PathVariable Long movieId, Model model) {
        Movie movie = movieService.getMovieById(movieId); // Movie 엔티티 반환
        model.addAttribute("movie", movie);
        return "movie/movieDetail";
    }
}
