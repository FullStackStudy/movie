package com.movie.repository;

import com.movie.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 영화별 리뷰 목록 조회 (페이지네이션)
    Page<Review> findByMovieMovieIdOrderByRegDateDesc(Long movieId, Pageable pageable);
    
    // 영화별 리뷰 목록 조회 (전체)
    List<Review> findByMovieMovieIdOrderByRegDateDesc(Long movieId);

    // 회원이 특정 영화에 작성한 리뷰 조회
    Optional<Review> findByMovieMovieIdAndMemberMemberId(Long movieId, String memberId);

    // 회원이 특정 영화에 리뷰를 작성했는지 확인
    boolean existsByMovieMovieIdAndMemberMemberId(Long movieId, String memberId);

    // 영화별 평균 평점 조회
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.movie.movieId = :movieId")
    Double getAverageRatingByMovieId(@Param("movieId") Long movieId);

    // 영화별 리뷰 개수 조회
    @Query("SELECT COUNT(r) FROM Review r WHERE r.movie.movieId = :movieId")
    Long getReviewCountByMovieId(@Param("movieId") Long movieId);
} 