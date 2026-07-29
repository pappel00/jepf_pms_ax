package com.platform.module.code.repository;

import com.platform.module.code.domain.CodeGroup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeGroupRepository extends JpaRepository<CodeGroup, String> {
    boolean existsByGroupCode(String groupCode);
    Optional<CodeGroup> findByIdAndDeletedYn(String id, String deletedYn);

    /** 다른 모듈이 부트스트랩 시 특정 코드그룹이 이미 있는지 찾아볼 때 사용 (find-or-create). */
    Optional<CodeGroup> findByGroupCodeAndDeletedYn(String groupCode, String deletedYn);

    // 전체 목록 조회는 JpaRepository가 기본 제공하는 findAll(Pageable)을 그대로 쓴다 — 관리자
    // 화면은 삭제된 항목도 상태(deletedYn)와 함께 보여줘야 하므로 deletedYn으로 걸러내지 않는다.
}
