package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 텔레그램 봇으로 들어온 메시지(「/start 코드」 연결)와 버튼 클릭(진척 입력)을 주기적으로 가져와 처리한다(getUpdates).
 * 외부에서 PMS 로 들어오는 주소가 필요 없어 개발 PC 에서도 동작한다. 같은 봇을 여러 서버가 동시에 가져오면 충돌하므로 서버는 한 대만
 * 켜 두거나, 다른 서버에서는 pms.notify.telegram.polling=false 로 끈다.
 */
@Component
public class TelegramPoller {

    private static final Logger log = LoggerFactory.getLogger(TelegramPoller.class);

    private final TelegramClient telegram;
    private final TelegramLinkService linkService;
    private final NotifyTokens tokens;
    private final NotifyCallbackService callbackService;
    private final boolean enabled;
    /** 직접 입력 대기 — 「✏️ 직접 입력」을 누른 chat 의 다음 숫자 메시지를 그 업무의 진척으로 받는다(서버 메모리, 10분). */
    private record ManualPending(String taskId, Long promptMessageId, Instant expires) {
    }

    private static final Pattern NUMBER = Pattern.compile("^(\\d{1,3})([.,]\\d)?\\s*%?$");
    private final Map<String, ManualPending> manual = new ConcurrentHashMap<>();
    private long offset = 0;
    private Instant lastErrorLog = Instant.EPOCH;

    public TelegramPoller(TelegramClient telegram, TelegramLinkService linkService, NotifyTokens tokens, NotifyCallbackService callbackService,
                          @Value("${pms.notify.telegram.polling:true}") boolean enabled) {
        this.telegram = telegram;
        this.linkService = linkService;
        this.tokens = tokens;
        this.callbackService = callbackService;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelay = 3000, initialDelay = 5000)
    public void poll() {
        if (!enabled || !telegram.hasToken()) {
            return;
        }
        try {
            for (Map<String, Object> update : telegram.getUpdates(offset)) {
                offset = Math.max(offset, ((Number) update.get("update_id")).longValue() + 1);
                try {
                    handle(update);
                } catch (RuntimeException e) {
                    log.warn("텔레그램 업데이트 처리 실패: {}", e.getMessage(), e);
                }
            }
        } catch (RuntimeException e) {
            // 네트워크 단절 등으로 계속 실패해도 로그가 쌓이지 않게 1분에 한 번만 남긴다.
            if (Instant.now().isAfter(lastErrorLog.plusSeconds(60))) {
                lastErrorLog = Instant.now();
                log.warn("텔레그램 메시지 가져오기 실패: {}", e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handle(Map<String, Object> update) {
        if (update.get("message") instanceof Map<?, ?> m) {
            handleMessage((Map<String, Object>) m);
        } else if (update.get("callback_query") instanceof Map<?, ?> c) {
            handleCallback((Map<String, Object>) c);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(Map<String, Object> msg) {
        Map<String, Object> chat = (Map<String, Object>) msg.get("chat");
        if (chat == null || !"private".equals(chat.get("type"))) {
            return; // 그룹·채널에서는 응답하지 않는다
        }
        String chatId = String.valueOf(chat.get("id"));
        String text = msg.get("text") == null ? "" : msg.get("text").toString().trim();
        if (text.startsWith("/start")) {
            String[] parts = text.split("\\s+");
            String reply = parts.length > 1
                    ? linkService.link(parts[1], chatId)
                    : "PMS 알림 봇입니다. PMS 인력 관리에서 발급한 연결 코드를 「/start 코드」 형태로 보내 주세요.";
            telegram.sendMessage(chatId, reply, null);
        } else if (manual.containsKey(chatId) && NUMBER.matcher(text).matches()) {
            applyManual(chatId, text);
        } else if (manual.containsKey(chatId) && !text.isEmpty()) {
            telegram.sendMessage(chatId, "진척률은 0~100 사이 숫자로 보내 주세요. (예: 37 또는 37.5)", null);
        } else if (!text.isEmpty()) {
            telegram.sendMessage(chatId, "PMS 알림 전용 봇입니다. 연결하려면 인력 관리에서 연결 코드를 발급받아 「/start 코드」를 보내 주세요.", null);
        }
    }

    /** 직접 입력한 숫자를 대기 중인 업무의 진척으로 입력한다. 규칙에 어긋나면 안내하고 대기는 유지한다. */
    private void applyManual(String chatId, String text) {
        ManualPending pending = manual.get(chatId);
        if (pending == null || pending.expires().isBefore(Instant.now())) {
            manual.remove(chatId);
            telegram.sendMessage(chatId, "입력 시간이 지났습니다. 알림의 「✏️ 직접 입력」 버튼을 다시 눌러 주세요.", null);
            return;
        }
        Matcher m = NUMBER.matcher(text);
        m.matches();
        BigDecimal value = new BigDecimal(m.group(1) + (m.group(2) == null ? "" : m.group(2).replace(',', '.')));
        Optional<TelegramLinkService.Owner> owner = linkService.findOwner(chatId);
        if (owner.isEmpty()) {
            manual.remove(chatId);
            telegram.sendMessage(chatId, "연결된 PMS 사용자를 찾을 수 없습니다.", null);
            return;
        }
        try {
            String done = callbackService.applyProgress(owner.get().tenantId(), owner.get().userId(), pending.taskId(), value, "TELEGRAM");
            manual.remove(chatId);
            telegram.sendMessage(chatId, done, null);
            if (pending.promptMessageId() != null) {
                telegram.editMessageText(chatId, pending.promptMessageId(), done);
            }
        } catch (BusinessException e) {
            telegram.sendMessage(chatId, "입력하지 못했습니다: " + e.getMessage() + "\n다시 숫자를 보내거나 PMS 에서 직접 입력해 주세요.", null);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleCallback(Map<String, Object> cq) {
        String id = String.valueOf(cq.get("id"));
        String chatId = String.valueOf(((Map<String, Object>) cq.get("from")).get("id"));
        String data = cq.get("data") == null ? null : cq.get("data").toString();
        Optional<TelegramLinkService.Owner> owner = linkService.findOwner(chatId);
        NotifyTokens.TelegramPayload p = owner.isPresent() ? tokens.verifyTelegram(chatId, data) : null;
        if (p == null) {
            telegram.answerCallbackQuery(id, "만료되었거나 유효하지 않은 버튼입니다. PMS 에서 직접 입력해 주세요.");
            return;
        }
        if (p.progressPct() < 0) { // 「✏️ 직접 입력」 — 다음 숫자 메시지를 기다린다
            Long promptId = cq.get("message") instanceof Map<?, ?> mm && mm.get("message_id") != null ? ((Number) mm.get("message_id")).longValue() : null;
            manual.put(chatId, new ManualPending(p.taskId(), promptId, Instant.now().plusSeconds(600)));
            telegram.answerCallbackQuery(id, "진척률을 숫자로 보내 주세요.");
            telegram.sendMessage(chatId, "✏️ 진척률(0~100)을 숫자로 보내 주세요. (예: 37 또는 37.5)", null);
            return;
        }
        String reply;
        boolean ok = false;
        try {
            reply = callbackService.applyProgress(owner.get().tenantId(), owner.get().userId(), p.taskId(), p.progressPct(), "TELEGRAM");
            ok = true;
        } catch (BusinessException e) {
            reply = "입력하지 못했습니다: " + e.getMessage();
        } catch (RuntimeException e) {
            log.warn("텔레그램 버튼 처리 실패: {}", e.getMessage(), e);
            reply = "처리 중 오류가 발생했습니다. PMS 에서 직접 입력해 주세요.";
        }
        telegram.answerCallbackQuery(id, reply);
        Object message = cq.get("message");
        if (ok && message instanceof Map<?, ?> mm && mm.get("message_id") != null) {
            telegram.editMessageText(chatId, ((Number) mm.get("message_id")).longValue(), reply);
        }
    }
}
