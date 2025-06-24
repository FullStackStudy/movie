package com.movie.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieDto { // movie db

    private Long movieId;

    @NotBlank
    private String movieTitle;

    @NotBlank
    private String movieContent;

    private String moviePoster;
} 