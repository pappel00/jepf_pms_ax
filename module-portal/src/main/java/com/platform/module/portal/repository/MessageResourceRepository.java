package com.platform.module.portal.repository;

import com.platform.module.portal.domain.MessageResource;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageResourceRepository extends JpaRepository<MessageResource, String> {

    boolean existsByMessageResourceCodeAndLanguageId(String messageResourceCode, String languageId);

    Optional<MessageResource> findByIdAndDeletedYn(String id, String deletedYn);

    /** 프론트가 "이 코드를 이 언어로 어떻게 표시할지" 물어볼 때 사용. */
    Optional<MessageResource> findByMessageResourceCodeAndLanguageIdAndDeletedYn(
            String messageResourceCode, String languageId, String deletedYn);

    /** 하나의 코드가 가진 모든 언어 변형을 볼 때 사용 (BackOffice 관리 화면). */
    Page<MessageResource> findAllByMessageResourceCodeAndDeletedYn(
            String messageResourceCode, String deletedYn, Pageable pageable);

    /** 관리자 목록 화면은 삭제된 항목도 상태(deletedYn)와 함께 보여줘야 하므로 필터링하지 않는다. */
    Page<MessageResource> findAllByMessageResourceCode(String messageResourceCode, Pageable pageable);

    Page<MessageResource> findAllByDeletedYn(String deletedYn, Pageable pageable);
}
