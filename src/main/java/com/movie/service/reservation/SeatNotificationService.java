package com.movie.service.reservation;

import com.movie.dto.reservation.SeatStatusMessageDto;
import com.movie.entity.cinema.Seat;
import com.movie.repository.seat.SeatRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeatNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SeatRepository seatRepository;

    public SeatNotificationService(SimpMessagingTemplate messagingTemplate, SeatRepository seatRepository){
        this.messagingTemplate = messagingTemplate;
        this.seatRepository = seatRepository;
    }

    //특정 스케줄과 좌석목록에대해 예약중상태를 알림 메세지로 전송
    public void notifySeatHold(Long scheduleId, List<Long> seatId, String status){

        //id-> name 변경
        List<String> seatNames = seatId.stream()
                .map(id -> seatRepository.findById(id)
                        .map(seat -> seat.getSeatRow() + seat.getSeatColumn())
                        .orElse("unknown"))
                .collect(Collectors.toList());

        SeatStatusMessageDto message = new SeatStatusMessageDto(scheduleId, seatId, seatNames, status);
        //메세지전송
        //구독경로: /topic/seats/{scheduleId}
        messagingTemplate.convertAndSend("/topic/seats/"+scheduleId, message);
    }

    //특정 스케줄과 좌석목록에대해 예약중상태를 알림 메세지로 전송인데 ttl이랑 같이 알람때문에
    public void notifyPayHold(Long scheduleId, List<Long> seatId, String status, Long ttl){
        SeatStatusMessageDto message = new SeatStatusMessageDto(scheduleId, seatId, status ,ttl !=null? ttl.intValue() : 60);
        System.out.println("▶️ WebSocket 메시지 전송 대상: /topic/pay/" + scheduleId);
        System.out.println("▶️ 메시지 내용: " + message); // JSON 직렬화
        messagingTemplate.convertAndSend("/topic/pay/"+scheduleId, message);
    }
}
