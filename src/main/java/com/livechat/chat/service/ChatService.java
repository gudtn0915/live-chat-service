package com.livechat.chat.service;

import com.livechat.chat.domain.ChatMessage;
import com.livechat.chat.domain.ChatMessageRepository;
import com.livechat.chat.dto.ChatMessageRequest;
import com.livechat.chat.dto.ChatMessageResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final Counter chatMessageCounter;

    public ChatService(SimpMessagingTemplate messagingTemplate,
                       ChatMessageRepository chatMessageRepository,
                       MeterRegistry meterRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.chatMessageRepository = chatMessageRepository;
        this.chatMessageCounter = Counter.builder("chat.messages.total")
                .description("Total number of chat messages sent")
                .register(meterRegistry);
    }

    public void sendMessage(ChatMessageRequest request) {
        ChatMessage message = ChatMessage.builder()
                .channelId(request.getChannelId())
                .sender(request.getSender())
                .content(request.getContent())
                .type(ChatMessage.MessageType.CHAT)
                .build();

        chatMessageRepository.save(message);
        chatMessageCounter.increment();

        ChatMessageResponse response = ChatMessageResponse.from(message);
        messagingTemplate.convertAndSend("/topic/chat/" + request.getChannelId(), response);

        log.debug("Message sent to channel {}: {}", request.getChannelId(), request.getContent());
    }

    public void sendSystemMessage(String channelId, String sender, ChatMessage.MessageType type) {
        String content = switch (type) {
            case JOIN -> sender + "님이 입장했습니다.";
            case LEAVE -> sender + "님이 퇴장했습니다.";
            default -> "";
        };

        ChatMessage message = ChatMessage.builder()
                .channelId(channelId)
                .sender("SYSTEM")
                .content(content)
                .type(type)
                .build();

        ChatMessageResponse response = ChatMessageResponse.from(message);
        messagingTemplate.convertAndSend("/topic/chat/" + channelId, response);
    }

    public List<ChatMessageResponse> getRecentMessages(String channelId) {
        return chatMessageRepository.findTop50ByChannelIdOrderByCreatedAtDesc(channelId)
                .stream()
                .map(ChatMessageResponse::from)
                .toList();
    }
}
