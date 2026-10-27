package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ProjectMember;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, String> {
    Optional<ProjectMember> findByIdAndDeletedYn(String id, String deletedYn);
}
