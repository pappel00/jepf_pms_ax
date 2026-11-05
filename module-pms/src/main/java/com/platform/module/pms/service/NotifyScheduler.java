package com.platform.module.pms.service;

import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.pms.dto.NotifyDto;
import com.platform.module.tenant.domain.Company;
import com.platform.module.tenant.repository.CompanyRepository;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매시 정각에 회사(테넌트)별로 업무 알림 설정을 확인해, 켜져 있고 발송 시각이 지금 시(時)인 회사의 알림을 보낸다.
 * 서버가 여러 대가 되면 같은 알림이 중복될 수 있으므로 그때는 분산 락(예: ShedLock)을 붙인다.
 */
@Component
public class NotifyScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotifyScheduler.class);

    private final CompanyRepository companyRepository;
    private final NotifyJobService jobService;

    public NotifyScheduler(CompanyRepository companyRepository, NotifyJobService jobService) {
        this.companyRepository = companyRepository;
        this.jobService = jobService;
    }

    private void report(String tenantId, String kind, NotifyDto.RunResult r) {
        if (r.targets() > 0) {
            log.info("기한 알림 배치({}) tenant={} 대상 {}건(발송 {}, 건너뜀 {}, 실패 {}, 중복 {})", kind, tenantId, r.targets(), r.sent(), r.skipped(),
                    r.failed(), r.duplicates());
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void hourly() {
        int hour = LocalTime.now().getHour();
        for (Company company : companyRepository.findAll()) {
            if (!"N".equals(company.getDeletedYn())) {
                continue;
            }
            try {
                TenantContext.setTenantId(company.getId());
                var settings = jobService.getSettings();
                if (!settings.enabled()) {
                    continue;
                }
                // 날짜 기준 알림은 정한 시각에 한 번, 시(時) 단위 SLA 알림은 매시 확인한다.
                if (settings.hour() == hour) {
                    report(company.getId(), "일일", jobService.run(false, NotifyType.Scope.DAILY));
                }
                report(company.getId(), "SLA", jobService.run(false, NotifyType.Scope.SLA));
            } catch (RuntimeException e) {
                log.error("업무 알림 배치 실패 tenant={}: {}", company.getId(), e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
