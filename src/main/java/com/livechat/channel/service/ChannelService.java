package com.livechat.channel.service;

import com.livechat.channel.domain.Channel;
import com.livechat.channel.domain.ChannelRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ConcurrentHashMap<String, AtomicInteger> activeConnections = new ConcurrentHashMap<>();

    public ChannelService(ChannelRepository channelRepository, MeterRegistry meterRegistry) {
        this.channelRepository = channelRepository;

        Gauge.builder("chat.connections.active", activeConnections,
                        map -> map.values().stream().mapToInt(AtomicInteger::get).sum())
                .description("Total active WebSocket connections")
                .register(meterRegistry);
    }

    @Transactional
    public Channel createChannel(String channelId, String streamerName) {
        Channel channel = Channel.builder()
                .channelId(channelId)
                .streamerName(streamerName)
                .build();
        return channelRepository.save(channel);
    }

    @Transactional
    public void startBroadcast(String channelId) {
        Channel channel = getChannel(channelId);
        channel.goLive();
        log.info("Channel {} started broadcasting", channelId);
    }

    @Transactional
    public void stopBroadcast(String channelId) {
        Channel channel = getChannel(channelId);
        channel.goOffline();
        activeConnections.remove(channelId);
        log.info("Channel {} stopped broadcasting", channelId);
    }

    public void addViewer(String channelId) {
        activeConnections.computeIfAbsent(channelId, k -> new AtomicInteger(0)).incrementAndGet();
        log.debug("Viewer joined channel {}, active: {}", channelId,
                activeConnections.get(channelId).get());
    }

    public void removeViewer(String channelId) {
        AtomicInteger count = activeConnections.get(channelId);
        if (count != null && count.decrementAndGet() <= 0) {
            activeConnections.remove(channelId);
        }
        log.debug("Viewer left channel {}", channelId);
    }

    public int getViewerCount(String channelId) {
        AtomicInteger count = activeConnections.get(channelId);
        return count != null ? count.get() : 0;
    }

    public Channel getChannel(String channelId) {
        return channelRepository.findByChannelId(channelId)
                .orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelId));
    }

    public List<Channel> getAllLiveChannels() {
        return channelRepository.findAll().stream()
                .filter(Channel::isLive)
                .toList();
    }
}
