package com.platform.module.pms.service;

import com.platform.module.pms.domain.NotifyLog;
import com.platform.module.pms.domain.UserProfile;
import com.platform.module.pms.dto.NotifyDto;
import com.platform.module.pms.repository.NotifyLogRepository;
import com.platform.module.pms.repository.UserProfileRepository;
import com.platform.module.pms.service.MattermostClient.MattermostException;
import com.platform.module.pms.service.MattermostClient.MmUser;
import com.platform.module.user.domain.User;
import com.platform.module.user.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 발송 — 사용자의 알림 채널(pms_user_profile.notify_channel_code)에 맞춰 보낸다.
 * 지금은 Mattermost 만 연결되어 있고, 텔레그램·이메일(협력사 인력용)은 아직 연결 전이라 건너뛰고 이력에 남긴다.
 * 발송 실패가 호출한 업무(이슈 저장 등)를 막지 않도록 send 는 예외를 던지지 않고 결과를 돌려준다.
 */
@Service
@Transactional(readOnly = true)
public class NotifyService {

    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";
    public static final String SKIPPED = "SKIPPED";

    private final MattermostClient mattermost;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final NotifyLogRepository logRepository;

    public NotifyService(MattermostClient mattermost, UserRepository userRepository,
                         UserProfileRepository profileRepository, NotifyLogRepository logRepository) {
        this.mattermost = mattermost;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.logRepository = logRepository;
    }

    /** 연결 상태 — 서버 응답, 토큰 유무, 봇 인증까지 차례로 확인한다. */
    public NotifyDto.Status status() {
        String base = mattermost.getBaseUrl();
        String version;
        try {
            version = mattermost.ping();
        } catch (MattermostException e) {
            return new NotifyDto.Status(base, false, null, mattermost.hasToken(), false, null, e.getMessage());
        }
        if (!mattermost.hasToken()) {
            return new NotifyDto.Status(base, true, version, false, false, null,
                    "서버는 정상입니다. 봇 토큰(PMS_MATTERMOST_TOKEN)을 설정하면 알림을 보낼 수 있습니다.");
        }
        try {
            MmUser bot = mattermost.me();
            return new NotifyDto.Status(base, true, version, true, true, bot.username(), "연결되었습니다.");
        } catch (MattermostException e) {
            return new NotifyDto.Status(base, true, version, true, false, null, "봇 인증에 실패했습니다. " + e.getMessage());
        }
    }

    /** 이 사용자가 Mattermost 의 누구와 연결되는지 찾아 본다(이메일 → 메신저 ID 순). 발송은 하지 않는다. */
    public NotifyDto.Resolve resolve(String userId) {
        User user = userRepository.findById(userId).filter(u -> "N".equals(u.getDeletedYn())).orElse(null);
        if (user == null) {
            return new NotifyDto.Resolve(userId, null, false, null, null, null, "존재하지 않는 사용자입니다.");
        }
        UserProfile profile = profileRepository.findByUserIdAndDeletedYn(userId, "N").orElse(null);
        String channel = profile == null ? null : profile.getNotifyChannelCode();
        if (!mattermost.hasToken()) {
            return new NotifyDto.Resolve(userId, channel, false, null, null, null, "봇 토큰이 없어 Mattermost 사용자를 조회할 수 없습니다.");
        }
        try {
            String email = PmsSupport.blankToNull(user.getEmail());
            if (email != null) {
                Optional<MmUser> byEmail = mattermost.findByEmail(email);
                if (byEmail.isPresent()) {
                    return found(userId, channel, byEmail.get(), "이메일");
                }
            }
            String handle = profile == null ? null : PmsSupport.blankToNull(profile.getMattermostUserId());
            if (handle != null) {
                Optional<MmUser> byName = mattermost.findByUsername(handle);
                if (byName.isPresent()) {
                    return found(userId, channel, byName.get(), "메신저 ID");
                }
            }
            return new NotifyDto.Resolve(userId, channel, false, null, null, null,
                    "Mattermost 에서 이메일" + (handle != null ? "·메신저 ID" : "") + "로 사용자를 찾지 못했습니다.");
        } catch (MattermostException e) {
            return new NotifyDto.Resolve(userId, channel, false, null, null, null, e.getMessage());
        }
    }

    /** 사용자 한 명에게 알림을 보낸다. 결과는 항상 이력에 남기고, 예외는 던지지 않는다. */
    @Transactional
    public NotifyDto.Result send(String userId, String kind, String text) {
        UserProfile profile = profileRepository.findByUserIdAndDeletedYn(userId, "N").orElse(null);
        String channel = profile == null || profile.getNotifyChannelCode() == null ? "미설정" : profile.getNotifyChannelCode();
        String status;
        String reason = null;
        if (!"Mattermost".equals(channel)) {
            status = SKIPPED;
            reason = "미설정".equals(channel) ? "알림 채널이 설정되지 않았습니다." : channel + " 채널은 아직 연결되지 않았습니다.";
        } else if (!mattermost.hasToken()) {
            status = SKIPPED;
            reason = "봇 토큰이 설정되지 않았습니다.";
        } else {
            NotifyDto.Resolve target = resolve(userId);
            if (!target.found()) {
                status = SKIPPED;
                reason = target.message();
            } else {
                try {
                    mattermost.sendDirect(mattermost.me().id(), target.mattermostUserId(), text);
                    status = SENT;
                } catch (MattermostException e) {
                    status = FAILED;
                    reason = e.getMessage();
                }
            }
        }
        logRepository.save(NotifyLog.create().withUserId(userId).withChannelCode(channel).withKindCode(kind)
                .withMessage(text).withStatusCode(status).withErrorMessage(reason));
        return new NotifyDto.Result(status, reason == null ? "발송했습니다." : reason);
    }

    public List<NotifyDto.Log> recentLogs() {
        List<NotifyLog> logs = logRepository.findTop50ByDeletedYnOrderByCreatedDateDesc("N");
        Map<String, String> names = new HashMap<>();
        userRepository.findAll().forEach(u -> names.put(u.getId(), u.getUserName()));
        return logs.stream()
                .map(l -> new NotifyDto.Log(l.getId(), l.getUserId(), names.getOrDefault(l.getUserId(), l.getUserId()), l.getChannelCode(),
                        l.getKindCode(), l.getMessage(), l.getStatusCode(), l.getErrorMessage(), l.getCreatedDate()))
                .toList();
    }

    private static NotifyDto.Resolve found(String userId, String channel, MmUser u, String via) {
        return new NotifyDto.Resolve(userId, channel, true, u.id(), u.username(), via, "Mattermost 사용자 @" + u.username() + " 와 연결됩니다(" + via + ").");
    }
}
