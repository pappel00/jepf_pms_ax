package com.platform.module.pms.repository;

import com.platform.module.pms.domain.SalesNote;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesNoteRepository extends JpaRepository<SalesNote, String> {
    Optional<SalesNote> findByIdAndDeletedYn(String id, String deletedYn);

    List<SalesNote> findAllByDeletedYnOrderByNoteDateDescCreatedDateDesc(String deletedYn);

    /** 코드 채번용 — 삭제된 행 포함 전체. */
    List<SalesNote> findAll();
}
