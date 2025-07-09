package com.movie.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration  // 스프링 설정 클래스임을 표시
@EnableWebSocketMessageBroker  // WebSocket 메시지 브로커 활성화 (STOMP 프로토콜 사용)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 메시지 브로커 설정 (클라이언트가 구독할 topic, 그리고 서버가 받을 메시지 prefix 지정)
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");  // "/topic"으로 시작하는 주소를 가진 메시지는 내장 브로커가 구독자에게 전달
        config.setApplicationDestinationPrefixes("/app");  // 클라이언트가 서버로 메시지 보낼 때 경로 앞에 붙이는 prefix
    }

    // WebSocket 연결할 엔드포인트 등록 및 SockJS 설정
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-seat")  // 클라이언트가 WebSocket 연결 요청할 때 접속하는 URL
                .setAllowedOriginPatterns("*")  // 모든 도메인에서 접속 허용 (보안 이슈 있을 수 있음, 필요시 변경)
                .withSockJS();  // SockJS 사용하여 WebSocket 미지원 브라우저 대응 (폴백 옵션)
    }

}
