package com.platform.module.pms.service;

import com.platform.module.pms.domain.NotifyLog;
import com.platform.module.pms.domain.UserProfile;
import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.pms.domain.WbsTask;
import com.platform.module.pms.repository.ProjectRepository;
import com.platform.module.pms.repository.WbsTaskRepository;
import com.platform.module.pms.exception.PmsErrorCode;
import com.platform.framework.core.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
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
 * Mattermost·텔레그램이 연결되어 있고, 이메일(협력사 인력용)은 아직 연결 전이라 건너뛰고 이력에 남긴다.
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
    private final WbsTaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final NotifyTokens tokens;
    private final String publicBaseUrl;
    private final TelegramClient telegram;
    private final boolean telegramPolling;
    private final EmailClient email;

    /** 진척 입력 버튼이 달린 메시지의 제목·본문. 버튼은 채널(Mattermost·텔레그램)에 맞춰 보낼 때 만든다. */
    private record Prompt(String title, String detail, String taskId) {
    }

    private static final int[] PROGRESS_STEPS = {0, 25, 50, 75, 100};

    public NotifyService(MattermostClient mattermost, UserRepository userRepository,
                         UserProfileRepository profileRepository, NotifyLogRepository logRepository,
                         WbsTaskRepository taskRepository, ProjectRepository projectRepository, NotifyTokens tokens,
                         @Value("${pms.notify.public-base-url:http://localhost:8080}") String publicBaseUrl, TelegramClient telegram,
                         @Value("${pms.notify.telegram.polling:true}") boolean telegramPolling, EmailClient email) {
        this.email = email;
        this.telegram = telegram;
        this.telegramPolling = telegramPolling;
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.tokens = tokens;
        this.publicBaseUrl = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
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

    /** 이메일(SMTP) 연결 상태 — 설정 유무와 서버 접속·인증을 확인한다. */
    public NotifyDto.EmailStatus emailStatus() {
        if (!email.isConfigured()) {
            return new NotifyDto.EmailStatus(false, false, email.getHost().isEmpty() ? null : email.getHost(), null,
                    "SMTP 설정이 없습니다. 서버·계정·비밀번호(pms.notify.email.* 또는 PMS_SMTP_*)를 지정하세요.");
        }
        try {
            email.testConnection();
            return new NotifyDto.EmailStatus(true, true, email.getHost() + ":" + email.getPort(), email.getFrom(), "연결되었습니다.");
        } catch (EmailClient.EmailException e) {
            return new NotifyDto.EmailStatus(true, false, email.getHost() + ":" + email.getPort(), email.getFrom(), e.getMessage());
        }
    }

    /** 이메일 본문 — 버튼이 없으므로 진척 입력 안내를 PMS 화면 입력으로 바꾼다. */
    private static String emailBody(String text, Prompt prompt) {
        if (prompt == null) {
            return text + "\n\n— PMS 알림";
        }
        String detail = prompt.detail();
        int cut = detail.lastIndexOf('\n');
        String info = cut > 0 ? detail.substring(0, cut) : detail;
        return text + "\n\n" + info + "\n\nPMS의 WBS / 진척 관리에서 진척률을 입력해 주세요.\n\n— PMS 알림";
    }

    /** 텔레그램 봇 연결 상태 — 토큰 유무와 봇 인증을 확인한다. */
    public NotifyDto.TelegramStatus telegramStatus() {
        if (!telegram.hasToken()) {
            return new NotifyDto.TelegramStatus(false, false, null, telegramPolling, "봇 토큰(PMS_TELEGRAM_TOKEN)이 설정되지 않았습니다.");
        }
        try {
            Object name = telegram.getMe().get("username");
            return new NotifyDto.TelegramStatus(true, true, name == null ? null : String.valueOf(name), telegramPolling,
                    telegramPolling ? "연결되었습니다." : "연결되었지만 메시지 가져오기(polling)가 꺼져 있어 사용자 연결·버튼 클릭이 처리되지 않습니다.");
        } catch (TelegramClient.TelegramException e) {
            return new NotifyDto.TelegramStatus(true, false, null, telegramPolling, e.getMessage());
        }
    }

    private List<MattermostClient.Action> mattermostActions(String userId, String taskId) {
        String tenant = TenantContext.getTenantId();
        String url = publicBaseUrl + "/api/v1/pms/notify/callback";
        List<MattermostClient.Action> actions = new java.util.ArrayList<>();
        for (int pct : PROGRESS_STEPS) {
            actions.add(new MattermostClient.Action("pct" + pct, pct + "%", url, Map.of("token", tokens.sign(tenant, userId, taskId, pct))));
        }
        return actions;
    }

    /** 이 사용자가 Mattermost 의 누구와 연결되는지 찾아 본다(이메일 → 메신저 ID 순). 발송은 하지 않는다. */
    public NotifyDto.Resolve resolve(String userId) {
        User user = userRepository.findById(userId).filter(u -> "N".equals(u.getDeletedYn())).orElse(null);
        if (user == null) {
            return new NotifyDto.Resolve(userId, null, false, null, null, null, "존재하지 않는 사용자입니다.");
        }
        UserProfile profile = profileRepository.findByUserIdAndDeletedYn(userId, "N").orElse(null);
        String channel = profile == null ? null : profile.getNotifyChannelCode();
        if ("이메일".equals(channel)) {
            String to = PmsSupport.blankToNull(user.getEmail());
            return new NotifyDto.Resolve(userId, channel, to != null, to, null, "계정 이메일",
                    to != null ? "수신 주소: " + to : "사용자의 이메일 주소가 없습니다. 인력 관리에서 이메일을 입력하세요.");
        }
        if ("텔레그램".equals(channel)) {
            String chatId = PmsSupport.blankToNull(profile.getTelegramChatId());
            return new NotifyDto.Resolve(userId, channel, chatId != null, chatId, null, "연결 코드",
                    chatId != null ? "텔레그램이 연결되어 있습니다(chat " + chatId + ")." : "텔레그램이 연결되지 않았습니다. 연결 코드를 발급해 사용자가 봇에게 보내야 합니다.");
        }
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
        return deliver(userId, kind, text, null, null);
    }

    /** refId(알림이 가리키는 항목 ID)를 이력에 남겨 같은 날 중복 발송을 막는 데 쓴다. */
    @Transactional
    public NotifyDto.Result send(String userId, String kind, String text, String refId) {
        return deliver(userId, kind, text, null, refId);
    }

    /** 업무 담당자에게 진척 입력 버튼(0/25/50/75/100%)이 달린 메시지를 보낸다. 버튼을 누르면 콜백으로 진척이 입력된다. */
    @Transactional
    public NotifyDto.Result sendProgressPrompt(String taskId) {
        return sendProgressPrompt(taskId, "PROGRESS_PROMPT", "진척 입력 요청");
    }

    /** kind·headline 으로 알림 종류(마감 D-1, 오늘 시작 등)를 구분한다. 같은 업무·종류는 업무 ID 로 이력에 남는다. */
    @Transactional
    public NotifyDto.Result sendProgressPrompt(String taskId, String kind, String headline) {
        WbsTask t = taskRepository.findByIdAndDeletedYn(taskId, "N")
                .orElseThrow(() -> new BusinessException(PmsErrorCode.TASK_NOT_FOUND, "존재하지 않는 업무입니다: " + taskId));
        if (!"LEAF".equals(t.getNodeTypeCode()) || t.getAssigneeUserId() == null) {
            throw new BusinessException(PmsErrorCode.STAFF_INVALID, "담당자가 지정된 하위 업무만 진척 입력 알림을 보낼 수 있습니다.");
        }
        String project = projectRepository.findByIdAndDeletedYn(t.getProjectId(), "N").map(p -> p.getProjectName()).orElse("");
        String current = t.getProgressPct() == null ? "0" : t.getProgressPct().stripTrailingZeros().toPlainString();
        String detail = "프로젝트: " + project + "\n기한: " + t.getEndDate() + " · 현재 진척 " + current + "%\n아래 버튼으로 진척률을 입력하세요.";
        return deliver(t.getAssigneeUserId(), kind, "[PMS] " + headline + " — " + t.getTaskName(),
                new Prompt(t.getTaskName(), detail, t.getId()), t.getId());
    }

    private NotifyDto.Result deliver(String userId, String kind, String text, Prompt prompt, String refId) {
        UserProfile profile = profileRepository.findByUserIdAndDeletedYn(userId, "N").orElse(null);
        String channel = profile == null || profile.getNotifyChannelCode() == null ? "미설정" : profile.getNotifyChannelCode();
        String status;
        String reason = null;
        if ("텔레그램".equals(channel)) {
            String chatId = profile == null ? null : PmsSupport.blankToNull(profile.getTelegramChatId());
            if (!telegram.hasToken()) {
                status = SKIPPED;
                reason = "텔레그램 봇 토큰이 설정되지 않았습니다.";
            } else if (chatId == null) {
                status = SKIPPED;
                reason = "텔레그램이 연결되지 않았습니다(인력 관리에서 연결 코드를 발급하세요).";
            } else {
                try {
                    if (prompt == null) {
                        telegram.sendMessage(chatId, text, null);
                    } else {
                        List<TelegramClient.Button> buttons = new java.util.ArrayList<>();
                        for (int pct : PROGRESS_STEPS) {
                            buttons.add(new TelegramClient.Button(pct + "%", tokens.signTelegram(chatId, prompt.taskId(), pct)));
                        }
                        // 두 번째 줄: 진척률을 숫자로 직접 입력(-1 은 직접 입력 표시)
                        List<TelegramClient.Button> manual = List.of(new TelegramClient.Button("✏️ 직접 입력", tokens.signTelegram(chatId, prompt.taskId(), -1)));
                        telegram.sendMessage(chatId, text + "\n\n" + prompt.detail(), List.of(buttons, manual));
                    }
                    status = SENT;
                } catch (TelegramClient.TelegramException e) {
                    status = FAILED;
                    reason = e.getMessage();
                }
            }
        } else if ("이메일".equals(channel)) {
            String to = userRepository.findById(userId).map(User::getEmail).map(PmsSupport::blankToNull).orElse(null);
            if (!email.isConfigured()) {
                status = SKIPPED;
                reason = "이메일(SMTP) 설정이 없습니다.";
            } else if (to == null) {
                status = SKIPPED;
                reason = "사용자의 이메일 주소가 없습니다.";
            } else {
                try {
                    email.send(to, text.length() > 120 ? text.substring(0, 120) : text, emailBody(text, prompt));
                    status = SENT;
                } catch (EmailClient.EmailException e) {
                    status = FAILED;
                    reason = e.getMessage();
                }
            }
        } else if (!"Mattermost".equals(channel)) {
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
                    if (prompt == null) {
                        mattermost.sendDirect(mattermost.me().id(), target.mattermostUserId(), text);
                    } else {
                        mattermost.sendDirectWithActions(mattermost.me().id(), target.mattermostUserId(), text, prompt.title(), prompt.detail(),
                                mattermostActions(userId, prompt.taskId()));
                    }
                    status = SENT;
                } catch (MattermostException e) {
                    status = FAILED;
                    reason = e.getMessage();
                }
            }
        }
        logRepository.save(NotifyLog.create().withUserId(userId).withChannelCode(channel).withKindCode(kind)
                .withMessage(text).withStatusCode(status).withErrorMessage(reason).withRefId(refId));
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
