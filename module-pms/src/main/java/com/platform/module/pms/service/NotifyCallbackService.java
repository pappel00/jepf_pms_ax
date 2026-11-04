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

    /** Mattermost 가 기대하는 응답(update: 메시지 교체, ephemeral_text: 누른 사람에게만 보이는 안내)을 돌려준다. */
    public Map<String, Object> handle(Map<String, Object> body) {
        Object ctx = body == null ? null : body.get("context");
        Object token = ctx instanceof Map<?, ?> m ? m.get("token") : null;
        NotifyTokens.Payload p = tokens.verify(token == null ? null : token.toString());
        if (p == null) {
            return Map.of("ephemeral_text", "만료되었거나 유효하지 않은 버튼입니다. PMS 에서 직접 입력해 주세요.");
        }
        try {
            TenantContext.setTenantId(p.tenantId());
            List<GrantedAuthority> authorities = effectiveRoleService.resolveRoles(p.userId()).stream()
                    .map(SimpleGrantedAuthority::new).map(GrantedAuthority.class::cast).toList();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p.userId(), null, authorities));
            wbsService.updateProgress(p.taskId(), new WbsDto.ProgressRequest(BigDecimal.valueOf(p.progressPct()), null), "MATTERMOST");
            String done = "[PMS] ✅ 진척 " + p.progressPct() + "% 입력 완료";
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
