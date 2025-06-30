package com.movie.service;

import com.movie.dto.MovieDto;
import com.movie.entity.Movie;
import com.movie.repository.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    @Autowired
    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    // 메인에 보여줄 영화 리스트 반환
    public List<MovieDto> getMainMovies() {
        List<Movie> movies = movieRepository.findAll(); // 필요시 정렬/상위 N개만 반환 등 커스텀
        return movies.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Entity → DTO 변환
    private MovieDto convertToDto(Movie movie) {
        MovieDto dto = new MovieDto();
        dto.setMovieId(movie.getMovieId());
        dto.setMovieTitle(movie.getMovieTitle());
        dto.setMoviePoster(movie.getMoviePoster());
        dto.setGenre(movie.getGenre());
        dto.setDetailInfo(movie.getDetailInfo());
        dto.setMovieRating(movie.getMovieRating());
        dto.setOpenDate(movie.getOpenDate());
        return dto;
    }

    public Movie getMovieById(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다."));
    }
}
