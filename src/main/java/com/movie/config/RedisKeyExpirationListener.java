package com.movie.config;

import com.movie.dto.reservation.SeatStatusMessageDto;
import com.movie.entity.cinema.Seat;
import com.movie.repository.seat.SeatRepository;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Component
public class RedisKeyExpirationListener implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final SeatRepository seatRepository;

    public RedisKeyExpirationListener(SimpMessagingTemplate messagingTemplate, SeatRepository seatRepository) {
        this.messagingTemplate = messagingTemplate;
        this.seatRepository = seatRepository;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.info("🔔 Redis expired key: {}", expiredKey);

        String[] parts = expiredKey.split(":");

        if (parts.length == 3 && parts[0].equals("seat")) {
            // seat:{scheduleId}:[object Object]seatId} - 일반적인 형태
            try {
                Long scheduleId = Long.parseLong(parts[1]);
                Long seatId = Long.parseLong(parts[2]);
                sendReleaseMessage(scheduleId, seatId);
            } catch (NumberFormatException e) {
                log.warn("❌ Redis 키에서 scheduleId 또는 seatId를 파싱할 수 없습니다: {}", expiredKey);
            }
        } else if (parts.length == 4 && parts[0].equals("seat") && parts[1].equals("hold")) {
            // seat:hold:{scheduleId}:[object Object]seatId} - hold 형태
            try {
                Long scheduleId = Long.parseLong(parts[2]);
                Long seatId = Long.parseLong(parts[3]);
                sendReleaseMessage(scheduleId, seatId);
            } catch (NumberFormatException e) {
                log.warn("❌ Redis 키에서 scheduleId 또는 seatId를 파싱할 수 없습니다: {}", expiredKey);
            }
        } else {
            log.warn("❌ 잘못된 Redis 키 형식: {}", expiredKey);
        }
    }

    private void sendReleaseMessage(Long scheduleId, Long seatId) {
        try {
            Seat seat = seatRepository.findById(seatId).orElse(null);
            
            if (seat == null) {
                log.warn("❌ 좌석을 찾을 수 없습니다: seatId={}", seatId);
                return;
            }
            
            String seatName = seat.getSeatRow() + seat.getSeatColumn();

            SeatStatusMessageDto seatStatusMessage =
                    new SeatStatusMessageDto(scheduleId, List.of(seatId), List.of(seatName), "released");

            messagingTemplate.convertAndSend("/topic/seats/" + scheduleId, seatStatusMessage);
            log.info("✅ 좌석 해제 메시지 전송 완료: scheduleId={}, seatId={}, seatName={}", scheduleId, seatId, seatName);
            
        } catch (Exception e) {
            log.error("❌ 좌석 해제 메시지 전송 중 오류 발생: scheduleId={}, seatId={}", scheduleId, seatId, e);
        }
    }
}