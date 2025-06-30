package com.movie.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MovieDto { // movie db

    @NotBlank
    private Long movieId;

    @NotBlank
    private String movieTitle;

    @NotBlank
    private String genre; // 장르

    @NotBlank
    private String movieRuntime;

    private double movieRating;

    @NotBlank
    private String movieCast;

    @NotBlank
    private String movieContent;

    private String moviePoster;

    private String detailInfo; // 기본정보

    private String openDate; // 개봉일
}