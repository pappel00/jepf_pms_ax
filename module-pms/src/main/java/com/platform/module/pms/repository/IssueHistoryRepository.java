package com.platform.module.pms.repository;

import com.platform.module.pms.domain.IssueHistory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueHistoryRepository extends JpaRepository<IssueHistory, String> {
    Optional<IssueHistory> findByIdAndDeletedYn(String id, String deletedYn);

    List<IssueHistory> findAllByIssueIdInAndDeletedYnOrderByActedAtAsc(Collection<String> issueIds, String deletedYn);
}
