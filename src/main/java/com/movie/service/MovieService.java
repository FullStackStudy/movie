package com.movie.service;

import com.movie.dto.MovieDto;
import com.movie.entity.Movie;
import com.movie.repository.MovieRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    @Autowired
    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    // 메인배너 - 캐싱 적용
    @Cacheable(value = "mainMovies", key = "'main'")
    public List<MovieDto> getMainMovies() {
        List<Movie> movies = movieRepository.findAll();
        return movies.stream()
                .map(this::convertToDto)
                .limit(15) // 15개만 반환
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

    // 영화 상세 정보 - 캐싱 적용
    @Cacheable(value = "movieDetail", key = "#movieId")
    public Movie getMovieById(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("영화를 찾을 수 없습니다."));
    }

    // 영화 검색 - 캐싱 적용
    @Cacheable(value = "movieSearch", key = "#keyword")
    public List<MovieDto> searchMovies(String keyword) {
        List<Movie> movies = movieRepository.findByMovieTitleContainingIgnoreCase(keyword);
        return movies.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // 페이징 영화 목록 - 캐싱 적용
    @Cacheable(value = "moviePaged", key = "'page_' + #page + '_size_' + #size")
    public List<MovieDto> getMainMoviesPaged(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Movie> movies = movieRepository.findAll(pageable);
        return movies.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // 페이징 검색 - 캐싱 적용
    @Cacheable(value = "movieSearchPaged", key = "'search_' + #keyword + '_page_' + #page + '_size_' + #size")
    public List<MovieDto> searchMoviesPaged(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Movie> movies = movieRepository.findByMovieTitleContainingIgnoreCase(keyword, pageable);
        return movies.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // 캐시 삭제 메서드 (새 영화 추가 시)
    @CacheEvict(value = {"mainMovies", "moviePaged"}, allEntries = true)
    public void evictMainMoviesCache() {
        // 메인 영화 관련 캐시 삭제
    }

    // 검색 캐시 삭제
    @CacheEvict(value = {"movieSearch", "movieSearchPaged"}, allEntries = true)
    public void evictSearchCache() {
        // 검색 관련 캐시 삭제
    }

    // 모든 캐시 삭제
    @CacheEvict(value = {"mainMovies", "movieDetail", "movieSearch", "moviePaged", "movieSearchPaged"}, allEntries = true)
    public void evictAllCaches() {
        // 모든 캐시 삭제
    }
}
