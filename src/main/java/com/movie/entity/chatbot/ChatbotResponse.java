package com.movie.entity.chatbot;

import com.movie.constant.ResponseType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bot_id")
    private Long id;

    // 나중에 user와 매핑 예정

    private String response;     // 실제 응답

    @Enumerated(EnumType.STRING)
    private ResponseType type;   //TEXT, LINK
} 