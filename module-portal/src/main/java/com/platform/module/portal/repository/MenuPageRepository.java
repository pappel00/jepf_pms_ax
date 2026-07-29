package com.platform.module.portal.repository;

import com.platform.module.portal.domain.MenuPage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuPageRepository extends JpaRepository<MenuPage, String> {
    boolean existsByMenuIdAndPageId(String menuId, String pageId);
    List<MenuPage> findAllByMenuIdOrderBySortOrderAsc(String menuId);
    Optional<MenuPage> findByIdAndMenuId(String id, String menuId);

    /** 페이지 관점의 역방향 조회 — 이 페이지를 참조하는 매핑이 있는지(삭제 시 참고용 등). */
    List<MenuPage> findAllByPageId(String pageId);
}
