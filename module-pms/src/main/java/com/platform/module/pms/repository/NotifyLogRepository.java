package com.platform.module.pms.repository;

import com.platform.module.pms.domain.NotifyLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotifyLogRepository extends JpaRepository<NotifyLog, String> {
    List<NotifyLog> findTop50ByDeletedYnOrderByCreatedDateDesc(String deletedYn);

    /** 실패가 아닌(발송했거나 대상이 아니라 건너뛴) 같은 알림이 기준 시각 이후에 이미 있는지. */
    boolean existsByRefIdAndKindCodeAndStatusCodeNotAndCreatedDateAfter(String refId, String kindCode, String statusCode, LocalDateTime after);
}
