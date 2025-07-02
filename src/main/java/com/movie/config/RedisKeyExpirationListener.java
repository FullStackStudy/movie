package com.movie.config;

import com.movie.dto.reservation.SeatStatusMessageDto;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RedisKeyExpirationListener implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    public RedisKeyExpirationListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        System.out.println("🔔 Redis expired key: " + expiredKey);

        String[] parts = expiredKey.split(":");

        if (parts.length == 4 && parts[0].equals("seat") && parts[1].equals("hold")) {
            // seat:hold:{scheduleId}:{seatId}
            Long scheduleId = Long.parseLong(parts[2]);
            Long seatId = Long.parseLong(parts[3]);

            sendReleaseMessage(scheduleId, seatId);

        } else if (parts.length == 3 && parts[0].equals("seat")) {
            // seat:{scheduleId}:{seatId}
            Long scheduleId = Long.parseLong(parts[1]);
            Long seatId = Long.parseLong(parts[2]);

            sendReleaseMessage(scheduleId, seatId);

        } else {
            System.err.println("❌ 잘못된 Redis 키 형식: " + expiredKey);
        }
    }

    private void sendReleaseMessage(Long scheduleId, Long seatId) {
        SeatStatusMessageDto seatStatusMessage = new SeatStatusMessageDto(scheduleId, List.of(seatId),"released");

        messagingTemplate.convertAndSend("/topic/seats/" + scheduleId, seatStatusMessage);
    }
}