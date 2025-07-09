package com.movie.entity.cinema;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cinema")
@Getter
@Setter
public class Cinema {
    @Id
    @Column(name = "cinema_id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id; //지점 아이디
    private String name; //지점 이름
    private double latitude;
    private double longitude;
    private String address; //지점 주소
    private int screens; //보유한 상영관 개수
    private LocalTime startTime; //영업 시작시간
    private LocalTime endTime; //영업 종료시간

    @OneToMany(mappedBy = "cinema", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScreenRoom> screenRooms = new ArrayList<>();
} 