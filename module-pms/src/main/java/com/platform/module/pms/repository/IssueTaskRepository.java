package com.platform.module.pms.repository;

import com.platform.module.pms.domain.IssueTask;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueTaskRepository extends JpaRepository<IssueTask, String> {
    Optional<IssueTask> findByIdAndDeletedYn(String id, String deletedYn);
}
