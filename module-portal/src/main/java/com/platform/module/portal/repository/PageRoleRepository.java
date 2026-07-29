package com.platform.module.portal.repository;

import com.platform.module.portal.domain.PageRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageRoleRepository extends JpaRepository<PageRole, String> {
    boolean existsByPageIdAndRoleCode(String pageId, String roleCode);
    List<PageRole> findAllByPageId(String pageId);
    Optional<PageRole> findByIdAndPageId(String id, String pageId);

    /** roleCode 관점의 역방향 조회 — "이 역할코드가 매핑된 모든 페이지" (RolePageController). */
    List<PageRole> findAllByRoleCode(String roleCode);
}
