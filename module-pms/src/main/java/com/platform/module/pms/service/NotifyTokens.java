package com.platform.module.pms.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Mattermost 버튼에 심는 서명 토큰. 콜백은 JWT 없이 들어오므로, "누가 어느 업무를 몇 %로" 라는 내용과 유효기간을
 * HMAC-SHA256 으로 서명해 위조·재사용 범위를 막는다. 비밀키는 pms.notify.callback-secret(환경변수 PMS_NOTIFY_SECRET),
 * 없으면 JWT 비밀키에서 파생한다.
 */
@Component
public class NotifyTokens {

    /** 토큰 내용. */
    public record Payload(String tenantId, String userId, String taskId, int progressPct) {
    }

    private static final long VALID_SECONDS = 3L * 24 * 3600;

    private final byte[] key;

    public NotifyTokens(@Value("${pms.notify.callback-secret:}") String secret,
                        @Value("${framework.security.jwt.secret:}") String jwtSecret) {
        String base = secret == null || secret.isBlank() ? "pms-notify:" + jwtSecret : secret;
        this.key = base.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(String tenantId, String userId, String taskId, int progressPct) {
        String body = String.join("|", tenantId, userId, taskId, String.valueOf(progressPct),
                String.valueOf(Instant.now().getEpochSecond() + VALID_SECONDS));
        Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
        return enc.encodeToString(body.getBytes(StandardCharsets.UTF_8)) + "." + enc.encodeToString(hmac(body));
    }

    /** 서명·유효기간을 확인해 내용을 돌려준다. 잘못되었거나 만료되면 null. */
    public Payload verify(String token) {
        if (token == null) {
            return null;
        }
        int dot = token.indexOf('.');
        if (dot < 1) {
            return null;
        }
        try {
            String body = new String(Base64.getUrlDecoder().decode(token.substring(0, dot)), StandardCharsets.UTF_8);
            byte[] sig = Base64.getUrlDecoder().decode(token.substring(dot + 1));
            if (!MessageDigest.isEqual(sig, hmac(body))) {
                return null;
            }
            String[] p = body.split("\\|", -1);
            if (p.length != 5 || Long.parseLong(p[4]) < Instant.now().getEpochSecond()) {
                return null;
            }
            return new Payload(p[0], p[1], p[2], Integer.parseInt(p[3]));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private byte[] hmac(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
