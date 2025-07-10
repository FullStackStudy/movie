package com.movie.repository.cinema;

import com.movie.entity.cinema.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CinemaRepository extends JpaRepository<Cinema, Long> {
    Optional<Cinema> findByName(String name);
    boolean existsByNameAndAddress(String name, String address);
} 