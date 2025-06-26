package com.movie.service.reservation;

import com.movie.dto.ReservedSeatDto;
import com.movie.dto.reservation.ReservationDto;
import com.movie.dto.reservation.ReservationRedisResultDto;
import com.movie.entity.Member;
import com.movie.entity.ReservedSeat;
import com.movie.entity.Seat;
import com.movie.entity.reservation.Reservation;
import com.movie.repository.MemberRepository;
import com.movie.repository.ReservedSeatRepository;
import com.movie.repository.SeatRepository;
import com.movie.repository.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.movie.constant.ReservationStatus.RESERVED;

@Service
@Transactional
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public ReservationRedisResultDto reserveRedis(ReservationDto reservationDto){
        boolean reserved;
        List<String> failList = new ArrayList<>();
        List<String> successList = new ArrayList<>();

        //좌석이 2개 이상일 때 하나씩 레디스 넣어준다.
        for(String seatId : reservationDto.getSeat_id()){
            reserved = tryReserveSeat(reservationDto.getMovie_id(), seatId, reservationDto.getMember_id());
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
        String key = makeKey(movieId, seatId);

        // setIfAbsent = Redis SETNX (Set if Not Exists)
        // 성공하면 true, 이미 존재하면 false (이미 예약된 상태)
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, userId, Duration.ofMinutes(3));
        System.out.println("Redis 저장 시도: key=" + key + ", 결과=" + success);

        return Boolean.TRUE.equals(success);// false랑 null일 때 false반환 아님 true반환
    }

    //예약해제(결제 실패또는 취소시)
    public boolean releaseSeat(Long movieId, String seatId, String userId) {
        String key = makeKey(movieId, seatId);
        String currentUser = redisTemplate.opsForValue().get(key);
        Boolean success=false;
        // 현재 잠금이 해당 사용자 것인지 확인 후 삭제 (안 그러면 다른 사람 예약 지울 위험)
        if (userId.equals(currentUser)) {
            success = redisTemplate.delete(key);
            System.out.println("Redis 삭제 시도: key=" + key + ", 결과=" + success);
        }
        return Boolean.TRUE.equals(success);
    }

    //----------------------------여기까지 redis--------------------------------------//

    public boolean saveReservation(@AuthenticationPrincipal UserDetails userDetails ,ReservationDto reservationDto){ //예약되면 reservedSeat에도 들어가

        System.out.println("userDetail :"+ userDetails.getUsername());
        System.out.println("reDto mem: " + reservationDto.getMember_id());

        //로그인 되어있는 유저랑 지금 가져온 유저가 같은지 검사
        if(reservationDto.getMember_id().equals(userDetails.getUsername())){
            reservationDto.setReservationStatus(RESERVED); //상태  reserve로 넣고

            // dto -> entity 변환
            Reservation reservation = reservationDto.createReservation();

            //seatId String으로 변환
            String seat_id = reservation.getSeat_id().toString(); //seatId String으로 변환

            //seatId 저장
            reservation.setSeat_id(seat_id); //seatId 저장

            reservationRepository.save(reservation); //예약 정보 저장
            //예약정보에는 list로 들어가있고 seat에서 나눠서 하나씩 넣고, redis도 삭제해줌

            Member member = memberRepository.findById(reservationDto.getMember_id()).orElseThrow(() ->new IllegalArgumentException("회원 없음"));

            //해당 유저 id랑 가져온 id 같으면 redis지움 and reservedSeat에 하나씩 넣음
            for (String seat : reservationDto.getSeat_id()) {
                Seat reservedSeat = seatRepository.findById(Long.parseLong(seat)).orElseThrow(() -> new IllegalArgumentException("좌석없음"));

                ReservedSeat reservedSeatEntity = new ReservedSeat();
                reservedSeatEntity.setReservation(reservation);
                reservedSeatEntity.setMember(member);
                reservedSeatEntity.setSeat(reservedSeat);
                reservedSeatEntity.setMovie_id(reservation.getMovie_id());

                //reservedSeatEntity 채워서 디비 넣음
                reservedSeatRepository.save(reservedSeatEntity);

                //redis
                String key = makeKey(reservationDto.getMovie_id(), seat);
                String currentUser = redisTemplate.opsForValue().get(key);

                //지금유저랑 redis 들어가있는 유저가 같아야 지울 수 있음.
                if(currentUser.equals(reservationDto.getMember_id())) {
                    releaseSeat(reservationDto.getMovie_id(), seat, currentUser); //redis 삭제
                }
            }
            return true;
        }else{
            System.out.println("로그인 유저와 지금 저장하려는 유저가 다름");
            return false;
        }
    }

    //키 구조 만드는 함수
    public String makeKey(Long movieId, String seatId){
        return "seat:" + movieId + ":" + seatId;
    }
}
