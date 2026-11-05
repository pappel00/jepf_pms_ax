package com.platform.module.pms.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 텔레그램 Bot API 클라이언트. 봇 토큰은 설정(pms.notify.telegram.token, 환경변수 PMS_TELEGRAM_TOKEN)에서 읽는다.
 * 외부에서 PMS 로 들어오는 주소가 필요 없도록 웹훅이 아니라 getUpdates(TelegramPoller)로 메시지·버튼 클릭을 가져온다.
 */
@Component
public class TelegramClient {

    /** 메시지 아래 버튼 하나. data 는 눌렀을 때 돌아오는 값(최대 64바이트). */
    public record Button(String text, String data) {
    }

    /** 호출 실패(Telegram 오류 응답·네트워크 오류). */
    public static class TelegramException extends RuntimeException {
        public TelegramException(String message) {
            super(message);
        }
    }

    private final String token;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public TelegramClient(@Value("${pms.notify.telegram.token:}") String token) {
        this.token = token == null ? "" : token.trim();
    }

    public boolean hasToken() {
        return !token.isEmpty();
    }

    /** 봇 자신의 정보(토큰이 유효한지 확인). */
    public Map<String, Object> getMe() {
        return asMap(call("getMe", Map.of()));
    }

    /** rows 가 있으면 메시지 아래에 줄별 버튼으로 붙인다(줄마다 버튼 목록). */
    public void sendMessage(String chatId, String text, List<List<Button>> rows) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        if (rows != null && !rows.isEmpty()) {
            List<List<Map<String, String>>> keyboard = rows.stream()
                    .map(row -> row.stream().map(b -> Map.of("text", b.text(), "callback_data", b.data())).toList()).toList();
            body.put("reply_markup", Map.of("inline_keyboard", keyboard));
        }
        call("sendMessage", body);
    }

    /** 새 메시지(말 걸기·버튼 클릭)를 가져온다. offset 은 마지막으로 처리한 update_id + 1. */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getUpdates(long offset) {
        Object r = call("getUpdates", Map.of("offset", offset, "timeout", 0, "allowed_updates", List.of("message", "callback_query")));
        return r instanceof List<?> l ? (List<Map<String, Object>>) l : new ArrayList<>();
    }

    /** 버튼 클릭에 응답한다(눌린 버튼의 로딩 표시를 끄고 짧은 안내를 보여 준다). */
    public void answerCallbackQuery(String callbackQueryId, String text) {
        call("answerCallbackQuery", Map.of("callback_query_id", callbackQueryId, "text", text));
    }

    /** 메시지 내용을 바꾼다. reply_markup 을 주지 않으므로 달려 있던 버튼은 사라진다. */
    public void editMessageText(String chatId, long messageId, String text) {
        call("editMessageText", Map.of("chat_id", chatId, "message_id", messageId, "text", text));
    }

    private Object call(String method, Map<String, Object> body) {
        if (!hasToken()) {
            throw new TelegramException("텔레그램 봇 토큰이 설정되지 않았습니다(PMS_TELEGRAM_TOKEN).");
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.telegram.org/bot" + token + "/" + method))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(PmsJson.write(body)))
                .build();
        HttpResponse<String> res;
        try {
            res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new TelegramException("텔레그램 서버에 연결할 수 없습니다: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TelegramException("텔레그램 호출이 중단되었습니다.");
        }
        Map<String, Object> json;
        try {
            json = PmsJson.readMap(res.body());
        } catch (RuntimeException e) {
            throw new TelegramException("텔레그램 응답을 해석할 수 없습니다(HTTP " + res.statusCode() + ")");
        }
        if (!Boolean.TRUE.equals(json.get("ok"))) {
            throw new TelegramException("텔레그램 오류: " + json.getOrDefault("description", "HTTP " + res.statusCode()));
        }
        return json.get("result");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }
}
