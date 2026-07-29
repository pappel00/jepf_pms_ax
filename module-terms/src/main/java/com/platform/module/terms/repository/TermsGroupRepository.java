package com.platform.module.terms.repository;

import com.platform.module.terms.domain.TermsGroup;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsGroupRepository extends JpaRepository<TermsGroup, String> {
    /** 같은 termsCode라도 포탈이 다르면 별개의 약관으로 취급한다 (TermsGroup 클래스 주석 참고). */
    boolean existsByPortalIdAndTermsCode(String portalId, String termsCode);

    Optional<TermsGroup> findByIdAndDeletedYn(String id, String deletedYn);

    /** 포탈별 관리 화면에서 특정 포탈의 약관만 걸러볼 때 사용. */
    Page<TermsGroup> findAllByPortalId(String portalId, Pageable pageable);

    // 전체 목록 조회는 JpaRepository가 기본 제공하는 findAll(Pageable)을 그대로 쓴다 — 관리자
    // 화면은 삭제된 항목도 상태(deletedYn)와 함께 보여줘야 하므로 deletedYn으로 걸러내지 않는다.
}
