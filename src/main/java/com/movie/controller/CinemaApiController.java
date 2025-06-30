package com.movie.controller;

import com.movie.dto.CinemaDto;
import com.movie.service.CinemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Rest API 전용
@RestController
@RequestMapping("/cinema/api")
@RequiredArgsConstructor
public class CinemaApiController {
    private final CinemaService cinemaService;

    @PostMapping("/save-all")
    public ResponseEntity<Void> saveAll(@RequestBody List<CinemaDto> dtoList) {
        cinemaService.saveCinemas(dtoList);
        return ResponseEntity.ok().build();  // 단순 성공 응답
    }
}
