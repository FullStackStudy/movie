package com.movie.controller.movie;

import com.movie.service.movie.MovieService;
import com.movie.service.movie.ReviewService;
import com.movie.entity.movie.Movie;
import com.movie.dto.movie.MovieDto;
import com.movie.dto.movie.ReviewDto;
import org.springframework.data.domain.Page;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;
    private final ReviewService reviewService;

    @GetMapping("/movie") // infinite scroll
    public String movieList(@RequestParam(value = "search", required = false) String search, Model model) {
        List<MovieDto> movies;
        int firstPageSize = 8;
        if (search != null && !search.trim().isEmpty()) {
            movies = movieService.searchMoviesPaged(search, 0, firstPageSize);
        } else {
            movies = movieService.getMainMoviesPaged(0, firstPageSize);
        }
        model.addAttribute("movies", movies);
        model.addAttribute("search", search);
        return "movie/movieList";
    }

    @GetMapping("/movie/{movieId}")
    public String movieDetail(@PathVariable Long movieId, 
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        Movie movie = movieService.getMovieById(movieId); // Movie 엔티티 반환
        
        // 리뷰 정보 조회 (페이지네이션)
        int pageSize = 5; // 페이지당 10개 리뷰
        Page<ReviewDto> reviewPage = reviewService.getReviewsByMovieIdPaged(movieId, page, pageSize);
        Double averageRating = reviewService.getAverageRatingByMovieId(movieId);
        Long reviewCount = reviewService.getReviewCountByMovieId(movieId);
        
        // 로그인한 사용자의 리뷰 작성 가능 여부 확인
        boolean canReview = false;
        boolean isAdmin = false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String memberId = auth.getName();
            canReview = reviewService.canWriteReview(movieId, memberId);
            isAdmin = reviewService.isAdmin(memberId);
        }
        
        model.addAttribute("movie", movie);
        model.addAttribute("reviews", reviewPage.getContent());
        model.addAttribute("reviewPage", reviewPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("averageRating", averageRating != null ? averageRating : 0.0);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("canReview", canReview);
        model.addAttribute("isAdmin", isAdmin);
        
        return "movie/movieDetail";
    }

    @GetMapping("/movie/list-ajax")
    @ResponseBody
    public List<MovieDto> getMoviesAjax( // infinite scroll 사용을 위한 ajax data
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(value = "search", required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return movieService.searchMoviesPaged(search, page, size);
        } else {
            return movieService.getMainMoviesPaged(page, size);
        }
    }
} 