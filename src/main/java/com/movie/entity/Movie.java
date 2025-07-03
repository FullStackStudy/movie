package com.movie.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity
@Table(name = "movie")
@Getter
@Setter
@ToString
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "movie_title", nullable = false, length = 200)
    private String movieTitle;

    // 장르
    @Column(name = "movie_genre",nullable = false)
    private String genre;

    @Column(name = "movie_cast",nullable = false)
    private String movieCast;

    @Column(name = "movie_rating",nullable = false)
    private double movieRating;

    // 줄거리
    @Column(name = "movie_content", columnDefinition = "TEXT")
    private String movieContent;

    // 포스터 url
    @Column(name = "movie_poster", length = 500)
    private String moviePoster;

    @Column(name = "movie_price",nullable = false)
    private Integer moviePrice = 12000;

    // 등록시간
    @Column(name = "movie_reg_date", nullable = false)
    private LocalDateTime regDate;

    // 개봉일
    @Column(name = "movie_open_date")
    private String openDate;

    // 기본 정보 (연령, 상영시간, 개봉국가)
    @Column(name = "movie_detailinfo")
    private String detailInfo;

    @PrePersist
    protected void onCreate() {
        regDate = LocalDateTime.now();
    }
}
