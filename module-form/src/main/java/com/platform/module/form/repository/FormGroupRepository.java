package com.platform.module.form.repository;

import com.platform.module.form.domain.FormGroup;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormGroupRepository extends JpaRepository<FormGroup, String> {
    /** 같은 formCode라도 포탈이 다르면 별개의 양식으로 취급한다 (클래스 주석 참고). */
    boolean existsByPortalIdAndFormCode(String portalId, String formCode);

    Optional<FormGroup> findByIdAndDeletedYn(String id, String deletedYn);

    /** 포탈별 관리 화면에서 특정 포탈의 양식만 걸러볼 때 사용. */
    Page<FormGroup> findAllByPortalId(String portalId, Pageable pageable);

    // 전체 목록 조회는 JpaRepository가 기본 제공하는 findAll(Pageable)을 그대로 쓴다 — 관리자
    // 화면은 삭제된 항목도 상태(deletedYn)와 함께 보여줘야 하므로 deletedYn으로 걸러내지 않는다.
}
