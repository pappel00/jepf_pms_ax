package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ProjectMember;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, String> {
    Optional<ProjectMember> findByIdAndDeletedYn(String id, String deletedYn);

    List<ProjectMember> findAllByProjectIdAndKindCodeOrderByCreatedDateAsc(String projectId, String kindCode);

    List<ProjectMember> findAllByKindCodeOrderByCreatedDateAsc(String kindCode);

    List<ProjectMember> findAllByUserIdAndDeletedYn(String userId, String deletedYn);

    List<ProjectMember> findAllByProjectIdInAndKindCodeOrderByCreatedDateAsc(Collection<String> projectIds, String kindCode);
}