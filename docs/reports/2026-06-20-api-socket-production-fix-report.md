# API Socket Production Fix Report - 2026-06-20

## Scope

- Reviewed backend notification push flow from Spring Boot API to RTC.
- Investigated why socket-related features work locally but fail in production behind VPS + Traefik + CloudFront.
- Applied production-hardening changes in API configuration.

## Root Cause Assessment

The backend does not open WebSocket connections directly. It pushes notification events to RTC through HTTP endpoints:

- `POST /internal/notify`
- `POST /internal/unread-counts`

So "socket pooling" at the backend should be interpreted as HTTP connection pooling for the API -> RTC hop, not WebSocket pooling.

The original API `WebClient` configuration used generic timeouts but no explicit connection pool tuning. In production, repeated notification bursts can create avoidable connection churn and amplify latency or transient failures when the RTC service is behind proxy layers.

## Changes Applied

- Added explicit Reactor Netty connection pooling for the shared `WebClient`.
- Added configurable socket HTTP settings in `application.yml`:
  - `socket.http.connect-timeout-ms`
  - `socket.http.response-timeout-ms`
  - `socket.http.pool.max-connections`
  - `socket.http.pool.pending-acquire-timeout-ms`
  - `socket.http.pool.max-idle-time-ms`
  - `socket.http.pool.max-life-time-ms`

Files changed:

- `src/main/java/com/hcmute/careergraph/config/app/AppConfig.java`
- `src/main/resources/application.yml`

## Production Interpretation

- Switching the API -> RTC internal push path to connection pooling is correct and production-appropriate.
- This improves reuse of outbound HTTP connections and reduces handshake overhead.
- This does not solve frontend WebSocket upgrade failures by itself; that issue lives in the browser -> CloudFront -> Traefik -> RTC path.

## Validation

- `mvn -q -DskipTests compile` passed on 2026-06-20.

## Risks Still Present

- The current RTC architecture keeps room/chat/notification connection state in memory.
- If RTC is scaled to more than one instance later, events and room state will fragment unless a shared adapter such as Redis is introduced.
- If API points to a public RTC URL through CloudFront instead of an internal container/network URL, failures can still be introduced by CDN/proxy behavior.

## Recommended Production Configuration

- Point `SOCKET_SERVER_URL` to the internal RTC service address on the Docker network when possible, not the public CDN endpoint.
- Keep the new HTTP pool settings enabled in production.
- Monitor `SocketNotificationPusher` warning logs for:
  - connect timeout
  - response timeout
  - 401 from wrong internal API key
  - 502/504 from proxy chain

## Senior Test Review

- The backend compile check is clean after the change.
- There is still no automated integration test in this repo that proves the full API -> RTC -> browser notification path under proxy conditions.

## Recommendation

Production readiness is improved by this change, but full closure requires paired frontend transport fallback and infra verification on RTC routing.
