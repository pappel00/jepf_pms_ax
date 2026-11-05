package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.pms.domain.UserProfile;
import com.platform.module.pms.dto.NotifyDto;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.module.pms.repository.UserProfileRepository;
import com.platform.module.tenant.domain.Company;
import com.platform.module.tenant.repository.CompanyRepository;
import com.platform.module.user.domain.User;
import com.platform.module.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 텔레그램 사용자 연결 — 인력 관리에서 연결 코드를 발급하고, 사용자가 봇에게 「/start 코드」를 보내면 그 대화의 chat ID 를
 * 사용자 프로필(telegram_chat_id)에 저장한다. 코드는 10분만 유효하고 서버 메모리에 둔다(재기동하면 다시 발급).
 * 봇은 회사(테넌트)와 무관하게 하나라서, 버튼 클릭처럼 로그인 정보가 없는 요청은 chat ID 로 회사·사용자를 찾는다.
 */
@Service
public class TelegramLinkService {

    /** chat ID 의 주인. */
    public record Owner(String tenantId, String userId) {
    }

    private record Pending(String tenantId, String userId, Instant expires) {
    }

    private static final int VALID_MINUTES = 10;
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final TelegramClient telegram;
    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final TransactionTemplate tx;
    private volatile String botUsername;

    public TelegramLinkService(TelegramClient telegram, UserProfileRepository profileRepository, UserRepository userRepository,
                               CompanyRepository companyRepository, PlatformTransactionManager txManager) {
        this.telegram = telegram;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.tx = new TransactionTemplate(txManager);
    }

    /** 봇 사용자명(@ 없이). 연결 링크(t.me/봇?start=코드)를 만드는 데 쓴다. */
    public String botUsername() {
        if (botUsername == null) {
            botUsername = String.valueOf(telegram.getMe().get("username"));
        }
        return botUsername;
    }

    /** 현재 로그인한 회사의 사용자에게 연결 코드를 발급한다. */
    public NotifyDto.LinkCode issue(String userId) {
        userRepository.findById(userId).filter(u -> "N".equals(u.getDeletedYn()))
                .orElseThrow(() -> new BusinessException(PmsErrorCode.STAFF_INVALID, "존재하지 않는 사용자입니다."));
        Instant now = Instant.now();
        pending.values().removeIf(p -> p.expires().isBefore(now));
        pending.values().removeIf(p -> p.userId().equals(userId)); // 같은 사람의 이전 코드는 무효
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
            code = sb.toString();
        } while (pending.containsKey(code));
        pending.put(code, new Pending(TenantContext.getTenantId(), userId, now.plusSeconds(VALID_MINUTES * 60L)));
        return new NotifyDto.LinkCode(code, "https://t.me/" + botUsername() + "?start=" + code, VALID_MINUTES);
    }

    /** 봇이 받은 「/start 코드」 처리. 사용자에게 돌려줄 답장 문구를 돌려준다. */
    public String link(String code, String chatId) {
        Pending p = code == null ? null : pending.remove(code.trim().toUpperCase());
        if (p == null || p.expires().isBefore(Instant.now())) {
            return "연결 코드가 없거나 만료되었습니다. PMS 인력 관리에서 연결 코드를 다시 발급해 주세요.";
        }
        try {
            TenantContext.setTenantId(p.tenantId());
            return tx.execute(status -> {
                User user = userRepository.findById(p.userId()).orElse(null);
                if (user == null) {
                    return "연결할 사용자를 찾을 수 없습니다.";
                }
                boolean usedByOther = profileRepository.findAllByTelegramChatIdAndDeletedYn(chatId, "N").stream()
                        .anyMatch(x -> !x.getUserId().equals(p.userId()));
                if (usedByOther) {
                    return "이 텔레그램 계정은 이미 다른 사용자에 연결되어 있습니다. 관리자에게 연결 해제를 요청해 주세요.";
                }
                UserProfile profile = profileRepository.findByUserIdAndDeletedYn(p.userId(), "N")
                        .orElseGet(() -> UserProfile.create().withUserId(p.userId()).withOrgTypeCode("자사"));
                profile.withTelegramChatId(chatId);
                String channel = profile.getNotifyChannelCode();
                if (channel == null || channel.isBlank() || "미설정".equals(channel)) {
                    profile.withNotifyChannelCode("텔레그램");
                }
                profileRepository.save(profile);
                return "✅ " + user.getUserName() + "님의 텔레그램이 PMS 알림에 연결되었습니다. 이제 업무 알림이 이 대화로 옵니다.";
            });
        } finally {
            TenantContext.clear();
        }
    }

    /** chat ID 로 회사·사용자를 찾는다(로그인 정보가 없는 버튼 클릭 처리용). */
    public Optional<Owner> findOwner(String chatId) {
        for (Company c : companyRepository.findAll()) {
            if (!"N".equals(c.getDeletedYn())) {
                continue;
            }
            try {
                TenantContext.setTenantId(c.getId());
                List<UserProfile> found = profileRepository.findAllByTelegramChatIdAndDeletedYn(chatId, "N");
                if (!found.isEmpty()) {
                    return Optional.of(new Owner(c.getId(), found.get(0).getUserId()));
                }
            } finally {
                TenantContext.clear();
            }
        }
        return Optional.empty();
    }

    /** 현재 회사 사용자의 텔레그램 연결을 끊는다(chat ID 삭제). */
    public void unlink(String userId) {
        tx.executeWithoutResult(status -> profileRepository.findByUserIdAndDeletedYn(userId, "N").ifPresent(p -> {
            p.withTelegramChatId(null);
            profileRepository.save(p);
        }));
    }
}
