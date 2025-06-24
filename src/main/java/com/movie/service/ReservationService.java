package com.movie.service;

import com.movie.dto.ReservationDto;
import com.movie.dto.ReservationRedisResultDto;
import com.movie.entity.Reservation;
import com.movie.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.movie.constant.ReservationStatus.RESERVED;

@Service
@Transactional
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public ReservationRedisResultDto reserveRedis(ReservationDto reservationDto){
        boolean reserved;
        List<String> failList = new ArrayList<>();
        List<String> successList = new ArrayList<>();

        //좌석이 2개 이상일 때 하나씩 레디스 넣어준다.
        for(String seatId : reservationDto.getSeat_id()){
            reserved = tryReserveSeat(reservationDto.getMovie_id(), seatId, reservationDto.getUser_id());
            if(!reserved){
                failList.add(seatId);
            }else {
                successList.add(seatId);
            }
        }
        return new ReservationRedisResultDto(successList, failList);
    }


    //seat:MOV123:A1 = user123
    public boolean tryReserveSeat(Long movieId, String seatId, String userId) {
        String key = String.format("seat:%s:%s", movieId, seatId);

        // setIfAbsent = Redis SETNX (Set if Not Exists)
        // 성공하면 true, 이미 존재하면 false (이미 예약된 상태)
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, userId, Duration.ofMinutes(3));
        System.out.println("Redis 저장 시도: key=" + key + ", 결과=" + success);

        return Boolean.TRUE.equals(success);// false랑 null일 때 false반환 아님 true반환
    }

    //예약해제(결제 실패또는 취소시)
    public void releaseSeat(Long movieId, String seatId, String userId) {
        String key = String.format("seat:%s:%s", movieId, seatId);
        String currentUser = redisTemplate.opsForValue().get(key);

        // 현재 잠금이 해당 사용자 것인지 확인 후 삭제 (안 그러면 다른 사람 예약 지울 위험)
        if (userId.equals(currentUser)) {
            redisTemplate.delete(key);
        }
    }

    //----------------------------여기까지 redis--------------------------------------//

    public void saveReservation(ReservationDto reservationDto){
        //로그인 되어있는 아이디랑 같은지 검사하기 나중에
        reservationDto.setReservationStatus(RESERVED);
        Reservation reservation = reservationDto.createReservation();
        String seat_id = reservation.getSeat_id().toString();
        reservation.setSeat_id(seat_id);
        reservationRepository.save(reservation);

            //reservatoinDto.seatId는 list로 받고, 예약정보에는 list로 들어가있고 seat에서만 list 나눠서 하나씩 넣자.

    }
}
