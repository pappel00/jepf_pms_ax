package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Issue;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueRepository extends JpaRepository<Issue, String> {
    Optional<Issue> findByIdAndDeletedYn(String id, String deletedYn);
}
