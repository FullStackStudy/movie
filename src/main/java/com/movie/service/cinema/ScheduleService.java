package com.movie.service.cinema;

import com.movie.dto.cinema.CinemaDto;
import com.movie.dto.cinema.ScheduleDto;
import com.movie.dto.seat.SeatDto;
import com.movie.entity.cinema.Cinema;
import com.movie.entity.movie.Movie;
import com.movie.entity.cinema.Schedule;
import com.movie.entity.cinema.ScreenRoom;
import com.movie.repository.cinema.CinemaRepository;
import com.movie.repository.movie.MovieRepository;
import com.movie.repository.cinema.ScheduleRepository;
import com.movie.repository.cinema.ScreenRoomRepository;
import com.movie.repository.reservation.ReservationRepository;
import com.movie.repository.reservation.ReservedSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleService {

    private final MovieRepository movieRepository;
    private final ScreenRoomRepository screenRoomRepository;
    private final CinemaRepository cinemaRepository;
    private final ScheduleRepository scheduleRepository;
    private final ReservationRepository reservationRepository;
    private final ReservedSeatRepository reservedSeatRepository;

    /* 스케줄 등록: DTO를 받아서 스케줄 등록*/
    public void registerFromDto(ScheduleDto dto) {
        Movie movie = movieRepository.findByMovieTitle(dto.getMovieTitle())
                .orElseThrow(() -> new IllegalArgumentException("해당 영화가 존재하지 않습니다: " + dto.getMovieTitle()));

        ScreenRoom room = screenRoomRepository
                .findByRoomNmAndCinema_Name(dto.getScreenRoomName(), dto.getCinemaNm())
                .orElseThrow(() -> new IllegalArgumentException("해당 상영관이 존재하지 않습니다: " + dto.getScreenRoomName()));

        Cinema cinema = cinemaRepository.findByName(dto.getCinemaNm())
                .orElseThrow(() -> new IllegalArgumentException("해당 영화관이 존재하지 않습니다: " + dto.getCinemaNm()));

        // 💡 createSchedule 정적 메서드 활용하여 Schedule 생성
        Schedule schedule = Schedule.createSchedule(dto, cinema, movie, room);
        scheduleRepository.save(schedule);
    }

    /*
        특정 영화관의 전체 스케줄을 조회 전략 - 영화별 그룹화 Map<String, List<ScheduleDto>>
     */
    @Transactional(readOnly = true)
    public Map<String, List<ScheduleDto>> getGroupedSchedulesByCinemaName(String cinemaName) {
        List<Schedule> schedules = scheduleRepository.findByCinema_Name(cinemaName); // 👈 더 직관적인 방식

        return schedules.stream()
                .map(schedule -> {
                    ScheduleDto dto = new ScheduleDto();
                    dto.setId(schedule.getId());
                    dto.setMovieTitle(schedule.getMovie().getMovieTitle());
                    dto.setScreenRoomName(schedule.getScreenRoom().getRoomNm());
                    dto.setCinemaNm(schedule.getCinema().getName()); // 👈 dto에 영화관 이름도 담기
                    dto.setStartTime(schedule.getStartTime());
                    dto.setShowDate(schedule.getShowDate());
                    dto.setStatus(schedule.getStatus());
                    dto.setAvailableSeat(schedule.getAvailableSeat());
                    return dto;
                })
                .collect(Collectors.groupingBy(
                        ScheduleDto::getMovieTitle, // 영화 제목으로 그룹화
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted((a, b) -> a.getShowDate().compareTo(b.getShowDate())) // 시작 시간 순 정렬
                                        .collect(Collectors.toList())
                        )
                ));
    }
    @Transactional(readOnly = true)
    //스케줄 별 잔여좌석 관리하기
    public void updateAvailableSeats(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 스케줄이 존재하지 않습니다."));
        int totalSeat = schedule.getScreenRoom().getTotalSeats();
        int reservedCount = reservedSeatRepository.countBySchedule_Id(scheduleId);

        int availableSeat = totalSeat - reservedCount;

        schedule.setAvailableSeat(availableSeat);
        scheduleRepository.save(schedule);
    }

    //포스터가져오기 이다은
    public Movie getPosterUrl(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(()-> new NullPointerException());
        return schedule.getMovie();
    }
} 