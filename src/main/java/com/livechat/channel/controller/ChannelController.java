package com.livechat.channel.controller;

import com.livechat.channel.domain.Channel;
import com.livechat.channel.service.ChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping
    public ResponseEntity<Channel> createChannel(@RequestBody Map<String, String> request) {
        Channel channel = channelService.createChannel(
                request.get("channelId"),
                request.get("streamerName")
        );
        return ResponseEntity.ok(channel);
    }

    @PostMapping("/{channelId}/start")
    public ResponseEntity<Void> startBroadcast(@PathVariable String channelId) {
        channelService.startBroadcast(channelId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{channelId}/stop")
    public ResponseEntity<Void> stopBroadcast(@PathVariable String channelId) {
        channelService.stopBroadcast(channelId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/live")
    public ResponseEntity<List<Channel>> getLiveChannels() {
        return ResponseEntity.ok(channelService.getAllLiveChannels());
    }

    @GetMapping("/{channelId}/viewers")
    public ResponseEntity<Map<String, Integer>> getViewerCount(@PathVariable String channelId) {
        return ResponseEntity.ok(Map.of("viewerCount", channelService.getViewerCount(channelId)));
    }
}
