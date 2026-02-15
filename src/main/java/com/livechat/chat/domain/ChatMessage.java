package com.livechat.chat.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message", indexes = {
        @Index(name = "idx_channel_id_created_at", columnList = "channelId, createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String channelId;

    @Column(nullable = false, length = 50)
    private String sender;

    @Column(nullable = false, length = 500)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType type;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChatMessage(String channelId, String sender, String content, MessageType type) {
        this.channelId = channelId;
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE
    }
}
