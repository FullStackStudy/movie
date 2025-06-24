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

    @Column(name = "movie_content", columnDefinition = "TEXT")
    private String movieContent;

    // 포스터 url
    @Column(name = "movie_poster", length = 500)
    private String moviePoster;

    @Column(name = "movie_regdate", nullable = false)
    private LocalDateTime regDate;

    @Column(name = "movie_updatedate")
    private LocalDateTime updateDate;

    @PrePersist
    protected void onCreate() {
        regDate = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateDate = LocalDateTime.now();
    }
}
