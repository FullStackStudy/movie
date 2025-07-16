package com.movie.service.reservation;

import com.movie.constant.ReservationStatus;
import com.movie.dto.reservation.ReservedSeatDto;
import com.movie.dto.cinema.ScheduleDto;
import com.movie.dto.seat.SeatDto;
import com.movie.dto.reservation.ReservationDto;
import com.movie.dto.reservation.ReservationRedisResultDto;
import com.movie.dto.reservation.ReservationResponseDto;
import com.movie.entity.cinema.Schedule;
import com.movie.entity.cinema.Seat;
import com.movie.entity.member.Member;
import com.movie.entity.reservation.Reservation;
import com.movie.entity.reservation.ReservedSeat;
import com.movie.repository.member.MemberRepository;
import com.movie.repository.seat.SeatRepository;
import com.movie.repository.reservation.ReservedSeatRepository;
import com.movie.repository.cinema.ScheduleRepository;
import com.movie.repository.cinema.ScreenRoomRepository;
import com.movie.repository.reservation.ReservationRepository;
import com.movie.service.cinema.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.AccessDeniedException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final ScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;
    private final ScreenRoomRepository screenRoomRepository;

    final private StringRedisTemplate redisTemplate; //redis
    final private SeatNotificationService seatNotificationService; //websocket

    private static int SeatHoldTimeoutMinutes;

    public List<ScheduleDto> getScheduleInfo(){//
            List<Schedule> scheduleList = scheduleRepository.findAll();
            List<ScheduleDto> scheduleDtoList = new ArrayList<>();
        for(Schedule schedule : scheduleList) {
            ScheduleDto scheduleDto = new ScheduleDto();
            scheduleDto.setMovieTitle(schedule.getMovie().getMovieTitle());
            scheduleDto.setCinemaNm(schedule.getCinema().getName());
            scheduleDto.setScreenRoomName(schedule.getScreenRoom().getRoomNm());
            scheduleDto.setStartTime(schedule.getStartTime());
            scheduleDto.setId(schedule.getId());
            scheduleDto.setRoomId(schedule.getScreenRoom().getId());
            scheduleDtoList.add(scheduleDto);
        }
        //스케쥴 가져와서 보여주고 고르는걸로 하자 지기ㅡㅁ
        return scheduleDtoList;
    }
    //해당 스크린 좌석 가져오기
    public List<SeatDto> getSeats(Long roomId){
        //roomId로 screenRoom객체 가져오고
        //ScreenRoom screenRoom = screenRoomRepository.findById(roomId).orElseThrow(()-> new IllegalArgumentException());
        //
        List<Seat> seatList = seatRepository.findByScreenRoom_Id(roomId);
        List<SeatDto> seatDtos1 = SeatDto.ofList(seatList);

        return seatDtos1;
    }

    //해당 스케쥴 예약된 좌석 가져오기
    public List<ReservedSeatDto> getReservedSeats(Long scheduleId){
        List<ReservedSeat> reservedSeatList = reservedSeatRepository.findBySchedule_Id(scheduleId);
        List<ReservedSeatDto> reservedSeatDtoList = new ArrayList<>();
        for(ReservedSeat reservedSeat: reservedSeatList){
            ReservedSeatDto reservedSeatDto = new ReservedSeatDto();
            reservedSeatDto.setReservedId(reservedSeat.getReservation().getId());
            reservedSeatDto.setSeatId(reservedSeat.getSeat().getId());
            reservedSeatDto.setMemberId(reservedSeat.getMember().getMemberId());
            reservedSeatDto.setScheduleId(reservedSeat.getSchedule().getId());

            reservedSeatDtoList.add(reservedSeatDto);

        }
        return reservedSeatDtoList;
    }

    //예약 저장
    public boolean saveReservation(@AuthenticationPrincipal UserDetails userDetails ,ReservationDto reservationDto){ //예약되면 reservedSeat에도 들어가

        System.out.println("userDetail :"+ userDetails.getUsername());
        System.out.println("reDto mem: " + reservationDto.getMemberId());

        //로그인 되어있는 유저랑 지금 가져온 유저가 같은지 검사
        if(reservationDto.getMemberId().equals(userDetails.getUsername())){
            Member member = memberRepository.findById(reservationDto.getMemberId()).orElseThrow(() ->new IllegalArgumentException("회원 없음"));
            Schedule schedule = scheduleRepository.findById(reservationDto.getScheduleId()).orElseThrow(()->new IllegalArgumentException("스케쥴 없음"));
            // dto -> entity 변환
            Reservation reservation = reservationDto.createReservation(schedule, member, reservationDto);

            reservationRepository.save(reservation); //예약 정보 저장

            scheduleService.updateAvailableSeats(schedule.getId());

            //예약정보에는 list로 들어가있고 seat에서 나눠서 하나씩 넣고, redis도 삭제해줌
            //해당 유저 id랑 가져온 id 같으면 redis지움 and reservedSeat에 하나씩 넣음
            for (Long seat : reservationDto.getSeatId()) {
                Seat reservedSeat = seatRepository.findById(seat).orElseThrow(() -> new IllegalArgumentException("좌석없음"));

                ReservedSeat reservedSeatEntity = new ReservedSeat();
                reservedSeatEntity.setReservation(reservation);
                reservedSeatEntity.setMember(member);
                reservedSeatEntity.setSeat(reservedSeat);
                reservedSeatEntity.setSchedule(schedule);

                //reservedSeatEntity 채워서 디비 넣음
                reservedSeatRepository.save(reservedSeatEntity);

                //redis
                String key = makeKey(reservationDto.getScheduleId(), seat);
                String currentUser = redisTemplate.opsForValue().get(key);

                //지금유저랑 redis 들어가있는 유저가 같아야 지울 수 있음.
                if(currentUser.equals(reservationDto.getMemberId())) {
                    releaseSeat(reservationDto.getScheduleId(), seat, currentUser); //redis 삭제
                }
            }

            //예약하면 추천 ai 새로 받아오기 위해 redis 삭제
            relaseRecomRedis(member.getMemberId());
            return true;
        }else{
            System.out.println("로그인 유저와 지금 저장하려는 유저가 다름");
            return false;
        }
    }

    //해당 유저 예약내역 가져와서 ReservationResponseDto에 넣어주기
    public List<ReservationResponseDto> getAllReservation(String memberId){
        List<Reservation> reservations = reservationRepository.findAllByMember_memberIdOrderByReservedAtDesc(memberId);
        List<ReservationResponseDto> reservationResponseDtos = new ArrayList<>();
        for(Reservation reservation : reservations){
            try {
                ReservationResponseDto dto = getReservationResponseDto(reservation);
                cancelTimeCheck(dto); // 취소시간 체크
                reservationResponseDtos.add(dto);
            } catch (Exception e) {
                System.err.println("예약 처리 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return reservationResponseDtos;
    }

    //예약목록 데이터가져오려고 만든거
    private ReservationResponseDto getReservationResponseDto(Reservation reservation) {
        ReservationResponseDto dto = new ReservationResponseDto();
        dto.setReservedAt(reservation.getReservedAt());
        dto.setPer(reservation.getPer());
        dto.setCinemaName(reservation.getSchedule().getCinema().getName());
        dto.setMovieName(reservation.getSchedule().getMovie().getMovieTitle());
        dto.setScreenName(reservation.getSchedule().getScreenRoom().getRoomNm());
        dto.setShowDate(reservation.getSchedule().getShowDate());
        dto.setStartTime(reservation.getSchedule().getStartTime());
        dto.setReservationId(reservation.getId());
        dto.setReservationStatus(reservation.getReservationStatus());
        return dto;
    }

    //reservaionResponseDto 만들어줌
    public ReservationResponseDto getReservationResponseDto(Long scheduleId, List<SeatDto> seatDtos, String memberId){
        Schedule schedule = scheduleRepository.findById(scheduleId).orElseThrow(()->new IllegalArgumentException("스케줄없음"));

        ReservationResponseDto dto = new ReservationResponseDto();
        List<String> seatNames = new ArrayList<>();
        int price = 0;
        for(SeatDto seatName : seatDtos){
            String name = seatName.getSeatRow() + seatName.getSeatColumn();
            seatNames.add(name);
            price += seatName.getPrice();
        }
        dto.setCinemaName(schedule.getCinema().getName());
        dto.setMovieName(schedule.getMovie().getMovieTitle());
        dto.setScreenName(schedule.getScreenRoom().getRoomNm());
        dto.setShowDate(schedule.getShowDate());
        dto.setStartTime(schedule.getStartTime());
        dto.setPer((long)seatDtos.size());
        dto.setMemberId(memberId);
        dto.setPrice(price);
        dto.setSeatName(seatNames);

        return dto;
    }

    //지금 시간이 영화시간보다 10분 안으로남으면 취소불가
    public void cancelTimeCheck(ReservationResponseDto dto) {
        LocalDateTime movieDateTime = LocalDateTime.of(dto.getShowDate(), dto.getStartTime());
        dto.setCancellable(LocalDateTime.now().isBefore(movieDateTime.minusMinutes(10))); //지금 시간이 영화시간보다 10분 안으로남으면 취소불가

    }
    //예약 취소할 때 저장되어있는 예약을한 유저랑 지금 로그인되어있는 유저가 같은지 확인하기 위함
    public Reservation getMemberIdFromReservation(Long reservationId){
        return reservationRepository.findById(reservationId).orElseThrow(IllegalArgumentException::new);
    }
    //예약취소하기 log를 위해 column삭제말고 reservationStatus를 CANCEL로 바꿈
    public boolean cancelReservation(Long reservationId, UserDetails userDetails) throws AccessDeniedException {
        Reservation reservation = getMemberIdFromReservation(reservationId);

        String memberIdFromReservation = reservation.getMember().getMemberId();
        if(!memberIdFromReservation.equals(userDetails.getUsername())){ //예약햇던 유저와 지금 로긴되있는 유저가 같으면 실행
            throw new AccessDeniedException("삭제 권한이 없습니다. 유저가 다릅니다.");
        }
        //지우려는 예약에서 저장한 좌석 지우고
        reservedSeatRepository.deleteAllByReservation_id(reservationId);
        //상태 cancel로 바꿔준다.
        reservation.setReservationStatus(ReservationStatus.CANCEL);
        reservation.setCancelAt(LocalDateTime.now());

        // ✅ 예약 취소 후 잔여 좌석 갱신
        scheduleService.updateAvailableSeats(reservation.getSchedule().getId());


        return true;
    }
    //----------------------------여기붙터 redis--------------------------------------//
    //키 구조 만드는 함수
    public String makeKey(Long scheduleId, Long seatId){
        return "seat:" + scheduleId + ":" + seatId;
    }

    public ReservationRedisResultDto reserveRedis(List<Long> seatIds, String memberId, Long scheduleId){
        boolean reserved;
        List<Long> failList = new ArrayList<>();
        List<Long> successList = new ArrayList<>();

        //좌석이 2개 이상일 때 하나씩 레디스 넣어준다.
        for(Long seatId : seatIds){
            reserved = tryReserveSeat(scheduleId, seatId, memberId);
            if(!reserved){
                failList.add(seatId);
            }else {
                successList.add(seatId);
            }
        }
        return new ReservationRedisResultDto(successList, failList);
    }


    //seat:MOV123:A1 = user123
    public boolean tryReserveSeat(Long scheduleId, Long seatId, String userId) {
        String key = makeKey(scheduleId, seatId);

        // setIfAbsent = Redis SETNX (Set if Not Exists)
        // 성공하면 true, 이미 존재하면 false (이미 예약된 상태)
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, userId, Duration.ofMinutes(SeatHoldTimeoutMinutes));

        System.out.println("Redis 저장 시도: key=" + key + ", 결과=" + success);

        if(Boolean.TRUE.equals(success)){ //저장 성공하면 websoket 알림전송 hold다!!

            seatNotificationService.notifySeatHold(scheduleId, List.of(seatId), "hold");
        }
        if(Boolean.FALSE.equals(success)){ //실패(저장이되어있음)이지만 실제로 키가있나 다시확인 -> redis 풀리고 새로고침하면 400떠서 한번더 확인
            boolean stillExists = redisTemplate.hasKey(key);

            if(!stillExists){
                success = redisTemplate.opsForValue().setIfAbsent(key, userId, Duration.ofMinutes(SeatHoldTimeoutMinutes));
            }
        }
        return Boolean.TRUE.equals(success);
    }

    //redis 예약해제(결제 실패또는 취소시)
    public boolean releaseSeat(Long scheduleId, Long seatId, String userId) {
        String key = makeKey(scheduleId, seatId);
        String currentUser = redisTemplate.opsForValue().get(key);
        Boolean success=false;
        // 현재 잠금이 해당 사용자 것인지 확인 후 삭제 (안 그러면 다른 사람 예약 지울 위험)
        if (userId.equals(currentUser)) {
            success = redisTemplate.delete(key);
            System.out.println("Redis 삭제 시도: key=" + key + ", 결과=" + success);
            if(Boolean.TRUE.equals(success)){//삭제성공하면
                //websoket 알림(available)
                seatNotificationService.notifySeatHold(scheduleId, List.of(seatId),"available");
            }
        }
        return Boolean.TRUE.equals(success);
    }

    //hold redis list만들기
    public List<Seat> getHoldingSeats(Long scheduleId){
        String keyBySchedule = "seat:"+scheduleId+":*";

        Set<String> keys = redisTemplate.keys(keyBySchedule);
        if(keys==null) return Collections.emptyList();

        List<Long> seatId = keys.stream()
                .map(key -> {
                    String[] parts = key.split(":");
                    return Long.parseLong(parts[2]);
                }).collect(Collectors.toList());

        return seatRepository.findAllById(seatId);
    }
    //예약완료하면 추천 ai 다시 보여주려고 redis 지움
    public void relaseRecomRedis(String memberId){
        try {
            String hybridKey = "recommend:hybrid:"+memberId;
            String collaboKey = "recommend:collabo:"+memberId;
            String contentKey = "recommend:content:"+memberId;
            String tmdbKey = "recommend:tmdb:"+memberId;

            redisTemplate.delete(hybridKey);
            redisTemplate.delete(collaboKey);
            redisTemplate.delete(contentKey);
            redisTemplate.delete(tmdbKey);
            System.out.println("예약저장 추천레디스 삭제 성공");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("예약저장 추천레디스 삭제과정에서 실패하였습니다.");
        }
    }
}
