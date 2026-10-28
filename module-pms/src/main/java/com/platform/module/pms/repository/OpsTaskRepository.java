package com.platform.module.pms.repository;

import com.platform.module.pms.domain.OpsTask;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsTaskRepository extends JpaRepository<OpsTask, String> {
    Optional<OpsTask> findByIdAndDeletedYn(String id, String deletedYn);

    List<OpsTask> findAllByDeletedYnOrderByDueDateAsc(String deletedYn);

    /** 코드 채번용 — 삭제된 행 포함 전체. */
    List<OpsTask> findAll();

    List<OpsTask> findAllByProjectIdAndDeletedYn(String projectId, String deletedYn);
}
