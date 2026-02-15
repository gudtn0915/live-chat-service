package com.livechat.common.handler;

import com.livechat.channel.service.ChannelService;
import com.livechat.chat.domain.ChatMessage;
import com.livechat.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventHandler {

    private final ChannelService channelService;
    private final ChatService chatService;

    // sessionId -> {channelId, sender}
    private final Map<String, SessionInfo> sessionMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.debug("WebSocket connected: sessionId={}", accessor.getSessionId());
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        String sessionId = accessor.getSessionId();

        if (destination != null && destination.startsWith("/topic/chat/")) {
            String channelId = destination.replace("/topic/chat/", "");
            String sender = accessor.getFirstNativeHeader("sender");
            if (sender == null) {
                sender = "anonymous";
            }

            sessionMap.put(sessionId, new SessionInfo(channelId, sender));
            channelService.addViewer(channelId);
            chatService.sendSystemMessage(channelId, sender, ChatMessage.MessageType.JOIN);

            log.info("User {} subscribed to channel {}", sender, channelId);
        }
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        handleUserLeave(accessor.getSessionId());
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        handleUserLeave(accessor.getSessionId());
    }

    private void handleUserLeave(String sessionId) {
        SessionInfo info = sessionMap.remove(sessionId);
        if (info != null) {
            channelService.removeViewer(info.channelId());
            chatService.sendSystemMessage(info.channelId(), info.sender(), ChatMessage.MessageType.LEAVE);
            log.info("User {} left channel {}", info.sender(), info.channelId());
        }
    }

    private record SessionInfo(String channelId, String sender) {}
}
