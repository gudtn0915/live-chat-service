# Live Chat Service

대규모 실시간 채팅 서비스 - 부하 테스트 기반 아키텍처 성능 최적화 프로젝트

> 구독자 수가 많은 스트리머 방송 시 수만 명의 동시 시청자가 보내는 채팅을 안정적으로 처리하기 위한 서비스입니다.
> 아키텍처를 단계적으로 진화시키며 각 단계별 부하 테스트를 통해 성능 개선을 검증합니다.

---

## 기술 스택

| 영역 | 기술 | 선정 이유 |
|------|------|-----------|
| Language | Java 21 | Virtual Threads, Record, Pattern Matching |
| Framework | Spring Boot 3.4.x | WebSocket/STOMP 네이티브 지원 |
| WebSocket | Spring WebSocket + STOMP | 실시간 양방향 통신, 토픽 기반 메시지 라우팅 |
| Message Broker | Redis Pub/Sub → Kafka | 단계별 확장 (단일 → 분산 → 고처리량) |
| Cache | Redis 7.x | 세션 관리, 채널 정보 캐싱, Rate Limiting |
| Database | MySQL 8.x | 채널/사용자/채팅 이력 영속화 |
| Load Test | k6 | WebSocket 부하 테스트, JavaScript 기반 시나리오 |
| Monitoring | Micrometer + Prometheus + Grafana | 실시간 메트릭 수집 및 시각화 |
| Container | Docker + Docker Compose | 로컬 인프라 환경 구성 |
| Build | Gradle (Kotlin DSL) | 의존성 관리 |

---

## 아키텍처 개요

```
┌─────────────┐     WebSocket(STOMP)     ┌──────────────────┐
│   Client     │◄──────────────────────►│  Spring Boot App  │
│  (Browser)   │                         │                  │
└─────────────┘                         │  ┌────────────┐  │
                                        │  │ ChatService │  │
┌─────────────┐     WebSocket(STOMP)     │  └─────┬──────┘  │
│   Client     │◄──────────────────────►│        │         │
└─────────────┘                         │        ▼         │
                                        │  ┌────────────┐  │
┌─────────────┐     WebSocket(STOMP)     │  │  Message   │  │
│   Client     │◄──────────────────────►│  │  Broker    │  │
└─────────────┘                         │  └─────┬──────┘  │
                                        └────────┼─────────┘
                                                 │
                                    ┌────────────┼────────────┐
                                    ▼            ▼            ▼
                              ┌──────────┐ ┌──────────┐ ┌──────────┐
                              │  MySQL   │ │  Redis   │ │  Kafka   │
                              │ (영속화)  │ │ (캐시)   │ │ (Phase3) │
                              └──────────┘ └──────────┘ └──────────┘
```

---

## 프로젝트 구조

```
live-chat-service/
├── build.gradle.kts
├── settings.gradle.kts
├── docker-compose.yml
├── README.md
├── k6/
│   └── chat-load-test.js              # 부하 테스트 스크립트
├── monitoring/
│   └── prometheus.yml                  # Prometheus 수집 설정
└── src/main/java/com/livechat/
    ├── LiveChatApplication.java
    ├── config/
    │   ├── WebSocketConfig.java        # STOMP WebSocket 설정
    │   └── RedisConfig.java            # Redis 직렬화 설정
    ├── chat/
    │   ├── controller/
    │   │   └── ChatController.java     # STOMP 메시지 핸들러 + REST API
    │   ├── service/
    │   │   └── ChatService.java        # 채팅 비즈니스 로직
    │   ├── domain/
    │   │   ├── ChatMessage.java        # 채팅 메시지 엔티티
    │   │   └── ChatMessageRepository.java
    │   └── dto/
    │       ├── ChatMessageRequest.java
    │       └── ChatMessageResponse.java
    ├── channel/
    │   ├── controller/
    │   │   └── ChannelController.java  # 채널 관리 REST API
    │   ├── service/
    │   │   └── ChannelService.java     # 채널 + 시청자 수 관리
    │   └── domain/
    │       ├── Channel.java            # 채널 엔티티
    │       └── ChannelRepository.java
    └── common/
        └── handler/
            └── StompEventHandler.java  # 연결/구독/해제 이벤트 처리
```

---

## 메시지 흐름

```
1. 클라이언트 → ws://localhost:8080/ws/chat 으로 WebSocket 연결
2. STOMP CONNECT → 서버 CONNECTED 응답
3. SUBSCRIBE /topic/chat/{channelId} → 채널 구독 (입장 알림 브로드캐스트)
4. SEND /app/chat/send → ChatController → ChatService → DB 저장 + 브로드캐스트
5. 서버 → /topic/chat/{channelId} 구독자 전체에게 MESSAGE 프레임 전송
6. DISCONNECT → 퇴장 알림 브로드캐스트
```

---

## 빠른 시작

### 1. 인프라 구동

```bash
docker-compose up -d
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 테스트 채널 생성

```bash
# 채널 생성
curl -X POST http://localhost:8080/api/channels \
  -H "Content-Type: application/json" \
  -d '{"channelId": "test-channel-1", "streamerName": "테스트스트리머"}'

# 방송 시작
curl -X POST http://localhost:8080/api/channels/test-channel-1/start
```

### 4. 부하 테스트 실행

```bash
# k6 설치 (macOS)
brew install k6

# 부하 테스트 실행
k6 run k6/chat-load-test.js
```

### 5. 모니터링 확인

- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Actuator Metrics**: http://localhost:8080/actuator/prometheus

---

## 주요 메트릭

| 메트릭 | 설명 |
|--------|------|
| `chat.messages.total` | 전송된 총 채팅 메시지 수 |
| `chat.connections.active` | 현재 활성 WebSocket 연결 수 |
| `ws_messages_sent` (k6) | 부하 테스트 시 전송한 메시지 수 |
| `ws_message_latency` (k6) | 메시지 전송 → 수신 레이턴시 (p95, p99) |
| `ws_connection_errors` (k6) | WebSocket 연결 에러율 |

---

## 단계별 구현 로드맵

### Phase 1: 단일 서버 기본 채팅 (현재 구현 완료)

**목표**: 기본 동작하는 실시간 채팅 + 성능 베이스라인 측정

- [x] WebSocket + STOMP 기반 실시간 채팅
- [x] 채널 입장/퇴장 알림
- [x] 채팅 메시지 DB 저장
- [x] k6 부하 테스트 스크립트
- [x] Prometheus + Grafana 모니터링
- [x] Micrometer 커스텀 메트릭 (메시지 수, 활성 연결 수)

**예상 성능 한계**: 단일 서버의 In-Memory Broker 한계로 동시 접속 수천 명 수준에서 병목 발생

---

### Phase 2: Redis Pub/Sub 스케일아웃

**목표**: 다중 인스턴스 배포 + 인스턴스 간 메시지 공유

구현 항목:
- [ ] Redis Pub/Sub 기반 메시지 브로커 전환
  - `SimpleBroker` → Redis 기반 외부 브로커로 교체
  - 인스턴스 간 메시지 동기화
- [ ] Redis 기반 세션 관리
  - 각 인스턴스의 WebSocket 세션 정보를 Redis에 저장
  - 인스턴스 장애 시 세션 복구
- [ ] Rate Limiting 구현
  - Redis의 Sliding Window Counter 패턴 적용
  - 사용자별 초당 메시지 제한 (예: 5msg/sec)
- [ ] 다중 인스턴스 부하 테스트
  - 2~3개 인스턴스 배포 후 k6 테스트
  - Phase 1 대비 처리량 비교

**아키텍처 변경점**:
```
Client ──► Nginx(LB) ──► App Instance 1 ──┐
                    ──► App Instance 2 ──┤──► Redis Pub/Sub
                    ──► App Instance 3 ──┘
```

**검증 포인트**:
- 단일 인스턴스 대비 처리량이 선형적으로 증가하는가?
- 인스턴스 간 메시지 전달 지연은 얼마인가?
- Rate Limiting이 성능에 미치는 영향은?

---

### Phase 3: Kafka 도입

**목표**: 대규모 트래픽 처리를 위한 메시지 큐 아키텍처 도입

구현 항목:
- [ ] Kafka Producer/Consumer 구현
  - 채팅 메시지를 Kafka Topic으로 발행
  - Consumer Group으로 메시지 소비 및 WebSocket 브로드캐스트
- [ ] 메시지 영속성 및 순서 보장
  - channelId를 Partition Key로 사용
  - 같은 채널의 메시지 순서 보장
- [ ] 채팅 이력 비동기 저장
  - DB 저장을 Kafka Consumer에서 비동기 처리
  - 실시간 경로에서 DB I/O 제거
- [ ] 장애 복구 (Replayability)
  - 서버 재시작 시 Kafka offset 기반 메시지 복구

**아키텍처 변경점**:
```
Client ──► App ──► Kafka Topic ──► Consumer Group ──► WebSocket Broadcast
                              ──► Consumer Group ──► DB 저장
```

**검증 포인트**:
- Kafka 도입 전/후 메시지 처리량 비교
- DB I/O를 비동기로 분리했을 때 레이턴시 개선 효과
- Kafka Partition 수에 따른 처리량 변화

---

### Phase 4: 고급 최적화

**목표**: Java 21 기능 활용 + 고급 최적화로 극한 성능 달성

구현 항목:
- [ ] Virtual Threads 적용
  - `spring.threads.virtual.enabled=true`
  - Platform Thread 대비 동시 연결 수용량 비교
- [ ] 메시지 배칭 (Batching)
  - 100ms 윈도우 내 메시지를 모아서 한 번에 전송
  - 대량 채팅 시 WebSocket 프레임 수 감소
  - 클라이언트 렌더링 부하 감소
- [ ] Backpressure 처리
  - 클라이언트 수신 속도에 맞춰 전송 조절
  - 느린 클라이언트 감지 및 연결 정리
- [ ] Connection 최적화
  - WebSocket 커넥션 풀 튜닝
  - Idle Connection 타임아웃 관리
  - 하트비트 주기 최적화
- [ ] 메모리 최적화
  - 메시지 객체 재활용 (Object Pool)
  - Off-heap 버퍼 활용

**검증 포인트**:
- Virtual Threads: 동일 하드웨어에서 동시 연결 수가 얼마나 늘어나는가?
- 메시지 배칭: 1만 명 채널에서 초당 메시지 전송량 변화
- Backpressure: 느린 클라이언트가 전체 시스템에 미치는 영향 차단 여부

---

## 부하 테스트 시나리오

### 기본 시나리오 (Phase 1)
| 단계 | 동시 사용자 | 지속 시간 | 목표 |
|------|------------|-----------|------|
| Warm-up | 0 → 100 | 30초 | 워밍업 |
| Sustain | 100 | 1분 | 안정성 확인 |
| Ramp-up | 100 → 500 | 30초 | 부하 증가 |
| Sustain | 500 | 1분 | 중간 부하 |
| Peak | 500 → 1000 | 30초 | 최대 부하 |
| Sustain | 1000 | 1분 | 피크 유지 |
| Cool-down | 1000 → 0 | 30초 | 정리 |

### 성능 목표
| 지표 | Phase 1 | Phase 2 | Phase 3 | Phase 4 |
|------|---------|---------|---------|---------|
| 동시 접속 | 1,000 | 5,000 | 10,000 | 50,000+ |
| 메시지 처리량 | 1,000/s | 5,000/s | 20,000/s | 100,000/s |
| 레이턴시 p95 | < 500ms | < 200ms | < 100ms | < 50ms |
| 레이턴시 p99 | < 1,000ms | < 500ms | < 200ms | < 100ms |
| 에러율 | < 10% | < 5% | < 1% | < 0.1% |

---

## API Reference

### REST API

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/channels` | 채널 생성 |
| POST | `/api/channels/{channelId}/start` | 방송 시작 |
| POST | `/api/channels/{channelId}/stop` | 방송 종료 |
| GET | `/api/channels/live` | 라이브 중인 채널 목록 |
| GET | `/api/channels/{channelId}/viewers` | 시청자 수 조회 |
| GET | `/api/chat/{channelId}/messages` | 최근 채팅 50건 조회 |

### WebSocket (STOMP)

| 유형 | Destination | 설명 |
|------|-------------|------|
| CONNECT | `/ws/chat` | WebSocket 연결 |
| SUBSCRIBE | `/topic/chat/{channelId}` | 채널 채팅 구독 |
| SEND | `/app/chat/send` | 채팅 메시지 전송 |

### 메시지 형식

**요청 (ChatMessageRequest)**
```json
{
  "channelId": "channel-1",
  "sender": "user123",
  "content": "안녕하세요!"
}
```

**응답 (ChatMessageResponse)**
```json
{
  "channelId": "channel-1",
  "sender": "user123",
  "content": "안녕하세요!",
  "type": "CHAT",
  "createdAt": "2026-02-15T12:00:00"
}
```
