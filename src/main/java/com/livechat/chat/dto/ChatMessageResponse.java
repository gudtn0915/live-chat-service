package com.livechat.chat.dto;

import com.livechat.chat.domain.ChatMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatMessageResponse {

    private String channelId;
    private String sender;
    private String content;
    private ChatMessage.MessageType type;
    private LocalDateTime createdAt;

    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .channelId(message.getChannelId())
                .sender(message.getSender())
                .content(message.getContent())
                .type(message.getType())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
