package com.movie.service.reservation;

import com.movie.dto.reservation.SeatStatusMessageDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    //생성자 주입
    public SeatNotificationService(SimpMessagingTemplate messagingTemplate){
        this.messagingTemplate = messagingTemplate;
    }

    //특정 스케줄과 좌석목록에대해 예약중상태를 알림 메세지로 전송
    public void notifySeatHold(Long scheduleId, List<Long> seatId, String status){
        SeatStatusMessageDto message = new SeatStatusMessageDto(scheduleId, seatId, status);
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
