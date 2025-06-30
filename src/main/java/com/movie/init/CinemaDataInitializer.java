package com.movie.init;

import com.movie.dto.CinemaDto;
import com.movie.service.CinemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CinemaDataInitializer implements CommandLineRunner {

    private final CinemaService cinemaService;

    @Override
    public void run(String... args) throws Exception {
        // 이미 저장된 데이터가 있으면 초기화 안 함
        if (cinemaService.count() == 0) {
            List<CinemaDto> cinemaList = List.of(
                    createCinema("MovieFlex 인천", "인천광역시 미추홀구 필동로 123", 5, 37.456584, 126.705734, "07:00", "23:00"),
                    createCinema("MovieFlex 성남", "경기도 성남시 분당구 판교로 456", 6, 37.419682, 127.128033, "09:00", "23:30"),
                    createCinema("MovieFlex 노량진", "서울 동작구 노량진로 789", 7, 37.515247, 126.942659, "09:00", "23:30"),
                    createCinema("MovieFlex 강남", "서울 강남구 테헤란로 100", 7, 37.498, 127.028, "10:00", "23:30"),
                    createCinema("MovieFlex 부산", "부산광역시 연제구 중앙대로 1001", 4, 35.180260, 129.074667, "08:00", "11:30")

            );
            cinemaService.saveCinemas(cinemaList);
            System.out.println("✅ 초기 영화관 데이터 저장 완료");
        } else {
            System.out.println("✅ 영화관 데이터 이미 존재함 - 초기화 생략");
        }
    }

    private CinemaDto createCinema(String name, String address, int screens, double lat, double lng, String start, String end) {
        CinemaDto dto = new CinemaDto();
        dto.setName(name);
        dto.setAddress(address);
        dto.setScreens(screens);
        dto.setLat(lat);
        dto.setLng(lng);
        dto.setStartTime(start);
        dto.setEndTime(end);
        return dto;
    }
}