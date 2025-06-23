package com.movie.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class ReservationController {

    @PostMapping("/reserve")
    public String reserveSeats(){
        return "reservation/reservePay";
    }
}
