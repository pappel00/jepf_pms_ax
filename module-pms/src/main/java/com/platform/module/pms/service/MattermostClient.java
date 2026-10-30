package com.platform.module.pms.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Mattermost REST API(v4) 클라이언트. 서버 주소와 봇 토큰은 설정(pms.notify.mattermost.*)에서 읽는다.
 * 토큰은 환경변수 PMS_MATTERMOST_TOKEN 으로 넣고 소스·DB에 두지 않는다. 토큰이 없으면 서버 상태 확인(ping)만 된다.
 */
@Component
public class MattermostClient {

    /** Mattermost 사용자 한 명. */
    public record MmUser(String id, String username, String email) {
    }

    /** 호출 실패(HTTP 오류·네트워크 오류). */
    public static class MattermostException extends RuntimeException {
        private final int status;

        public MattermostException(int status, String message) {
            super(message);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }

    private final String baseUrl;
    private final String token;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public MattermostClient(@Value("${pms.notify.mattermost.base-url:https://mmtt.dxworks.kr}") String baseUrl,
                            @Value("${pms.notify.mattermost.token:}") String token) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token == null ? "" : token.trim();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public boolean hasToken() {
        return !token.isEmpty();
    }

    /** 서버 응답 여부와 버전. 인증이 필요 없다. */
    public String ping() {
        HttpResponse<String> res = send(request("/api/v4/system/ping", false).GET().build());
        if (res.statusCode() != 200) {
            throw new MattermostException(res.statusCode(), "Mattermost 서버가 정상 응답하지 않습니다(HTTP " + res.statusCode() + ")");
        }
        return res.headers().firstValue("X-Version-Id").orElse("");
    }

    /** 토큰의 주인(봇) 정보. 토큰이 유효한지 확인하는 데 쓴다. */
    public MmUser me() {
        return toUser(get("/api/v4/users/me"));
    }

    public Optional<MmUser> findByEmail(String email) {
        return findUser("/api/v4/users/email/" + enc(email));
    }

    public Optional<MmUser> findByUsername(String username) {
        return findUser("/api/v4/users/username/" + enc(username));
    }

    /** 봇과 사용자의 1:1(DM) 대화방에 메시지를 보낸다. */
    public void sendDirect(String botUserId, String targetUserId, String text) {
        Map<String, Object> channel = post("/api/v4/channels/direct", List.of(botUserId, targetUserId));
        post("/api/v4/posts", Map.of("channel_id", String.valueOf(channel.get("id")), "message", text));
    }

    private Optional<MmUser> findUser(String path) {
        HttpResponse<String> res = send(request(path, true).GET().build());
        if (res.statusCode() == 404) {
            return Optional.empty();
        }
        return Optional.of(toUser(parse(res)));
    }

    private Map<String, Object> get(String path) {
        return parse(send(request(path, true).GET().build()));
    }

    private Map<String, Object> post(String path, Object body) {
        HttpRequest req = request(path, true)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(PmsJson.write(body)))
                .build();
        return parse(send(req));
    }

    private HttpRequest.Builder request(String path, boolean auth) {
        if (auth && !hasToken()) {
            throw new MattermostException(0, "봇 토큰이 설정되지 않았습니다(PMS_MATTERMOST_TOKEN).");
        }
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(10));
        if (auth) {
            b.header("Authorization", "Bearer " + token);
        }
        return b;
    }

    private HttpResponse<String> send(HttpRequest req) {
        try {
            return http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new MattermostException(0, "Mattermost 서버에 연결할 수 없습니다: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MattermostException(0, "Mattermost 호출이 중단되었습니다.");
        }
    }

    private Map<String, Object> parse(HttpResponse<String> res) {
        if (res.statusCode() / 100 != 2) {
            String detail = res.body();
            try {
                Object m = PmsJson.readMap(res.body()).get("message");
                if (m != null) {
                    detail = m.toString();
                }
            } catch (RuntimeException ignored) {
                // 본문이 JSON 이 아니면 원문 그대로 쓴다.
            }
            throw new MattermostException(res.statusCode(), "Mattermost 오류(HTTP " + res.statusCode() + "): " + detail);
        }
        return PmsJson.readMap(res.body());
    }

    private static MmUser toUser(Map<String, Object> m) {
        return new MmUser(String.valueOf(m.get("id")), String.valueOf(m.get("username")),
                m.get("email") == null ? null : String.valueOf(m.get("email")));
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
