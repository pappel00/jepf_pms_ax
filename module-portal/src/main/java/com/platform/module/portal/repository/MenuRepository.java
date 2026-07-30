package com.platform.module.portal.repository;

import com.platform.module.portal.domain.Menu;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, String> {
    boolean existsByMenuCode(String menuCode);
    Optional<Menu> findByIdAndDeletedYn(String id, String deletedYn);
    Page<Menu> findAllByDeletedYn(String deletedYn, Pageable pageable);
    Page<Menu> findAllByApplicationIdAndDeletedYn(String applicationId, String deletedYn, Pageable pageable);

    /** 관리자 목록 화면은 삭제된 항목도 상태(deletedYn)와 함께 보여줘야 하므로 필터링하지 않는다. */
    Page<Menu> findAllByApplicationId(String applicationId, Pageable pageable);

    /** 트리 조립용 — 페이징 없이 특정 어플리케이션의 활성 메뉴를 정렬 순서대로 전부 가져온다. */
    List<Menu> findAllByApplicationIdAndDeletedYnOrderBySortOrderAsc(String applicationId, String deletedYn);

    /** 다른 모듈이 부트스트랩 시 특정 메뉴가 이미 있는지 찾아볼 때 사용 (find-or-create). */
    Optional<Menu> findByMenuCodeAndDeletedYn(String menuCode, String deletedYn);
}

