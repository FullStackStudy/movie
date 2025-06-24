package com.movie.controller;


import com.movie.dto.ReservationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class MainController {

    @GetMapping("/")
    public String main(){
        //model.addAttribute("reserveData", new ReservationDto());
        return "redirect:/reservation" ;
    }
}
