package com.livechat.chat.controller;

import com.livechat.chat.dto.ChatMessageRequest;
import com.livechat.chat.dto.ChatMessageResponse;
import com.livechat.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * STOMP 메시지 핸들러
     * 클라이언트에서 /app/chat/send 로 메시지 전송 → /topic/chat/{channelId} 로 브로드캐스트
     */
    @MessageMapping("/chat/send")
    public void sendMessage(ChatMessageRequest request) {
        chatService.sendMessage(request);
    }

    /**
     * 채널의 최근 채팅 메시지 조회 (입장 시 이전 메시지 로딩용)
     */
    @GetMapping("/api/chat/{channelId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getRecentMessages(@PathVariable String channelId) {
        return ResponseEntity.ok(chatService.getRecentMessages(channelId));
    }
}
