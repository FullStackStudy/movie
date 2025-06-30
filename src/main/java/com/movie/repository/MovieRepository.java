package com.movie.repository;

import com.movie.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByMovieTitle(String movieTitle);

    boolean existsByMovieTitle(String movieTitle);
}