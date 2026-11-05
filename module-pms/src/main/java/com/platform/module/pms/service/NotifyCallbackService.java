package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.pms.dto.WbsDto;
import com.platform.module.user.service.EffectiveRoleService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Mattermost 버튼 콜백 처리. JWT 없이 들어오므로 버튼에 심은 서명 토큰(NotifyTokens)을 검증한 뒤, 토큰의 사용자·테넌트로
 * 잠시 로그인한 상태를 만들어 기존 진척 입력(WbsService.updateProgress)을 그대로 호출한다 — 담당자 본인만 입력할 수 있다는
 * 규칙과 선행 업무 검사가 웹 입력과 똑같이 적용된다.
 */
@Service
public class NotifyCallbackService {

    private static final Logger log = LoggerFactory.getLogger(NotifyCallbackService.class);

    private final NotifyTokens tokens;
    private final WbsService wbsService;
    private final EffectiveRoleService effectiveRoleService;

    public NotifyCallbackService(NotifyTokens tokens, WbsService wbsService, EffectiveRoleService effectiveRoleService) {
        this.tokens = tokens;
        this.wbsService = wbsService;
        this.effectiveRoleService = effectiveRoleService;
    }

    /**
     * 담당자 본인(userId)으로 잠시 로그인한 상태에서 진척을 입력한다 — 웹 입력과 같은 규칙(담당자 본인·선행 업무)이 적용된다.
     * 규칙에 어긋나면 BusinessException 을 던진다. 성공하면 사용자에게 보여 줄 완료 문구를 돌려준다.
     */
    public String applyProgress(String tenantId, String userId, String taskId, int pct, String channel) {
        return applyProgress(tenantId, userId, taskId, BigDecimal.valueOf(pct), channel);
    }

    /** 직접 입력한 값(소수 가능)으로 진척을 입력한다. 범위(0~100)는 요청 검증이 아니라 여기서 확인한다. */
    public String applyProgress(String tenantId, String userId, String taskId, BigDecimal pct, String channel) {
        if (pct.signum() < 0 || pct.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("ERR-PMS-0016", "진척률은 0~100 사이여야 합니다.");
        }
        try {
            TenantContext.setTenantId(tenantId);
            List<GrantedAuthority> authorities = effectiveRoleService.resolveRoles(userId).stream()
                    .map(SimpleGrantedAuthority::new).map(GrantedAuthority.class::cast).toList();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, authorities));
            wbsService.updateProgress(taskId, new WbsDto.ProgressRequest(pct, null), channel);
            return "[PMS] ✅ 진척 " + pct.setScale(1, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "% 입력 완료";
        } finally {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }
    }

    /** Mattermost 가 기대하는 응답(update: 메시지 교체, ephemeral_text: 누른 사람에게만 보이는 안내)을 돌려준다. */
    public Map<String, Object> handle(Map<String, Object> body) {
        Object ctx = body == null ? null : body.get("context");
        Object token = ctx instanceof Map<?, ?> m ? m.get("token") : null;
        NotifyTokens.Payload p = tokens.verify(token == null ? null : token.toString());
        if (p == null) {
            return Map.of("ephemeral_text", "만료되었거나 유효하지 않은 버튼입니다. PMS 에서 직접 입력해 주세요.");
        }
        try {
            String done = applyProgress(p.tenantId(), p.userId(), p.taskId(), p.progressPct(), "MATTERMOST");
            return Map.of("update", Map.of("message", done, "props", Map.of("attachments", List.of())), "ephemeral_text", done);
        } catch (BusinessException e) {
            return Map.of("ephemeral_text", "입력하지 못했습니다: " + e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Mattermost 버튼 콜백 처리 실패: {}", e.getMessage(), e);
            return Map.of("ephemeral_text", "처리 중 오류가 발생했습니다. PMS 에서 직접 입력해 주세요.");
        } finally {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }
    }
}
