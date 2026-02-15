package com.livechat.channel.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "channel")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String channelId;

    @Column(nullable = false, length = 100)
    private String streamerName;

    @Column(nullable = false)
    private boolean live;

    @Column(nullable = false)
    private int viewerCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Channel(String channelId, String streamerName) {
        this.channelId = channelId;
        this.streamerName = streamerName;
        this.live = false;
        this.viewerCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public void goLive() {
        this.live = true;
    }

    public void goOffline() {
        this.live = false;
        this.viewerCount = 0;
    }

    public void incrementViewerCount() {
        this.viewerCount++;
    }

    public void decrementViewerCount() {
        if (this.viewerCount > 0) {
            this.viewerCount--;
        }
    }
}
