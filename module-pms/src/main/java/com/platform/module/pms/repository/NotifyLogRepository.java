package com.platform.module.pms.repository;

import com.platform.module.pms.domain.NotifyLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotifyLogRepository extends JpaRepository<NotifyLog, String> {
    List<NotifyLog> findTop50ByDeletedYnOrderByCreatedDateDesc(String deletedYn);
}
